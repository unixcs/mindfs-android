package app.mindfs.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One MindFS node (device) the app talks to. */
@Serializable
data class NodeProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    // Plain-HTTP nodes are only usable after an explicit in-app confirmation.
    val confirmedCleartext: Boolean = false,
    val createdAtEpochMs: Long = 0L,
)

fun normalizeBaseUrl(raw: String): String = raw.trim().trimEnd('/')

fun isValidNodeUrl(raw: String): Boolean {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return false
    return runCatching {
        val uri = java.net.URI(trimmed)
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}

/** GET /api/dirs item: a workspace root on the node. */
@Serializable
data class RootSummary(
    val id: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("root_path") val rootPath: String = "",
    @SerialName("is_git_repo") val isGitRepo: Boolean = false,
)

/** GET /api/agents item. Unknown server fields are tolerated via ignoreUnknownKeys. */
@Serializable
data class AgentSummary(
    val name: String,
    val installed: Boolean = false,
    val available: Boolean = false,
    val version: String? = null,
    val current: Boolean = false,
)

@Serializable
data class AgentsResponse(
    val agents: List<AgentSummary> = emptyList(),
)

/** Result of one health probe against a node. latencyMs is -1 when the probe failed. */
data class NodeHealth(
    val ok: Boolean,
    val latencyMs: Int,
    val detail: String? = null,
)
