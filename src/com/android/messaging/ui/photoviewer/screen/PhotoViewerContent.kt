package com.android.messaging.ui.photoviewer.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.ui.common.components.PagerIndicator
import com.android.messaging.ui.common.components.mediapreview.MediaPreviewBackground
import com.android.messaging.ui.common.components.mediapreview.MediaPreviewItem
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_PAGE_INDICATOR_TEST_TAG
import com.android.messaging.ui.photoviewer.component.PhotoViewerDismissDragState
import com.android.messaging.ui.photoviewer.component.PhotoViewerEmptyState
import com.android.messaging.ui.photoviewer.component.PhotoViewerLoadError
import com.android.messaging.ui.photoviewer.component.PhotoViewerPager
import com.android.messaging.ui.photoviewer.component.PhotoViewerTopBar
import com.android.messaging.ui.photoviewer.component.rememberPhotoViewerPagerState
import com.android.messaging.ui.photoviewer.component.rememberPhotoViewerPreviewItems
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerLoadState
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerUiState
import kotlinx.collections.immutable.ImmutableList

private const val PHOTO_VIEWER_CLOSE_ANIMATION_MILLIS = 250
private const val PHOTO_VIEWER_ENTER_ANIMATION_MILLIS = 350
private val PhotoViewerPageIndicatorBottomPadding = 24.dp

@Composable
internal fun PhotoViewerAnimatedContent(
    launchRequest: PhotoViewerLaunchRequest,
    isClosing: Boolean,
    onCloseAnimationFinished: () -> Unit,
    content: @Composable () -> Unit,
) {
    var rootSize by remember { mutableStateOf(value = IntSize.Zero) }
    var hasEntered by remember { mutableStateOf(value = false) }
    var hasClosed by remember { mutableStateOf(value = false) }
    val transitionProgress = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(rootSize, launchRequest.sourceBounds) {
        if (rootSize != IntSize.Zero && !hasEntered) {
            transitionProgress.snapTo(targetValue = 0f)
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = PHOTO_VIEWER_ENTER_ANIMATION_MILLIS),
            )
            hasEntered = true
        }
    }

    LaunchedEffect(isClosing, rootSize) {
        if (isClosing && !hasClosed) {
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = PHOTO_VIEWER_CLOSE_ANIMATION_MILLIS),
            )
            hasClosed = true
            onCloseAnimationFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> rootSize = size }
            .graphicsLayer {
                val transform = resolvePhotoViewerTransform(
                    sourceBounds = launchRequest.sourceBounds,
                    rootSize = rootSize,
                    progress = transitionProgress.value,
                )

                alpha = transform.alpha
                scaleX = transform.scale
                scaleY = transform.scale
                translationX = transform.translationX
                translationY = transform.translationY
                transformOrigin = TransformOrigin.Center
            },
    ) {
        content()
    }
}

@Composable
internal fun PhotoViewerContent(
    uiState: PhotoViewerUiState,
    currentItem: PhotoViewerItem?,
    actionsEnabled: Boolean,
    onPageSettled: (Int) -> Unit,
    onToggleDisplayMode: () -> Unit,
    onEnterImmersiveMode: () -> Unit,
    dismissDragState: PhotoViewerDismissDragState,
    onMetadataClick: () -> Unit,
    onCloseClick: () -> Unit,
    onForwardClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val shouldShowTopBar by remember(dismissDragState) {
        derivedStateOf { dismissDragState.shouldShowTopBar }
    }
    val shouldShowChrome = uiState.displayMode == PhotoViewerDisplayMode.Carousel &&
        currentItem != null &&
        !uiState.isClosing &&
        shouldShowTopBar
    val shouldShowPageIndicator = shouldShowChrome &&
        uiState.loadState == PhotoViewerLoadState.Loaded &&
        uiState.items.size > 1

    Box(modifier = Modifier.fillMaxSize()) {
        PhotoViewerMediaContent(
            uiState = uiState,
            isPageIndicatorVisible = shouldShowPageIndicator,
            onPageSettled = onPageSettled,
            onToggleDisplayMode = onToggleDisplayMode,
            onEnterImmersiveMode = onEnterImmersiveMode,
            dismissDragState = dismissDragState,
            onCloseClick = onCloseClick,
        )

        when (uiState.loadState) {
            PhotoViewerLoadState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(alignment = Alignment.Center),
                )
            }

            PhotoViewerLoadState.Empty -> {
                PhotoViewerEmptyState(
                    modifier = Modifier
                        .align(alignment = Alignment.Center),
                )
            }

            PhotoViewerLoadState.Error -> {
                PhotoViewerLoadError(
                    modifier = Modifier
                        .align(alignment = Alignment.Center),
                )
            }

            else -> Unit
        }

        PhotoViewerTopBar(
            isVisible = shouldShowChrome,
            item = currentItem,
            actionsEnabled = actionsEnabled,
            onMetadataClick = onMetadataClick,
            onCloseClick = onCloseClick,
            onForwardClick = onForwardClick,
            onSaveClick = onSaveClick,
            onShareClick = onShareClick,
        )
    }
}

@Composable
private fun PhotoViewerMediaContent(
    uiState: PhotoViewerUiState,
    isPageIndicatorVisible: Boolean,
    onPageSettled: (Int) -> Unit,
    onToggleDisplayMode: () -> Unit,
    onEnterImmersiveMode: () -> Unit,
    dismissDragState: PhotoViewerDismissDragState,
    onCloseClick: () -> Unit,
) {
    if (uiState.items.isEmpty()) {
        return
    }

    val pagerState = rememberPhotoViewerPagerState(
        items = uiState.items,
        currentPage = uiState.currentPage,
        onPageSettled = onPageSettled,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        PhotoViewerBlurredBackground(
            dismissDragState = dismissDragState,
            items = rememberPhotoViewerPreviewItems(items = uiState.items),
            pagerState = pagerState,
        )

        PhotoViewerPager(
            modifier = Modifier.fillMaxSize(),
            items = uiState.items,
            pagerState = pagerState,
            displayMode = uiState.displayMode,
            isClosing = uiState.isClosing,
            onToggleDisplayMode = onToggleDisplayMode,
            onEnterImmersiveMode = onEnterImmersiveMode,
            dismissDragState = dismissDragState,
            onCloseClick = onCloseClick,
        )

        if (isPageIndicatorVisible) {
            PagerIndicator(
                modifier = Modifier
                    .align(alignment = Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = PhotoViewerPageIndicatorBottomPadding)
                    .testTag(tag = PHOTO_VIEWER_PAGE_INDICATOR_TEST_TAG),
                pagerState = pagerState,
                pageCount = uiState.items.size,
            )
        }
    }
}

@Composable
private fun PhotoViewerBlurredBackground(
    dismissDragState: PhotoViewerDismissDragState,
    items: ImmutableList<MediaPreviewItem>,
    pagerState: PagerState,
) {
    MediaPreviewBackground(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = dismissDragState.backgroundAlpha
            },
        items = items,
        pagerState = pagerState,
    )
}
