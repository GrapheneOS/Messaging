package com.android.messaging.ui.license.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.R
import com.android.messaging.ui.license.LicenseScreen
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.paneTitleMetadata

internal fun EntryProviderScope<NavKey>.licenseEntries() {
    entry<LicenseNavKey>(
        metadata = paneTitleMetadata(R.string.menu_license),
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
