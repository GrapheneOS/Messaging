package com.android.messaging.ui.appsettings.subscription.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.subscription.model.SubId

@Immutable
internal data class SubscriptionUiState(
    val subId: SubId = SubId(-1),
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
    val isSecondaryUser: Boolean = false,
)
