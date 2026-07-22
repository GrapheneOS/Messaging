package com.android.messaging.ui.license.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.license.LicenseScreen
import com.android.messaging.ui.navigation.LocalNavigator

internal fun EntryProviderScope<NavKey>.licenseEntries() {
    entry<LicenseNavKey>(
        content = licenseRouteContent(),
    )
}

private fun licenseRouteContent(): @Composable (LicenseNavKey) -> Unit {
    return {
        val navigator = LocalNavigator.current

        LicenseScreen(
            onNavigateBack = navigator::back,
        )
    }
}
