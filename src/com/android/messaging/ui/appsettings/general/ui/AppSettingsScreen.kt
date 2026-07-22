package com.android.messaging.ui.appsettings.general.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
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
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTopLevel: Boolean = false,
    onAdvancedClick: (() -> Unit)? = null,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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
                onPrivacyClick = onPrivacyClick,
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
}

private fun LazyListScope.coreSettingsItems(
    appSettings: AppSettingsUiState,
    onAction: (Action) -> Unit,
    onPrivacyClick: () -> Unit,
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

    item(key = "privacy") {
        SettingsClickableItem(
            title = stringResource(R.string.privacy_settings_activity_title),
            onClick = onPrivacyClick,
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
            onPrivacyClick = {},
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
            onPrivacyClick = {},
        )
    }
}
