package com.android.messaging.ui.vcarddetail.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.R
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.SeededViewModelStoreOwner
import com.android.messaging.ui.navigation.paneTitleMetadata
import com.android.messaging.ui.vcarddetail.screen.VCardDetailScreen
import com.android.messaging.ui.vcarddetail.screen.rememberVCardDetailEffectHandler

internal fun EntryProviderScope<NavKey>.vCardDetailEntries() {
    entry<VCardDetailNavKey>(
        metadata = paneTitleMetadata(R.string.vcard_detail_activity_title),
        content = vCardDetailRouteContent(),
    )
}

private fun vCardDetailRouteContent(): @Composable (VCardDetailNavKey) -> Unit {
    return { navKey ->
        val navigator = LocalNavigator.current
        val defaultArgs = remember(navKey) {
            vCardDetailDefaultArgs(navKey = navKey)
        }

        SeededViewModelStoreOwner(defaultArgs = defaultArgs) {
            VCardDetailScreen(
                effectHandler = rememberVCardDetailEffectHandler(),
                onNavigateBack = navigator::back,
            )
        }
    }
}
