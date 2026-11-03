package com.android.messaging.ui.appsettings.privacy.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.LinkOpeningMode
import com.android.messaging.ui.appsettings.common.SettingsClickableItem
import com.android.messaging.ui.appsettings.common.SettingsSwitchItem
import com.android.messaging.ui.appsettings.common.SettingsTopAppBar
import com.android.messaging.ui.appsettings.general.model.AppSettingsAction as Action
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
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
    var showLinkOpeningDialog by rememberSaveable { mutableStateOf(false) }

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
            item(key = "link_opening") {
                SettingsClickableItem(
                    title = stringResource(R.string.link_opening_pref_title),
                    summary = stringResource(appSettings.linkOpeningMode.titleResId()),
                    onClick = { showLinkOpeningDialog = true },
                )
            }
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

    if (showLinkOpeningDialog) {
        LinkOpeningDialog(
            mode = appSettings.linkOpeningMode,
            onDismiss = { showLinkOpeningDialog = false },
            onConfirm = { mode ->
                showLinkOpeningDialog = false
                onAction(Action.LinkOpeningModeChanged(mode))
            },
        )
    }
}

@Composable
private fun LinkOpeningDialog(
    mode: LinkOpeningMode,
    onDismiss: () -> Unit,
    onConfirm: (LinkOpeningMode) -> Unit,
) {
    var selectedMode by rememberSaveable { mutableStateOf(mode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.link_opening_pref_title))
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                LinkOpeningMode.entries.forEach { option ->
                    LinkOpeningOption(
                        mode = option,
                        selected = option == selectedMode,
                        onClick = { selectedMode = option },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMode) }) {
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
private fun LinkOpeningOption(
    mode: LinkOpeningMode,
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
        Column {
            Text(
                text = stringResource(mode.titleResId()),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(mode.summaryResId()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun LinkOpeningMode.titleResId(): Int {
    return when (this) {
        LinkOpeningMode.AskBeforeOpening -> R.string.link_opening_ask
        LinkOpeningMode.OpenOnTap -> R.string.link_opening_open
        LinkOpeningMode.DoNotOpen -> R.string.link_opening_off
    }
}

@StringRes
private fun LinkOpeningMode.summaryResId(): Int {
    return when (this) {
        LinkOpeningMode.AskBeforeOpening -> R.string.link_opening_ask_summary
        LinkOpeningMode.OpenOnTap -> R.string.link_opening_open_summary
        LinkOpeningMode.DoNotOpen -> R.string.link_opening_off_summary
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

@PreviewLightDark
@Composable
private fun LinkOpeningDialogPreview() {
    MessagingPreviewTheme {
        LinkOpeningDialog(
            mode = LinkOpeningMode.AskBeforeOpening,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
