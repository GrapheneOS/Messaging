package com.android.messaging.ui.photoviewer.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.R
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.overlayMetadata
import com.android.messaging.ui.navigation.paneTitleMetadata
import com.android.messaging.ui.photoviewer.screen.PhotoViewerScreen
import com.android.messaging.ui.photoviewer.screen.PhotoViewerViewModel

internal fun EntryProviderScope<NavKey>.photoViewerEntries() {
    entry<PhotoViewerNavKey>(
        metadata = overlayMetadata() +
            paneTitleMetadata(R.string.photo_view_activity_title),
        content = photoViewerRouteContent(),
    )
}

private fun photoViewerRouteContent(): @Composable (PhotoViewerNavKey) -> Unit {
    return { navKey ->
        val navigator = LocalNavigator.current

        PhotoViewerScreen(
            screenModel = hiltViewModel<PhotoViewerViewModel>(),
            launchRequest = navKey.launchRequest,
            onFinish = navigator::back,
        )
    }
}
