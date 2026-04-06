package com.android.messaging.ui.appsettings.general.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.ui.appsettings.common.SettingsCategoryHeader
import com.android.messaging.ui.appsettings.common.SettingsClickableItem
import com.android.messaging.ui.appsettings.common.SettingsSwitchItem
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.screen.SettingsScreenModel
import com.android.messaging.ui.appsettings.screen.model.SettingsAction as Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppSettingsScreen(
    appSettings: AppSettingsUiState,
    screenModel: SettingsScreenModel,
    onNavigateBack: () -> Unit,
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
            LargeTopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            coreSettingsItems(appSettings, screenModel)
            if (isTopLevel && onAdvancedClick != null) {
                advancedSettingsItem(onAdvancedClick)
            }
            debugSettingsItems(appSettings, screenModel)
            licenseSettingsItems(screenModel)
        }
    }
}

private fun LazyListScope.coreSettingsItems(
    appSettings: AppSettingsUiState,
    screenModel: SettingsScreenModel,
) {
    item(key = "default_sms_app") {
        SettingsClickableItem(
            title = stringResource(R.string.sms_disabled_pref_title),
            summary = appSettings.defaultSmsAppLabel,
            onClick = {
                screenModel.onAction(Action.DefaultSmsAppClicked(appSettings.isDefaultSmsApp))
            },
        )
    }

    item(key = "notifications") {
        SettingsClickableItem(
            title = stringResource(R.string.notifications_enabled_conversation_pref_title),
            onClick = {
                screenModel.onAction(Action.NotificationsClicked)
            },
        )
    }

    item(key = "send_sound") {
        SettingsSwitchItem(
            title = stringResource(R.string.send_sound_pref_title),
            checked = appSettings.sendSoundEnabled,
            onCheckedChange = {
                screenModel.onAction(Action.SendSoundChanged(it))
            },
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
    screenModel: SettingsScreenModel,
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
                screenModel.onAction(Action.DumpSmsChanged(it))
            },
        )
    }
    item(key = "dump_mms") {
        SettingsSwitchItem(
            title = stringResource(R.string.dump_mms_pref_title),
            summary = stringResource(R.string.dump_mms_pref_summary),
            checked = appSettings.dumpMmsEnabled,
            onCheckedChange = {
                screenModel.onAction(Action.DumpMmsChanged(it))
            },
        )
    }
}

private fun LazyListScope.licenseSettingsItems(screenModel: SettingsScreenModel) {
    item(key = "licenses_divider") {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    item(key = "licenses") {
        SettingsClickableItem(
            title = stringResource(R.string.menu_license),
            onClick = {
                screenModel.onAction(Action.LicensesClicked)
            },
        )
    }
}
