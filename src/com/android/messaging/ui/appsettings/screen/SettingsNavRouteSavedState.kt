package com.android.messaging.ui.appsettings.screen

import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import com.android.messaging.ui.appsettings.screen.model.SettingsNavRoute
import kotlinx.parcelize.Parcelize

internal sealed interface SettingsNavRouteSavedState : Parcelable {

    @Parcelize
    data object Main : SettingsNavRouteSavedState

    @Parcelize
    data object AppSettings : SettingsNavRouteSavedState

    @Parcelize
    data class SubscriptionSettings(
        val subId: Int,
        val title: String,
    ) : SettingsNavRouteSavedState

    companion object {
        val Saver: Saver<SettingsNavRoute, SettingsNavRouteSavedState> = Saver(
            save = { route -> route.toSavedState() },
            restore = { savedState -> savedState.toRoute() },
        )
    }
}

private fun SettingsNavRoute.toSavedState(): SettingsNavRouteSavedState {
    return when (this) {
        SettingsNavRoute.Main -> SettingsNavRouteSavedState.Main

        SettingsNavRoute.AppSettings -> SettingsNavRouteSavedState.AppSettings

        is SettingsNavRoute.SubscriptionSettings -> SettingsNavRouteSavedState.SubscriptionSettings(
            subId = subId,
            title = title,
        )
    }
}

private fun SettingsNavRouteSavedState.toRoute(): SettingsNavRoute {
    return when (this) {
        SettingsNavRouteSavedState.Main -> SettingsNavRoute.Main

        SettingsNavRouteSavedState.AppSettings -> SettingsNavRoute.AppSettings

        is SettingsNavRouteSavedState.SubscriptionSettings -> SettingsNavRoute.SubscriptionSettings(
            subId = subId,
            title = title,
        )
    }
}
