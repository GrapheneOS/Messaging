package com.android.messaging.data.subscriptionsettings.model

import com.android.messaging.data.subscription.model.SubId
import kotlinx.collections.immutable.ImmutableList

internal data class SubscriptionSettingsData(
    val isDefaultSmsApp: Boolean,
    val activeSubscriptionCount: Int,
    val isCellBroadcastAppEnabled: Boolean,
    val defaultSelfSubscription: PerSubscriptionData,
    val nonDefaultActiveSelfSubscriptions: ImmutableList<PerSubscriptionData>,
)

internal data class PerSubscriptionData(
    val subId: SubId,
    val subscriptionName: String?,
    val savedPhoneNumber: String,
    val defaultPhoneNumber: String,
    val formattedSavedPhoneNumber: String?,
    val formattedDefaultPhoneNumber: String?,
    val isGroupMmsSupported: Boolean,
    val isGroupMmsEnabled: Boolean,
    val autoRetrieveMms: Boolean,
    val autoRetrieveMmsWhenRoaming: Boolean,
    val isDeliveryReportsSupported: Boolean,
    val deliveryReportsEnabled: Boolean,
    val showCellBroadcast: Boolean,
)
