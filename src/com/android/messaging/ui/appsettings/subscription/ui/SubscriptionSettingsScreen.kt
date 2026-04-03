package com.android.messaging.ui.appsettings.subscription.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.appsettings.common.SettingsCategoryHeader
import com.android.messaging.ui.appsettings.common.SettingsClickableItem
import com.android.messaging.ui.appsettings.common.SettingsSwitchItem
import com.android.messaging.ui.appsettings.screen.SettingsScreenModel
import com.android.messaging.ui.appsettings.screen.model.SettingsAction as Action
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.ui.core.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionSettingsScreen(
    subscriptionSettings: SubscriptionSettingsUiState,
    title: String,
    screenModel: SettingsScreenModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showGroupMmsDialog by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
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
            mmsSettingsItems(
                subscriptionSettings = subscriptionSettings,
                screenModel = screenModel,
                onGroupMmsClick = { showGroupMmsDialog = true },
                onPhoneNumberClick = { showPhoneNumberDialog = true },
            )
            advancedSettingsItems(subscriptionSettings, screenModel)
        }
    }

    SubscriptionDialogs(
        subscriptionSettings = subscriptionSettings,
        screenModel = screenModel,
        showGroupMmsDialog = showGroupMmsDialog,
        onDismissGroupMms = { showGroupMmsDialog = false },
        showPhoneNumberDialog = showPhoneNumberDialog,
        onDismissPhoneNumber = { showPhoneNumberDialog = false },
    )
}

@Composable
private fun SubscriptionDialogs(
    subscriptionSettings: SubscriptionSettingsUiState,
    screenModel: SettingsScreenModel,
    showGroupMmsDialog: Boolean,
    onDismissGroupMms: () -> Unit,
    showPhoneNumberDialog: Boolean,
    onDismissPhoneNumber: () -> Unit,
) {
    if (showGroupMmsDialog) {
        GroupMmsDialog(
            isEnabled = subscriptionSettings.isGroupMmsEnabled,
            onDismiss = onDismissGroupMms,
            onConfirm = { enabled ->
                screenModel.onAction(
                    Action.GroupMmsChanged(subscriptionSettings.subId, enabled),
                )
                onDismissGroupMms()
            },
        )
    }

    if (showPhoneNumberDialog) {
        PhoneNumberDialog(
            currentNumber = subscriptionSettings.phoneNumber.ifEmpty {
                subscriptionSettings.defaultPhoneNumber
            },
            onDismiss = onDismissPhoneNumber,
            onConfirm = { phoneNumber ->
                screenModel.onAction(
                    Action.PhoneNumberChanged(subscriptionSettings.subId, phoneNumber),
                )
                onDismissPhoneNumber()
            },
        )
    }
}

private fun LazyListScope.mmsSettingsItems(
    subscriptionSettings: SubscriptionSettingsUiState,
    screenModel: SettingsScreenModel,
    onGroupMmsClick: () -> Unit,
    onPhoneNumberClick: () -> Unit,
) {
    item(key = "mms_category_header") {
        SettingsCategoryHeader(
            title = stringResource(R.string.mms_messaging_category_pref_title),
        )
    }

    if (subscriptionSettings.isGroupMmsSupported) {
        item(key = "group_mms") {
            SettingsClickableItem(
                title = stringResource(R.string.group_mms_pref_title),
                summary = if (subscriptionSettings.isGroupMmsEnabled) {
                    stringResource(R.string.enable_group_mms)
                } else {
                    stringResource(R.string.disable_group_mms)
                },
                enabled = subscriptionSettings.isDefaultSmsApp,
                onClick = onGroupMmsClick,
            )
        }
    }

    item(key = "phone_number") {
        SettingsClickableItem(
            title = stringResource(R.string.mms_phone_number_pref_title),
            summary = subscriptionSettings.displayDetail,
            onClick = onPhoneNumberClick,
        )
    }

    item(key = "auto_retrieve_mms") {
        SettingsSwitchItem(
            title = stringResource(R.string.auto_retrieve_mms_pref_title),
            summary = stringResource(R.string.auto_retrieve_mms_pref_summary),
            checked = subscriptionSettings.autoRetrieveMms,
            enabled = subscriptionSettings.isDefaultSmsApp,
            onCheckedChange = { enabled ->
                screenModel.onAction(
                    Action.AutoRetrieveMmsChanged(subscriptionSettings.subId, enabled),
                )
            },
        )
    }

    item(key = "auto_retrieve_mms_roaming") {
        SettingsSwitchItem(
            title = stringResource(R.string.auto_retrieve_mms_when_roaming_pref_title),
            summary = stringResource(R.string.auto_retrieve_mms_when_roaming_pref_summary),
            checked = subscriptionSettings.autoRetrieveMmsWhenRoaming,
            enabled = subscriptionSettings.isDefaultSmsApp && subscriptionSettings.autoRetrieveMms,
            onCheckedChange = { enabled ->
                screenModel.onAction(
                    Action.AutoRetrieveMmsWhenRoamingChanged(subscriptionSettings.subId, enabled),
                )
            },
        )
    }
}

private fun LazyListScope.advancedSettingsItems(
    subscriptionSettings: SubscriptionSettingsUiState,
    screenModel: SettingsScreenModel,
) {
    val hasAdvanced = subscriptionSettings.isDeliveryReportsSupported ||
        subscriptionSettings.isWirelessAlertsSupported
    if (!hasAdvanced) return

    item(key = "advanced_divider") {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    item(key = "advanced_category_header") {
        SettingsCategoryHeader(
            title = stringResource(R.string.advanced_category_pref_title),
        )
    }

    if (subscriptionSettings.isDeliveryReportsSupported) {
        item(key = "delivery_reports") {
            SettingsSwitchItem(
                title = stringResource(R.string.delivery_reports_pref_title),
                summary = stringResource(R.string.delivery_reports_pref_summary),
                checked = subscriptionSettings.deliveryReportsEnabled,
                enabled = subscriptionSettings.isDefaultSmsApp,
                onCheckedChange = { enabled ->
                    screenModel.onAction(
                        Action.DeliveryReportsChanged(subscriptionSettings.subId, enabled),
                    )
                },
            )
        }
    }

    if (subscriptionSettings.isWirelessAlertsSupported) {
        item(key = "wireless_alerts") {
            SettingsClickableItem(
                title = stringResource(R.string.wireless_alerts_title),
                onClick = {
                    screenModel.onAction(
                        Action.WirelessAlertsClicked(subscriptionSettings.subId),
                    )
                },
            )
        }
    }
}

@Composable
private fun GroupMmsDialog(
    isEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    var selectedEnabled by remember { mutableStateOf(isEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.group_mms_pref_title))
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                GroupMmsOption(
                    text = stringResource(R.string.enable_group_mms),
                    selected = selectedEnabled,
                    onClick = { selectedEnabled = true },
                )
                GroupMmsOption(
                    text = stringResource(R.string.disable_group_mms),
                    selected = !selectedEnabled,
                    onClick = { selectedEnabled = false },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedEnabled) }) {
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
private fun GroupMmsOption(
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

@Composable
private fun PhoneNumberDialog(
    currentNumber: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var phoneNumber by remember { mutableStateOf(currentNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.mms_phone_number_pref_title))
        },
        text = {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(phoneNumber) }) {
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

@Preview
@Composable
private fun GroupMmsDialogPreview() {
    AppTheme {
        GroupMmsDialog(
            isEnabled = true,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun PhoneNumberDialogPreview() {
    AppTheme {
        PhoneNumberDialog(
            currentNumber = "+31 6 1234 5678",
            onDismiss = {},
            onConfirm = {},
        )
    }
}
