package app.mindfs.android.core.network

import app.mindfs.android.core.model.AgentSummary
import app.mindfs.android.core.model.AgentsResponse
import app.mindfs.android.core.model.NodeHealth
import app.mindfs.android.core.model.RootSummary
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** REST surface of one MindFS node. v110 covers health/roots/agents; sessions and
 *  the /ws event stream join in v111–v112. */
class MindfsClient(
    baseUrl: String,
    private val json: Json,
    engine: HttpClientEngine? = null,
) {
    private val base = baseUrl.trimEnd('/')

    private fun configure(): HttpClientConfig<*>.() -> Unit = {
        expectSuccess = true
        install(ContentNegotiation) { json(this@MindfsClient.json) }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
    }

    private val client: HttpClient =
        if (engine != null) HttpClient(engine, configure()) else HttpClient(OkHttp, configure())

    /** GET /health — MindFS answers the literal text "ok". */
    suspend fun health(): NodeHealth = apiCall {
        val startedAt = System.nanoTime()
        val body = client.get("$base/health").bodyAsText()
        val latencyMs = ((System.nanoTime() - startedAt) / 1_000_000L).toInt()
        if (body.trim().equals("ok", ignoreCase = true)) {
            NodeHealth(ok = true, latencyMs = latencyMs)
        } else {
            NodeHealth(ok = false, latencyMs = latencyMs, detail = "Unexpected /health body")
        }
    }

    /** GET /api/dirs — workspace roots on the node. */
    suspend fun roots(): List<RootSummary> = apiCall {
        client.get("$base/api/dirs").body()
    }

    /** GET /api/agents?all=1 — agents installed on the node. */
    suspend fun agents(): List<AgentSummary> = apiCall {
        client.get("$base/api/agents") { parameter("all", "1") }.body<AgentsResponse>().agents
    }

    fun close() {
        client.close()
    }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 10_000L
        const val CONNECT_TIMEOUT_MS = 5_000L
    }
}
