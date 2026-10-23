package app.mindfs.android.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mindfs.android.BuildConfig
import app.mindfs.android.R
import app.mindfs.android.core.datastore.AppSettings
import app.mindfs.android.feature.main.MainViewModel
import app.mindfs.android.feature.main.Tab

@Composable
fun SettingsScreen(mainViewModel: MainViewModel, onOpenNodes: () -> Unit) {
    val settingsState = mainViewModel.settings.collectAsStateWithLifecycle()
    val settings = settingsState.value ?: return
    val uriHandler = LocalUriHandler.current

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        item { SectionLabel(stringResource(R.string.settings_section_nodes)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.nodes_title)) },
                leadingContent = {
                    Icon(Icons.Outlined.Dns, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onOpenNodes),
            )
        }
        item { HorizontalDivider() }

        item { SectionLabel(stringResource(R.string.settings_section_appearance)) }
        item {
            ThemeChips(settings, onSelect = { mainViewModel.setThemeMode(it) })
        }
        item {
            StartTabChips(settings, onSelect = { mainViewModel.setStartTab(it) })
        }
        item { HorizontalDivider() }

        item { SectionLabel(stringResource(R.string.settings_section_about)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about_source)) },
                supportingContent = { Text(SOURCE_URL) },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                },
                modifier = Modifier.clickable { uriHandler.openUri(SOURCE_URL) },
            )
        }
        item {
            Text(
                stringResource(R.string.settings_about_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            Text(
                stringResource(R.string.settings_about_license),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeChips(settings: AppSettings, onSelect: (String) -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = settings.themeMode == AppSettings.THEME_SYSTEM,
            onClick = { onSelect(AppSettings.THEME_SYSTEM) },
            label = { Text(stringResource(R.string.settings_theme_system)) },
        )
        FilterChip(
            selected = settings.themeMode == AppSettings.THEME_DARK,
            onClick = { onSelect(AppSettings.THEME_DARK) },
            label = { Text(stringResource(R.string.settings_theme_dark)) },
        )
        FilterChip(
            selected = settings.themeMode == AppSettings.THEME_LIGHT,
            onClick = { onSelect(AppSettings.THEME_LIGHT) },
            label = { Text(stringResource(R.string.settings_theme_light)) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StartTabChips(settings: AppSettings, onSelect: (String) -> Unit) {
    FlowRow(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tab.entries.forEach { tab ->
            FilterChip(
                selected = settings.startTab == tab.id,
                onClick = { onSelect(tab.id) },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private const val SOURCE_URL = "https://github.com/unixcs/mindfs-android"
