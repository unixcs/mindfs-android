package app.mindfs.android.core.network

import app.mindfs.android.core.datastore.AppSettings
import app.mindfs.android.core.datastore.AppSettingsCodec
import app.mindfs.android.core.datastore.NodeBook
import app.mindfs.android.core.datastore.NodeBookCodec
import app.mindfs.android.core.model.NodeProfile
import app.mindfs.android.core.model.RootSummary
import app.mindfs.android.core.model.isValidNodeUrl
import app.mindfs.android.core.model.normalizeBaseUrl
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MindfsClientTest {

    private fun client(handler: MockRequestHandler): MindfsClient =
        MindfsClient(
            baseUrl = "http://node.test:7331",
            json = MindfsJson,
            engine = MockEngine(handler),
        )

    @Test
    fun `health reports ok with non-negative latency`() = runTest {
        val client = client {
            respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        val health = client.health()
        assertTrue(health.ok)
        assertTrue(health.latencyMs >= 0)
        client.close()
    }

    @Test
    fun `health fails on unexpected body`() = runTest {
        val client = client {
            respond("not ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        val health = client.health()
        assertFalse(health.ok)
        client.close()
    }

    @Test
    fun `roots decode ignoring unknown fields`() = runTest {
        val body = """
            [
              {"id":"r1","display_name":"929","root_path":"/mnt/Projects/929","is_git_repo":true,
               "is_git_worktree":false,"meta_location":"/mnt/Projects/929/.mindfs",
               "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-10-24T00:00:00Z","size":123},
              {"id":"r2"}
            ]
        """.trimIndent()
        val client = client {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val roots = client.roots()
        assertEquals(2, roots.size)
        assertEquals("929", roots[0].displayName)
        assertTrue(roots[0].isGitRepo)
        assertEquals("", roots[1].displayName)
        assertFalse(roots[1].isGitRepo)
        client.close()
    }

    @Test
    fun `agents unwrap the agents array`() = runTest {
        val body = """
            {"agents":[{"name":"codex","installed":true,"available":true,"version":"1.2.3",
                        "modes":["code"],"models":["gpt-5"]}],
             "shells":["bash","zsh"]}
        """.trimIndent()
        val client = client {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val agents = client.agents()
        assertEquals(1, agents.size)
        assertEquals("codex", agents[0].name)
        assertTrue(agents[0].installed)
        assertEquals("1.2.3", agents[0].version)
        client.close()
    }

    @Test
    fun `http error surfaces as ApiError Http`() = runTest {
        val client = client {
            respond("{}", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val error = runCatching { client.roots() }.exceptionOrNull()
        assertTrue("expected ApiError.Http, got $error", error is ApiError.Http)
        client.close()
    }

    @Test
    fun `base url normalization requests the right paths`() = runTest {
        val requested = mutableListOf<String>()
        val engine = MockEngine { request ->
            requested += request.url.encodedPath
            respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        // MindfsClient trims trailing slashes off the base URL.
        val client = MindfsClient("http://node.test:7331/", MindfsJson, engine)
        client.health()
        assertEquals(listOf("/health"), requested)
        client.close()
    }
}

class NodeBookAndUrlTest {

    @Test
    fun `node book round trip preserves nodes and selection`() {
        val book = NodeBook(
            nodes = listOf(
                NodeProfile(id = "n1", name = "dell", baseUrl = "http://10.0.0.1:7331", confirmedCleartext = true),
                NodeProfile(id = "n2", name = "tencent", baseUrl = "https://tx.example:8443"),
            ),
            selectedId = "n2",
        )
        val decoded = NodeBookCodec.decode(NodeBookCodec.encode(book))
        assertEquals(book, decoded)
    }

    @Test
    fun `garbage or blank state decodes to an empty book`() {
        assertEquals(NodeBook(), NodeBookCodec.decode(null))
        assertEquals(NodeBook(), NodeBookCodec.decode(""))
        assertEquals(NodeBook(), NodeBookCodec.decode("not json at all"))
    }

    @Test
    fun `app settings round trip preserves a reordered tab list`() {
        val settings = AppSettings(
            tabOrder = listOf(AppSettings.TAB_SETTINGS, AppSettings.TAB_CHATS, AppSettings.TAB_PROJECTS, AppSettings.TAB_TASKS, AppSettings.TAB_ACTIVITY),
            startTab = AppSettings.TAB_SETTINGS,
            themeMode = AppSettings.THEME_DARK,
        )
        assertEquals(settings, AppSettingsCodec.decode(AppSettingsCodec.encode(settings)))
        assertEquals(AppSettings(), AppSettingsCodec.decode("garbage"))
    }

    @Test
    fun `normalizeBaseUrl trims whitespace and trailing slashes`() {
        assertEquals("http://h:7331", normalizeBaseUrl(" http://h:7331/ "))
        assertEquals("https://h", normalizeBaseUrl("https://h///"))
    }

    @Test
    fun `url validation accepts only http(s) with a host`() {
        assertTrue(isValidNodeUrl("http://100.118.126.86:7331"))
        assertTrue(isValidNodeUrl("https://tx-tencent.taile8eadf.ts.net:3448"))
        assertFalse(isValidNodeUrl("ftp://h:1"))
        assertFalse(isValidNodeUrl("http://"))
        assertFalse(isValidNodeUrl("not a url"))
        assertFalse(isValidNodeUrl(""))
    }
}
