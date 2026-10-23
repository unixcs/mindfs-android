package app.mindfs.android.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppSettings(
    val tabOrder: List<String> = DEFAULT_TAB_ORDER,
    val startTab: String = TAB_CHATS,
    val themeMode: String = THEME_SYSTEM,
) {
    companion object {
        const val TAB_ACTIVITY = "activity"
        const val TAB_CHATS = "chats"
        const val TAB_PROJECTS = "projects"
        const val TAB_TASKS = "tasks"
        const val TAB_SETTINGS = "settings"

        val DEFAULT_TAB_ORDER = listOf(TAB_ACTIVITY, TAB_CHATS, TAB_PROJECTS, TAB_TASKS, TAB_SETTINGS)

        const val THEME_SYSTEM = "system"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
    }
}

object AppSettingsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(settings: AppSettings): String = json.encodeToString(settings)

    fun decode(raw: String?): AppSettings =
        raw?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() }
            ?: AppSettings()
}

class AppSettingsStore(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("app_settings_json")

    val settings: Flow<AppSettings> = dataStore.data.map { AppSettingsCodec.decode(it[key]) }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            prefs[key] = AppSettingsCodec.encode(transform(AppSettingsCodec.decode(prefs[key])))
        }
    }
}
