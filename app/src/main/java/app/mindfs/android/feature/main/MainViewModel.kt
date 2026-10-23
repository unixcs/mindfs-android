package app.mindfs.android.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mindfs.android.core.data.NodeRepository
import app.mindfs.android.core.datastore.AppSettings
import app.mindfs.android.core.datastore.AppSettingsStore
import app.mindfs.android.core.datastore.NodeBook
import app.mindfs.android.core.model.NodeHealth
import app.mindfs.android.core.model.NodeProfile
import app.mindfs.android.core.model.RootSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RootsUiState {
    data object NoNode : RootsUiState
    data object Loading : RootsUiState
    data class Ready(val roots: List<RootSummary>) : RootsUiState
    data class Failed(val message: String) : RootsUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val appSettingsStore: AppSettingsStore,
) : ViewModel() {

    /** Null until the stored settings are read; the shell shows a loading frame. */
    val settings: StateFlow<AppSettings?> = appSettingsStore.settings
        .map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val nodeBook: StateFlow<NodeBook> = nodeRepository.book
        .stateIn(viewModelScope, SharingStarted.Eagerly, NodeBook())

    private val _health = MutableStateFlow<Map<String, NodeHealth>>(emptyMap())
    val health: StateFlow<Map<String, NodeHealth>> = _health.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private val _roots = MutableStateFlow<RootsUiState>(RootsUiState.Loading)
    val roots: StateFlow<RootsUiState> = _roots.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refreshHealth()
        // New or renamed nodes appear in the book via the nodes screen; pick them up
        // and refresh any node we have no health record for yet.
        viewModelScope.launch {
            nodeRepository.book.collect { book ->
                if (book.nodes.isNotEmpty() && book.nodes.any { it.id !in _health.value }) {
                    refreshHealth()
                }
            }
        }
    }

    val selectedNode: NodeProfile?
        get() = nodeBook.value.let { book ->
            book.nodes.firstOrNull { it.id == book.selectedId } ?: book.nodes.firstOrNull()
        }

    fun refreshHealth() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val nodes = nodeRepository.book.first().nodes
            if (nodes.isEmpty()) return@launch
            _checking.value = true
            nodeRepository.checkAll(nodes).forEach { check ->
                _health.value = _health.value + (check.node.id to check.health)
            }
            val ids = nodes.map { it.id }.toSet()
            _health.value = _health.value.filterKeys { it in ids }
            _checking.value = false
        }
    }

    fun loadRoots(force: Boolean = false) {
        if (!force && _roots.value is RootsUiState.Ready) return
        val node = selectedNode ?: run {
            _roots.value = RootsUiState.NoNode
            return
        }
        viewModelScope.launch {
            _roots.value = RootsUiState.Loading
            _roots.value = try {
                RootsUiState.Ready(nodeRepository.clientFor(node).roots())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failed: Exception) {
                RootsUiState.Failed(failed.message ?: failed::class.java.simpleName)
            }
        }
    }

    fun setThemeMode(mode: String) = viewModelScope.launch {
        appSettingsStore.update { it.copy(themeMode = mode) }
    }

    fun setStartTab(tab: String) = viewModelScope.launch {
        appSettingsStore.update { it.copy(startTab = tab) }
    }
}
