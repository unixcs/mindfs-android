package app.mindfs.android.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mindfs.android.R
import app.mindfs.android.core.datastore.AppSettings
import app.mindfs.android.core.designsystem.MindfsTheme
import app.mindfs.android.feature.server.NodesScreen
import app.mindfs.android.feature.settings.SettingsScreen

enum class Tab(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    ACTIVITY(AppSettings.TAB_ACTIVITY, R.string.tab_activity, Icons.Outlined.Bolt),
    CHATS(AppSettings.TAB_CHATS, R.string.tab_chats, Icons.AutoMirrored.Outlined.Chat),
    PROJECTS(AppSettings.TAB_PROJECTS, R.string.tab_projects, Icons.Outlined.FolderOpen),
    TASKS(AppSettings.TAB_TASKS, R.string.tab_tasks, Icons.Outlined.Checklist),
    SETTINGS(AppSettings.TAB_SETTINGS, R.string.tab_settings, Icons.Outlined.Person);

    companion object {
        fun byId(id: String): Tab? = entries.firstOrNull { it.id == id }
    }
}

@Composable
fun AppRoot(mainViewModel: MainViewModel = hiltViewModel()) {
    val settingsState = mainViewModel.settings.collectAsStateWithLifecycle()
    val settings = settingsState.value ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val darkTheme = when (settings.themeMode) {
        AppSettings.THEME_DARK -> true
        AppSettings.THEME_LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    MindfsTheme(darkTheme = darkTheme) {
        AppScaffold(mainViewModel, settings)
    }
}

@Composable
private fun AppScaffold(mainViewModel: MainViewModel, settings: AppSettings) {
    val tabs = remember(settings.tabOrder) {
        settings.tabOrder.mapNotNull(Tab::byId).ifEmpty { Tab.entries.toList() }
    }
    var selectedTabId by rememberSaveable { mutableStateOf(Tab.CHATS.id) }
    var showNodes by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showNodes) { showNodes = false }

    Scaffold(
        topBar = { NodeTopBar(mainViewModel, onOpenNodes = { showNodes = true }) },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTabId == tab.id && !showNodes,
                        onClick = {
                            selectedTabId = tab.id
                            showNodes = false
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                showNodes -> NodesScreen(onClose = { showNodes = false })
                selectedTabId == Tab.CHATS.id -> PlaceholderScreen(
                    Icons.AutoMirrored.Outlined.Chat,
                    stringResource(R.string.placeholder_chats_title),
                    stringResource(R.string.placeholder_chats_body),
                )
                selectedTabId == Tab.ACTIVITY.id -> PlaceholderScreen(
                    Icons.Outlined.Bolt,
                    stringResource(R.string.placeholder_activity_title),
                    stringResource(R.string.placeholder_activity_body),
                )
                selectedTabId == Tab.PROJECTS.id -> ProjectsScreen(mainViewModel)
                selectedTabId == Tab.TASKS.id -> PlaceholderScreen(
                    Icons.Outlined.Checklist,
                    stringResource(R.string.placeholder_tasks_title),
                    stringResource(R.string.placeholder_tasks_body),
                )
                else -> SettingsScreen(
                    mainViewModel,
                    onOpenNodes = { showNodes = true },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeTopBar(mainViewModel: MainViewModel, onOpenNodes: () -> Unit) {
    val book by mainViewModel.nodeBook.collectAsStateWithLifecycle()
    val health by mainViewModel.health.collectAsStateWithLifecycle()
    val checking by mainViewModel.checking.collectAsStateWithLifecycle()

    val nodes = book.nodes
    val text = when {
        nodes.isEmpty() -> stringResource(R.string.node_bar_no_node)
        checking && health.isEmpty() -> stringResource(R.string.node_bar_checking)
        else -> {
            val online = nodes.count { health[it.id]?.ok == true }
            stringResource(R.string.nodes_aggregate_ok, online, nodes.size)
        }
    }
    val dotColor = when {
        nodes.isEmpty() -> MaterialTheme.colorScheme.outline
        nodes.any { health[it.id]?.ok == true } -> Color(0xFF22C55E)
        checking -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.error
    }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(onClick = onOpenNodes),
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(dotColor, CircleShape),
                )
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }
        },
    )
}

@Composable
fun PlaceholderScreen(icon: ImageVector, title: String, body: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
