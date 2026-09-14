package com.android.messaging.ui.appsettings.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.subscription.model.SubId
import kotlinx.serialization.Serializable

@Serializable
internal data class SubscriptionSettingsNavKey(
    val subId: SubId,
    val title: String,
) : NavKey
