package app.mindfs.android.core.data

import app.mindfs.android.core.datastore.NodeBook
import app.mindfs.android.core.datastore.NodeStore
import app.mindfs.android.core.model.NodeHealth
import app.mindfs.android.core.model.NodeProfile
import app.mindfs.android.core.network.MindfsClient
import app.mindfs.android.core.network.MindfsJson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NodeCheck(val node: NodeProfile, val health: NodeHealth)

/** Owns one long-lived [MindfsClient] per node and the health-check fan-out. */
@Singleton
class NodeRepository @Inject constructor(
    private val nodeStore: NodeStore,
) {
    val book: Flow<NodeBook> = nodeStore.book

    private val clientsMutex = Mutex()
    private val clients = mutableMapOf<String, MindfsClient>()

    suspend fun clientFor(node: NodeProfile): MindfsClient = clientsMutex.withLock {
        clients.getOrPut(clientKey(node)) { MindfsClient(node.baseUrl, MindfsJson) }
    }

    suspend fun evictClient(node: NodeProfile) {
        clientsMutex.withLock { clients.remove(clientKey(node))?.close() }
    }

    suspend fun check(node: NodeProfile): NodeCheck {
        val health = try {
            clientFor(node).health()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failed: Exception) {
            NodeHealth(ok = false, latencyMs = -1, detail = failed.message ?: failed::class.java.simpleName)
        }
        return NodeCheck(node, health)
    }

    suspend fun checkAll(nodes: List<NodeProfile>): List<NodeCheck> = coroutineScope {
        nodes.map { node -> async { check(node) } }.awaitAll()
    }

    private fun clientKey(node: NodeProfile): String = "${node.id}@${node.baseUrl}"
}
