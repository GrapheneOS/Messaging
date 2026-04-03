package com.android.messaging.ui.appsettings.screen.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface SettingsNavRoute {

    val depth: Int

    data object Main : SettingsNavRoute {
        override val depth: Int = 0
    }

    data object AppSettings : SettingsNavRoute {
        override val depth: Int = 1
    }

    data class SubscriptionSettings(val subId: Int, val title: String) : SettingsNavRoute {
        override val depth: Int = 2
    }
}
