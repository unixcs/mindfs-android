package app.mindfs.android.feature.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mindfs.android.core.data.NodeRepository
import app.mindfs.android.core.datastore.NodeBook
import app.mindfs.android.core.datastore.NodeStore
import app.mindfs.android.core.model.NodeHealth
import app.mindfs.android.core.model.NodeProfile
import app.mindfs.android.core.model.normalizeBaseUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NodesViewModel @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val nodeStore: NodeStore,
) : ViewModel() {

    val book: StateFlow<NodeBook> = nodeStore.book
        .stateIn(viewModelScope, SharingStarted.Eagerly, NodeBook())

    private val _testResults = MutableStateFlow<Map<String, NodeHealth>>(emptyMap())
    val testResults: StateFlow<Map<String, NodeHealth>> = _testResults.asStateFlow()

    private val _testingIds = MutableStateFlow<Set<String>>(emptySet())
    val testingIds: StateFlow<Set<String>> = _testingIds.asStateFlow()

    fun upsert(name: String, url: String, cleartextConfirmed: Boolean, editing: NodeProfile?) {
        viewModelScope.launch {
            val baseUrl = normalizeBaseUrl(url)
            val node = editing?.let { current ->
                if (current.baseUrl != baseUrl) nodeRepository.evictClient(current)
                current.copy(
                    name = name.trim(),
                    baseUrl = baseUrl,
                    confirmedCleartext = current.confirmedCleartext || cleartextConfirmed,
                )
            } ?: NodeProfile(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                baseUrl = baseUrl,
                confirmedCleartext = cleartextConfirmed,
                createdAtEpochMs = System.currentTimeMillis(),
            )
            nodeStore.upsert(node)
            if (nodeStore.book.first().selectedId == null) nodeStore.select(node.id)
        }
    }

    fun delete(node: NodeProfile) {
        viewModelScope.launch {
            nodeRepository.evictClient(node)
            nodeStore.remove(node.id)
            _testResults.value = _testResults.value - node.id
        }
    }

    fun select(nodeId: String) {
        viewModelScope.launch { nodeStore.select(nodeId) }
    }

    fun test(node: NodeProfile) {
        if (node.id in _testingIds.value) return
        viewModelScope.launch {
            _testingIds.value = _testingIds.value + node.id
            _testResults.value = _testResults.value - node.id
            val health = try {
                nodeRepository.clientFor(node).health()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failed: Exception) {
                NodeHealth(ok = false, latencyMs = -1, detail = failed.message ?: "Unknown error")
            }
            _testResults.value = _testResults.value + (node.id to health)
            _testingIds.value = _testingIds.value - node.id
        }
    }

    fun testAll() {
        book.value.nodes.forEach { test(it) }
    }
}
