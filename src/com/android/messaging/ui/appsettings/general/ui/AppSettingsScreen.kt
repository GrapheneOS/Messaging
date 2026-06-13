package com.android.messaging.ui.appsettings.general.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.AppColorScheme
import com.android.messaging.ui.appsettings.common.SettingsCategoryHeader
import com.android.messaging.ui.appsettings.common.SettingsClickableItem
import com.android.messaging.ui.appsettings.common.SettingsSwitchItem
import com.android.messaging.ui.appsettings.common.SettingsTopAppBar
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.screen.model.SettingsAction as Action
import com.android.messaging.ui.core.MessagingPreviewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppSettingsScreen(
    appSettings: AppSettingsUiState,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isTopLevel: Boolean = false,
    onAdvancedClick: (() -> Unit)? = null,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showColorSchemeDialog by remember { mutableStateOf(false) }
    val title = if (isTopLevel) {
        stringResource(R.string.settings_activity_title)
    } else {
        stringResource(R.string.general_settings_activity_title)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SettingsTopAppBar(
                title = title,
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            coreSettingsItems(
                appSettings = appSettings,
                onAction = onAction,
                onColorSchemeClick = { showColorSchemeDialog = true },
            )

            if (isTopLevel && onAdvancedClick != null) {
                advancedSettingsItem(onAdvancedClick)
            }

            debugSettingsItems(
                appSettings = appSettings,
                onAction = onAction,
            )

            licenseSettingsItems(onAction)
        }
    }

    if (showColorSchemeDialog) {
        ColorSchemeDialog(
            selected = appSettings.colorScheme,
            onDismiss = { showColorSchemeDialog = false },
            onConfirm = { colorScheme ->
                onAction(Action.ColorSchemeChanged(colorScheme))
                showColorSchemeDialog = false
            },
        )
    }
}

private fun LazyListScope.coreSettingsItems(
    appSettings: AppSettingsUiState,
    onAction: (Action) -> Unit,
    onColorSchemeClick: () -> Unit,
) {
    item(key = "default_sms_app") {
        SettingsClickableItem(
            title = stringResource(R.string.sms_disabled_pref_title),
            summary = appSettings.defaultSmsAppLabel,
            onClick = {
                onAction(Action.DefaultSmsAppClicked(appSettings.isDefaultSmsApp))
            },
        )
    }

    item(key = "notifications") {
        SettingsClickableItem(
            title = stringResource(R.string.notifications_enabled_conversation_pref_title),
            onClick = {
                onAction(Action.NotificationsClicked)
            },
        )
    }

    item(key = "send_sound") {
        SettingsSwitchItem(
            title = stringResource(R.string.send_sound_pref_title),
            checked = appSettings.sendSoundEnabled,
            onCheckedChange = {
                onAction(Action.SendSoundChanged(it))
            },
        )
    }

    item(key = "color_scheme") {
        SettingsClickableItem(
            title = stringResource(R.string.color_scheme_pref_title),
            summary = stringResource(appSettings.colorScheme.titleResId),
            onClick = onColorSchemeClick,
        )
    }
}

private fun LazyListScope.advancedSettingsItem(onAdvancedClick: () -> Unit) {
    item(key = "advanced_settings") {
        SettingsClickableItem(
            title = stringResource(R.string.advanced_settings),
            onClick = onAdvancedClick,
        )
    }
}

private fun LazyListScope.debugSettingsItems(
    appSettings: AppSettingsUiState,
    onAction: (Action) -> Unit,
) {
    if (!appSettings.isDebugEnabled) return

    item(key = "debug_divider") {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    item(key = "debug_category_header") {
        SettingsCategoryHeader(
            title = stringResource(R.string.debug_category_pref_title),
        )
    }
    item(key = "dump_sms") {
        SettingsSwitchItem(
            title = stringResource(R.string.dump_sms_pref_title),
            summary = stringResource(R.string.dump_sms_pref_summary),
            checked = appSettings.dumpSmsEnabled,
            onCheckedChange = {
                onAction(Action.DumpSmsChanged(it))
            },
        )
    }
    item(key = "dump_mms") {
        SettingsSwitchItem(
            title = stringResource(R.string.dump_mms_pref_title),
            summary = stringResource(R.string.dump_mms_pref_summary),
            checked = appSettings.dumpMmsEnabled,
            onCheckedChange = {
                onAction(Action.DumpMmsChanged(it))
            },
        )
    }
}

private fun LazyListScope.licenseSettingsItems(
    onAction: (Action) -> Unit,
) {
    item(key = "licenses_divider") {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    item(key = "licenses") {
        SettingsClickableItem(
            title = stringResource(R.string.menu_license),
            onClick = {
                onAction(Action.LicensesClicked)
            },
        )
    }
}

@Composable
private fun ColorSchemeDialog(
    selected: AppColorScheme,
    onDismiss: () -> Unit,
    onConfirm: (AppColorScheme) -> Unit,
) {
    var selectedScheme by remember { mutableStateOf(selected) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.color_scheme_pref_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.color_scheme_pref_restart_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.selectableGroup()) {
                    AppColorScheme.entries.forEach { scheme ->
                        ColorSchemeOption(
                            text = stringResource(scheme.titleResId),
                            selected = scheme == selectedScheme,
                            onClick = { selectedScheme = scheme },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedScheme) }) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ColorSchemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@PreviewLightDark
@Composable
private fun ColorSchemeDialogPreview() {
    MessagingPreviewTheme {
        ColorSchemeDialog(
            selected = AppColorScheme.DYNAMIC,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AppSettingsScreenTopLevelPreview() {
    MessagingPreviewTheme {
        AppSettingsScreen(
            appSettings = AppSettingsUiState(
                isDefaultSmsApp = true,
                defaultSmsAppLabel = "Messaging",
                sendSoundEnabled = true,
            ),
            onAction = {},
            onNavigateBack = {},
            isTopLevel = true,
            onAdvancedClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AppSettingsScreenDebugPreview() {
    MessagingPreviewTheme {
        AppSettingsScreen(
            appSettings = AppSettingsUiState(
                isDefaultSmsApp = false,
                defaultSmsAppLabel = "Phone",
                sendSoundEnabled = false,
                isDebugEnabled = true,
                dumpSmsEnabled = true,
                dumpMmsEnabled = false,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
