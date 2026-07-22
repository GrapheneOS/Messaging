package com.android.messaging.ui.appsettings.privacy.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.appsettings.common.SettingsSwitchItem
import com.android.messaging.ui.appsettings.common.SettingsTopAppBar
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.screen.model.SettingsAction as Action
import com.android.messaging.ui.core.MessagingPreviewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrivacySettingsScreen(
    appSettings: AppSettingsUiState,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SettingsTopAppBar(
                title = stringResource(R.string.privacy_settings_activity_title),
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item(key = "youtube_link_previews") {
                SettingsSwitchItem(
                    title = stringResource(R.string.youtube_link_previews_pref_title),
                    summary = stringResource(R.string.youtube_link_previews_pref_summary),
                    checked = appSettings.youTubeLinkPreviewsEnabled,
                    onCheckedChange = {
                        onAction(Action.YouTubeLinkPreviewsChanged(it))
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PrivacySettingsScreenPreview() {
    MessagingPreviewTheme {
        PrivacySettingsScreen(
            appSettings = AppSettingsUiState(
                youTubeLinkPreviewsEnabled = false,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
