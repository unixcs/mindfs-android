package app.mindfs.android.feature.server

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mindfs.android.R
import app.mindfs.android.core.model.NodeHealth
import app.mindfs.android.core.model.NodeProfile
import app.mindfs.android.core.model.isValidNodeUrl
import app.mindfs.android.core.model.normalizeBaseUrl

private val OkGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodesScreen(onClose: () -> Unit, nodesViewModel: NodesViewModel = hiltViewModel()) {
    val book by nodesViewModel.book.collectAsStateWithLifecycle()
    val testResults by nodesViewModel.testResults.collectAsStateWithLifecycle()
    val testingIds by nodesViewModel.testingIds.collectAsStateWithLifecycle()

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<NodeProfile?>(null) }
    var deletingNode by remember { mutableStateOf<NodeProfile?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.nodes_title)) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
            actions = {
                if (book.nodes.size > 1) {
                    IconButton(onClick = { nodesViewModel.testAll() }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.action_check_all),
                        )
                    }
                }
            },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (book.nodes.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Outlined.Dns,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        stringResource(R.string.nodes_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = { editingNode = null; showEditor = true }) {
                        Text(stringResource(R.string.nodes_add))
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(book.nodes, key = { it.id }) { node ->
                        NodeCard(
                            node = node,
                            selected = node.id == book.selectedId,
                            testing = node.id in testingIds,
                            test = testResults[node.id],
                            onSelect = { nodesViewModel.select(node.id) },
                            onTest = { nodesViewModel.test(node) },
                            onEdit = { editingNode = node; showEditor = true },
                            onDelete = { deletingNode = node },
                        )
                    }
                }
            }
            FloatingActionButton(
                onClick = { editingNode = null; showEditor = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.nodes_add))
            }
        }
    }

    if (showEditor) {
        NodeEditorDialog(
            editing = editingNode,
            existing = book.nodes,
            onDismiss = { showEditor = false },
            onSave = { name, url, confirmed ->
                nodesViewModel.upsert(name, url, confirmed, editingNode)
                showEditor = false
            },
        )
    }

    deletingNode?.let { node ->
        AlertDialog(
            onDismissRequest = { deletingNode = null },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.nodes_delete_confirm, node.name)) },
            confirmButton = {
                TextButton(onClick = {
                    nodesViewModel.delete(node)
                    deletingNode = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingNode = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun NodeCard(
    node: NodeProfile,
    selected: Boolean,
    testing: Boolean,
    test: NodeHealth?,
    onSelect: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(onClick = onSelect) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(node.name, style = MaterialTheme.typography.titleMedium)
                if (selected) {
                    Text(
                        stringResource(R.string.nodes_selected),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.nodes_edit),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                node.baseUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    testing -> {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text(
                            stringResource(R.string.node_bar_checking),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    test == null -> {}
                    test.ok -> Text(
                        stringResource(R.string.nodes_test_ok, test.latencyMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = OkGreen,
                    )
                    else -> Text(
                        stringResource(R.string.nodes_test_failed, test.detail ?: "error"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onTest) { Text(stringResource(R.string.action_test)) }
            }
        }
    }
}

@Composable
private fun NodeEditorDialog(
    editing: NodeProfile?,
    existing: List<NodeProfile>,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, cleartextConfirmed: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var url by remember { mutableStateOf(editing?.baseUrl ?: "") }
    var confirmed by remember { mutableStateOf(editing?.confirmedCleartext ?: false) }

    val trimmedUrl = url.trim()
    val cleartext = trimmedUrl.startsWith("http://", ignoreCase = true)
    val urlInvalid = trimmedUrl.isNotEmpty() && !isValidNodeUrl(trimmedUrl)
    val duplicate = existing.any {
        it.baseUrl == normalizeBaseUrl(trimmedUrl) && it.id != editing?.id
    }
    val canSave = name.isNotBlank() && !urlInvalid && !duplicate &&
        (!cleartext || confirmed) && isValidNodeUrl(trimmedUrl)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (editing == null) R.string.nodes_add else R.string.nodes_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.nodes_add_name)) },
                    placeholder = { Text(stringResource(R.string.nodes_add_name_hint)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        confirmed = editing?.confirmedCleartext ?: false
                    },
                    label = { Text(stringResource(R.string.nodes_add_url)) },
                    placeholder = { Text(stringResource(R.string.nodes_add_url_hint)) },
                    singleLine = true,
                    isError = urlInvalid || duplicate,
                    supportingText = {
                        when {
                            urlInvalid -> Text(
                                stringResource(R.string.nodes_error_url),
                                color = MaterialTheme.colorScheme.error,
                            )
                            duplicate -> Text(
                                stringResource(R.string.nodes_error_duplicate),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )
                if (cleartext) {
                    Text(
                        stringResource(R.string.nodes_cleartext_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                        Text(
                            stringResource(R.string.nodes_cleartext_confirm),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { confirmed = !confirmed },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = { onSave(name, url, confirmed) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
