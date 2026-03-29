package com.android.messaging.ui.appsettings.redesign.subscription.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SubscriptionSettingsUiState(
    val subId: Int = -1,
    val displayName: String = "",
    val displayDetail: String = "",
    val phoneNumber: String = "",
    val defaultPhoneNumber: String = "",
    val isGroupMmsSupported: Boolean = false,
    val isGroupMmsEnabled: Boolean = true,
    val autoRetrieveMms: Boolean = true,
    val autoRetrieveMmsWhenRoaming: Boolean = false,
    val isDeliveryReportsSupported: Boolean = false,
    val deliveryReportsEnabled: Boolean = false,
    val isWirelessAlertsSupported: Boolean = false,
    val isDefaultSmsApp: Boolean = false,
)
