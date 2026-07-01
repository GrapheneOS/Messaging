package com.android.messaging.ui.photoviewer.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.ui.common.components.mediapreview.MediaPreviewItem
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_PAGER_TEST_TAG
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val PHOTO_VIEWER_CAROUSEL_PAGE_WIDTH_FRACTION = 0.9f
private const val PHOTO_VIEWER_PAGER_LAYOUT_ANIMATION_MILLIS = 250
private val PhotoViewerCarouselPageSpacing = 12.dp

@Composable
internal fun rememberPhotoViewerPagerState(
    items: ImmutableList<PhotoViewerItem>,
    currentPage: Int,
    onPageSettled: (Int) -> Unit,
): PagerState {
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(
            minimumValue = 0,
            maximumValue = items.lastIndex,
        ),
        pageCount = { items.size },
    )

    LaunchedEffect(items.size, currentPage) {
        val targetPage = currentPage.coerceIn(
            minimumValue = 0,
            maximumValue = items.lastIndex,
        )

        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(page = targetPage)
        }
    }

    LaunchedEffect(pagerState.settledPage, items.size) {
        if (items.isNotEmpty()) {
            onPageSettled(pagerState.settledPage)
        }
    }

    return pagerState
}

@Composable
internal fun rememberPhotoViewerPreviewItems(
    items: ImmutableList<PhotoViewerItem>,
): ImmutableList<MediaPreviewItem> {
    return remember(items) {
        items
            .map { item ->
                MediaPreviewItem(
                    contentUri = item.contentUri.toString(),
                    contentType = item.contentType,
                    isVideo = false,
                )
            }
            .toImmutableList()
    }
}

@Composable
internal fun PhotoViewerPager(
    modifier: Modifier,
    items: ImmutableList<PhotoViewerItem>,
    pagerState: PagerState,
    displayMode: PhotoViewerDisplayMode,
    isClosing: Boolean,
    onToggleDisplayMode: () -> Unit,
    onEnterImmersiveMode: () -> Unit,
    dismissDragState: PhotoViewerDismissDragState,
    onCloseClick: () -> Unit,
) {
    var isCurrentPageZoomed by remember(items) { mutableStateOf(value = false) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
        dismissDragState.reset()
    }

    BoxWithConstraints(modifier = modifier) {
        val imageDecodeSize = rememberPhotoViewerImageDecodeSize(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
        )
        val pagerLayout = rememberPhotoViewerPagerLayout(
            maxWidth = maxWidth,
            displayMode = displayMode,
        )

        HorizontalPager(
            modifier = Modifier
                .fillMaxSize()
                .testTag(tag = PHOTO_VIEWER_PAGER_TEST_TAG),
            state = pagerState,
            beyondViewportPageCount = 1,
            contentPadding = PaddingValues(horizontal = pagerLayout.horizontalInset),
            pageSize = PageSize.Fixed(pageSize = pagerLayout.pageWidth),
            pageSpacing = pagerLayout.pageSpacing,
            key = { page ->
                photoViewerPagerItemKey(
                    index = page,
                    item = items[page],
                )
            },
            userScrollEnabled = !isCurrentPageZoomed && !isClosing,
        ) { page ->
            PhotoViewerPage(
                modifier = Modifier.fillMaxSize(),
                item = items[page],
                page = page,
                pagerState = pagerState,
                displayMode = displayMode,
                imageDecodeSize = imageDecodeSize,
                onToggleDisplayMode = onToggleDisplayMode,
                onEnterImmersiveMode = onEnterImmersiveMode,
                onCloseClick = onCloseClick,
                dismissDragState = dismissDragState,
                onZoomChanged = { isZoomed ->
                    if (page == pagerState.currentPage) {
                        isCurrentPageZoomed = isZoomed
                    }
                },
            )
        }
    }
}

@Composable
private fun rememberPhotoViewerImageDecodeSize(maxWidth: Dp, maxHeight: Dp): IntSize {
    val density = LocalDensity.current
    return remember(maxWidth, maxHeight, density) {
        with(density) {
            resolvePhotoViewerDecodeSize(
                displayedImageSize = IntSize(
                    width = maxWidth.toPx().roundToInt(),
                    height = maxHeight.toPx().roundToInt(),
                ),
            )
        }
    }
}

@Composable
private fun rememberPhotoViewerPagerLayout(
    maxWidth: Dp,
    displayMode: PhotoViewerDisplayMode,
): PhotoViewerPagerLayout {
    val targetPageWidth = when (displayMode) {
        PhotoViewerDisplayMode.Carousel -> maxWidth * PHOTO_VIEWER_CAROUSEL_PAGE_WIDTH_FRACTION
        PhotoViewerDisplayMode.Immersive -> maxWidth
    }

    val targetPageSpacing = when (displayMode) {
        PhotoViewerDisplayMode.Carousel -> PhotoViewerCarouselPageSpacing
        PhotoViewerDisplayMode.Immersive -> 0.dp
    }

    val pageWidth by animateDpAsState(
        targetValue = targetPageWidth,
        animationSpec = tween(durationMillis = PHOTO_VIEWER_PAGER_LAYOUT_ANIMATION_MILLIS),
        label = "photoViewerPageWidth",
    )

    val pageSpacing by animateDpAsState(
        targetValue = targetPageSpacing,
        animationSpec = tween(durationMillis = PHOTO_VIEWER_PAGER_LAYOUT_ANIMATION_MILLIS),
        label = "photoViewerPageSpacing",
    )

    return PhotoViewerPagerLayout(
        pageWidth = pageWidth,
        pageSpacing = pageSpacing,
        horizontalInset = (maxWidth - pageWidth) / 2,
    )
}

private fun photoViewerPagerItemKey(
    index: Int,
    item: PhotoViewerItem,
): String {
    return "$index|${item.contentUri}"
}

private data class PhotoViewerPagerLayout(
    val pageWidth: Dp,
    val pageSpacing: Dp,
    val horizontalInset: Dp,
)
