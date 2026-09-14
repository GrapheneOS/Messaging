package com.android.messaging.ui.photoviewer.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_ZOOMABLE_PHOTO_TEST_TAG
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode

@Composable
internal fun PhotoViewerPage(
    modifier: Modifier,
    item: PhotoViewerItem,
    page: Int,
    pagerState: PagerState,
    displayMode: PhotoViewerDisplayMode,
    imageDecodeSize: IntSize,
    onToggleDisplayMode: () -> Unit,
    onEnterImmersiveMode: () -> Unit,
    onCloseClick: () -> Unit,
    dismissDragState: PhotoViewerDismissDragState,
    onZoomChanged: (Boolean) -> Unit,
) {
    ZoomablePhoto(
        modifier = modifier,
        item = item,
        page = page,
        pagerState = pagerState,
        displayMode = displayMode,
        imageDecodeSize = imageDecodeSize,
        onToggleDisplayMode = onToggleDisplayMode,
        onEnterImmersiveMode = onEnterImmersiveMode,
        onCloseClick = onCloseClick,
        dismissDragState = dismissDragState,
        onZoomChanged = onZoomChanged,
    )
}

@Composable
private fun ZoomablePhoto(
    modifier: Modifier,
    item: PhotoViewerItem,
    page: Int,
    pagerState: PagerState,
    displayMode: PhotoViewerDisplayMode,
    imageDecodeSize: IntSize,
    onToggleDisplayMode: () -> Unit,
    onEnterImmersiveMode: () -> Unit,
    onCloseClick: () -> Unit,
    dismissDragState: PhotoViewerDismissDragState,
    onZoomChanged: (Boolean) -> Unit,
) {
    val contentKey = PhotoViewerContentKey(
        page = page,
        contentUri = item.contentUri.toString(),
    )
    var isLoading by remember(contentKey) { mutableStateOf(value = true) }
    var isFailed by remember(contentKey) { mutableStateOf(value = false) }
    var imageSize by remember(contentKey) { mutableStateOf<IntSize?>(value = null) }
    val isCurrentPage = page == pagerState.currentPage

    Box(
        modifier = modifier,
    ) {
        ZoomablePhotoContainer(
            modifier = Modifier.photoViewerZoomableModifier(isCurrentPage = isCurrentPage),
            contentKey = contentKey,
            displayMode = displayMode,
            onToggleDisplayMode = onToggleDisplayMode,
            onEnterImmersiveMode = onEnterImmersiveMode,
            onCloseClick = onCloseClick,
            dismissDragState = dismissDragState.takeIf { isCurrentPage },
            onZoomChanged = onZoomChanged,
        ) {
            PhotoViewerImage(
                item = item,
                page = page,
                pagerState = pagerState,
                displayMode = displayMode,
                imageSize = imageSize,
                imageDecodeSize = imageDecodeSize,
                onImageLoading = {
                    isLoading = true
                    isFailed = false
                },
                onImageError = {
                    isLoading = false
                    isFailed = true
                },
                onImageLoaded = { loadedImageSize ->
                    imageSize = loadedImageSize
                    isLoading = false
                    isFailed = false
                },
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(alignment = Alignment.Center),
            )
        }

        if (isFailed) {
            PhotoViewerLoadError(
                modifier = Modifier
                    .align(alignment = Alignment.Center),
            )
        }
    }
}

private fun Modifier.photoViewerZoomableModifier(isCurrentPage: Boolean): Modifier {
    return this
        .fillMaxSize()
        .then(
            when {
                isCurrentPage -> {
                    Modifier.testTag(
                        tag = PHOTO_VIEWER_ZOOMABLE_PHOTO_TEST_TAG,
                    )
                }

                else -> Modifier
            },
        )
}
