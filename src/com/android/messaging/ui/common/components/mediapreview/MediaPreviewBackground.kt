package com.android.messaging.ui.common.components.mediapreview

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import com.android.messaging.ui.common.components.attachment.loadMediaThumbnailBitmap
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull

private const val MEDIA_PREVIEW_BACKGROUND_BITMAP_SIZE_PX = 40
private const val MEDIA_PREVIEW_BACKGROUND_OVERLAY_ALPHA = 0.5f
private const val MEDIA_PREVIEW_BACKGROUND_FALLBACK_ALPHA = 0.9f

@Composable
internal fun MediaPreviewBackground(
    items: ImmutableList<MediaPreviewItem>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val bitmapLoader: suspend (MediaPreviewItem) -> Bitmap? = remember(context) {
        { item ->
            loadMediaThumbnailBitmap(
                contentResolver = context.contentResolver,
                contentUri = item.contentUri.toUri(),
                contentType = item.contentType,
                size = IntSize(
                    width = MEDIA_PREVIEW_BACKGROUND_BITMAP_SIZE_PX,
                    height = MEDIA_PREVIEW_BACKGROUND_BITMAP_SIZE_PX,
                ),
                softenBitmap = true,
            )
        }
    }

    MediaPreviewBackground(
        modifier = modifier,
        items = items,
        pagerState = pagerState,
        bitmapLoader = bitmapLoader,
    )
}

@Composable
internal fun MediaPreviewBackground(
    items: ImmutableList<MediaPreviewItem>,
    pagerState: PagerState,
    bitmapLoader: suspend (MediaPreviewItem) -> Bitmap?,
    modifier: Modifier = Modifier,
) {
    val frames = rememberMediaPreviewBackgroundFrames(
        items = items,
        pagerState = pagerState,
        bitmapLoader = bitmapLoader,
    )
    val transitionState = remember { MediaPreviewBackgroundTransitionState() }

    MediaPreviewBackgroundTransitionEffects(
        items = items,
        pagerState = pagerState,
        frames = frames,
        transitionState = transitionState,
    )

    val renderState = resolveMediaPreviewBackgroundRenderState(
        isScrollInProgress = pagerState.isScrollInProgress,
        frames = frames,
        transitionState = transitionState,
    )

    MediaPreviewBackgroundContent(
        modifier = modifier,
        fallbackBackgroundColor = MaterialTheme
            .colorScheme
            .surfaceContainerHighest
            .copy(alpha = MEDIA_PREVIEW_BACKGROUND_FALLBACK_ALPHA),
        pagerState = pagerState,
        renderState = renderState,
    )
}

@Composable
private fun rememberMediaPreviewBackgroundFrames(
    items: ImmutableList<MediaPreviewItem>,
    pagerState: PagerState,
    bitmapLoader: suspend (MediaPreviewItem) -> Bitmap?,
): MediaPreviewBackgroundFrames {
    val blendPages by remember(pagerState, items.size) {
        derivedStateOf {
            resolveMediaPreviewBackgroundBlendPages(
                currentPage = pagerState.currentPage,
                currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
                pageCount = items.size,
            )
        }
    }
    val itemsToPrefetch = remember(
        items,
        pagerState.currentPage,
        pagerState.settledPage,
        pagerState.targetPage,
        blendPages,
    ) {
        getMediaPreviewBackgroundPrefetchItems(
            items = items,
            currentPage = pagerState.currentPage,
            settledPage = pagerState.settledPage,
            targetPage = pagerState.targetPage,
            blendPages = blendPages,
        )
    }
    val bitmapCache = rememberMediaPreviewBitmapCache(
        items = items,
        itemsToPrefetch = itemsToPrefetch,
        bitmapLoader = bitmapLoader,
    )
    val lowerFrame = mediaPreviewBackgroundFrame(
        items = items,
        page = blendPages.lowerPage,
        bitmapCache = bitmapCache,
    )
    val upperFrame = mediaPreviewBackgroundFrame(
        items = items,
        page = blendPages.upperPage,
        bitmapCache = bitmapCache,
    )

    return MediaPreviewBackgroundFrames(
        bitmapCache = bitmapCache,
        blendPages = blendPages,
        lowerFrame = lowerFrame,
        upperFrame = upperFrame,
        currentPageFrame = mediaPreviewBackgroundFrame(
            items = items,
            page = pagerState.currentPage,
            bitmapCache = bitmapCache,
        ),
        isInteractivePairReady = rememberMediaPreviewBackgroundInteractivePairReady(
            pagerState = pagerState,
            blendPages = blendPages,
            hasLowerFrame = lowerFrame != null,
            hasUpperFrame = upperFrame != null,
        ),
    )
}

@Composable
private fun rememberMediaPreviewBackgroundInteractivePairReady(
    pagerState: PagerState,
    blendPages: MediaPreviewBackgroundBlendPages,
    hasLowerFrame: Boolean,
    hasUpperFrame: Boolean,
): Boolean {
    val isInteractivePairReady by remember(
        pagerState,
        blendPages,
        hasLowerFrame,
        hasUpperFrame,
    ) {
        derivedStateOf {
            resolveMediaPreviewBackgroundInteractivePairReady(
                currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
                blendPages = blendPages,
                hasLowerFrame = hasLowerFrame,
                hasUpperFrame = hasUpperFrame,
            )
        }
    }

    return isInteractivePairReady
}

@Composable
private fun rememberMediaPreviewBitmapCache(
    items: ImmutableList<MediaPreviewItem>,
    itemsToPrefetch: ImmutableList<MediaPreviewItem>,
    bitmapLoader: suspend (MediaPreviewItem) -> Bitmap?,
): MediaPreviewBitmapCache {
    val bitmapCache = remember { MediaPreviewBitmapCache() }
    val bitmapPrefetcher = remember { MediaPreviewBitmapPrefetcher() }
    val currentBitmapLoader by rememberUpdatedState(newValue = bitmapLoader)

    LaunchedEffect(bitmapPrefetcher, bitmapCache) {
        bitmapPrefetcher.runWorkers(
            bitmapCache = bitmapCache,
            bitmapLoader = { item -> currentBitmapLoader(item) },
        )
    }
    LaunchedEffect(items, itemsToPrefetch, bitmapPrefetcher, bitmapCache) {
        bitmapPrefetcher.updateRequests(
            items = items,
            candidates = itemsToPrefetch,
            bitmapCache = bitmapCache,
        )
    }

    return bitmapCache
}

@Composable
private fun MediaPreviewBackgroundTransitionEffects(
    items: ImmutableList<MediaPreviewItem>,
    pagerState: PagerState,
    frames: MediaPreviewBackgroundFrames,
    transitionState: MediaPreviewBackgroundTransitionState,
) {
    MediaPreviewBackgroundScrollEffect(
        pagerState = pagerState,
        frames = frames,
        transitionState = transitionState,
    )
    MediaPreviewBackgroundSettledEffect(
        items = items,
        pagerState = pagerState,
        bitmapCache = frames.bitmapCache,
        transitionState = transitionState,
    )
}

@Composable
private fun MediaPreviewBackgroundScrollEffect(
    pagerState: PagerState,
    frames: MediaPreviewBackgroundFrames,
    transitionState: MediaPreviewBackgroundTransitionState,
) {
    val isScrollInProgress = pagerState.isScrollInProgress

    LaunchedEffect(
        isScrollInProgress,
        frames.isInteractivePairReady,
        pagerState.currentPage,
        frames.currentPageFrame,
    ) {
        if (isScrollInProgress) {
            transitionState.onScrollFrame(
                isInteractivePairReady = frames.isInteractivePairReady,
                currentPageFrame = frames.currentPageFrame,
            )
        }
    }
}

@Composable
private fun MediaPreviewBackgroundSettledEffect(
    items: ImmutableList<MediaPreviewItem>,
    pagerState: PagerState,
    bitmapCache: MediaPreviewBitmapCache,
    transitionState: MediaPreviewBackgroundTransitionState,
) {
    val currentItems by rememberUpdatedState(newValue = items)
    val isEmpty = items.isEmpty()

    LaunchedEffect(isEmpty, pagerState, bitmapCache, transitionState) {
        if (isEmpty) {
            transitionState.clear()
            return@LaunchedEffect
        }

        snapshotFlow {
            when {
                pagerState.isScrollInProgress -> null
                else -> mediaPreviewBackgroundFrame(
                    items = currentItems,
                    page = pagerState.settledPage,
                    bitmapCache = bitmapCache,
                )
            }
        }
            .filterNotNull()
            .conflate()
            .collect(transitionState::settle)
    }
}

@Composable
private fun MediaPreviewBackgroundContent(
    fallbackBackgroundColor: Color,
    pagerState: PagerState,
    renderState: MediaPreviewBackgroundRenderState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = fallbackBackgroundColor),
    ) {
        when (renderState) {
            MediaPreviewBackgroundRenderState.Fallback -> Unit
            is MediaPreviewBackgroundRenderState.Held -> {
                MediaPreviewBackgroundImage(frame = renderState.frame)
            }
            is MediaPreviewBackgroundRenderState.Interactive -> {
                MediaPreviewBackgroundInteractiveContent(
                    pagerState = pagerState,
                    renderState = renderState,
                )
            }
            is MediaPreviewBackgroundRenderState.Recovering -> {
                MediaPreviewBackgroundRecoveryContent(renderState = renderState)
            }
        }
        MediaPreviewBackgroundOverlay(renderState = renderState)
    }
}

@Composable
private fun MediaPreviewBackgroundInteractiveContent(
    pagerState: PagerState,
    renderState: MediaPreviewBackgroundRenderState.Interactive,
) {
    MediaPreviewBackgroundImage(frame = renderState.lowerFrame)
    renderState.upperFrame?.let { upperFrame ->
        MediaPreviewBackgroundImage(
            modifier = Modifier
                .graphicsLayer {
                    alpha = resolveMediaPreviewBackgroundUpperAlpha(
                        currentPage = pagerState.currentPage,
                        currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
                        lowerPage = renderState.lowerPage,
                        pageCount = pagerState.pageCount,
                    )
                },
            frame = upperFrame,
        )
    }
}

@Composable
private fun MediaPreviewBackgroundRecoveryContent(
    renderState: MediaPreviewBackgroundRenderState.Recovering,
) {
    renderState.displayedFrame?.let { displayedFrame ->
        MediaPreviewBackgroundImage(frame = displayedFrame)
    }
    MediaPreviewBackgroundImage(
        modifier = Modifier
            .graphicsLayer {
                alpha = renderState.recoveryProgress.value
            },
        frame = renderState.incomingFrame,
    )
}

@Composable
private fun MediaPreviewBackgroundOverlay(
    renderState: MediaPreviewBackgroundRenderState,
) {
    if (renderState == MediaPreviewBackgroundRenderState.Fallback) {
        return
    }

    val isRecoveringWithoutDisplayedFrame =
        renderState is MediaPreviewBackgroundRenderState.Recovering &&
            renderState.displayedFrame == null

    val overlayModifier = when {
        isRecoveringWithoutDisplayedFrame -> {
            Modifier
                .graphicsLayer {
                    alpha = renderState.recoveryProgress.value
                }
        }
        else -> Modifier
    }

    Box(
        modifier = overlayModifier
            .fillMaxSize()
            .background(
                color = Color.Black.copy(alpha = MEDIA_PREVIEW_BACKGROUND_OVERLAY_ALPHA),
            ),
    )
}

@Composable
private fun MediaPreviewBackgroundImage(
    frame: MediaPreviewBackgroundFrame,
    modifier: Modifier = Modifier,
) {
    Image(
        modifier = modifier.fillMaxSize(),
        bitmap = frame.imageBitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.Low,
    )
}

@PreviewLightDark
@Composable
private fun MediaPreviewBackgroundPreview() {
    val items = persistentListOf(
        MediaPreviewItem(
            contentUri = "content://com.android.messaging.preview/image.jpg",
            contentType = "image/jpeg",
            isVideo = false,
        ),
        MediaPreviewItem(
            contentUri = "content://com.android.messaging.preview/video.mp4",
            contentType = "video/mp4",
            isVideo = true,
        ),
    )

    MessagingPreviewTheme {
        MediaPreviewBackground(
            modifier = Modifier.fillMaxSize(),
            items = items,
            pagerState = rememberPagerState(pageCount = { items.size }),
        )
    }
}
