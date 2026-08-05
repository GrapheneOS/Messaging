package com.android.messaging.ui.appsettings.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.subscription.model.SubId

@Immutable
internal sealed interface SettingsNavRoute {

    val depth: Int

    data object Main : SettingsNavRoute {
        override val depth: Int = 0
    }

    data object AppSettings : SettingsNavRoute {
        override val depth: Int = 1
    }

    data class SubscriptionSettings(
        val subId: SubId,
        val title: String,
    ) : SettingsNavRoute {
        override val depth: Int = 2
    }
}
