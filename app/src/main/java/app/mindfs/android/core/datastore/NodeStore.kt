package app.mindfs.android.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.mindfs.android.core.model.NodeProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** The node list plus which node the app is currently pointed at. */
@Serializable
data class NodeBook(
    val nodes: List<NodeProfile> = emptyList(),
    val selectedId: String? = null,
)

/** Pure (de)serialization so the codec is unit-testable without a DataStore. */
object NodeBookCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(book: NodeBook): String = json.encodeToString(book)

    fun decode(raw: String?): NodeBook =
        raw?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<NodeBook>(it) }.getOrNull() }
            ?: NodeBook()
}

class NodeStore(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("node_book_json")

    val book: Flow<NodeBook> = dataStore.data.map { NodeBookCodec.decode(it[key]) }

    suspend fun upsert(node: NodeProfile) = save { book ->
        book.copy(nodes = book.nodes.filterNot { it.id == node.id } + node)
    }

    suspend fun remove(nodeId: String) = save { book ->
        book.copy(
            nodes = book.nodes.filterNot { it.id == nodeId },
            selectedId = book.selectedId?.takeUnless { it == nodeId },
        )
    }

    suspend fun select(nodeId: String) = save { it.copy(selectedId = nodeId) }

    private suspend fun save(transform: (NodeBook) -> NodeBook) {
        dataStore.edit { prefs ->
            val updated = transform(NodeBookCodec.decode(prefs[key]))
            prefs[key] = NodeBookCodec.encode(updated)
        }
    }
}
