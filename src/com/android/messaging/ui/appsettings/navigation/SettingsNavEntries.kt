package com.android.messaging.ui.appsettings.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.appsettings.screen.SettingsScreen
import com.android.messaging.ui.appsettings.screen.rememberSettingsEffectHandler
import com.android.messaging.ui.license.navigation.LicenseNavKey
import com.android.messaging.ui.navigation.LocalNavigator

internal fun EntryProviderScope<NavKey>.settingsEntries() {
    entry<SettingsNavKey>(
        content = settingsRouteContent(),
    )
}

private fun settingsRouteContent(): @Composable (SettingsNavKey) -> Unit {
    return {
        val navigator = LocalNavigator.current
        val effectHandler = rememberSettingsEffectHandler()

        SettingsScreen(
            effectHandler = effectHandler,
            onNavigateBack = navigator::back,
            onNavigateToLicenses = {
                navigator.push(destination = LicenseNavKey)
            },
        )
    }
}
