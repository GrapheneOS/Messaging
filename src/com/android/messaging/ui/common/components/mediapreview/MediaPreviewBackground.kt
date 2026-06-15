package com.android.messaging.ui.common.components.mediapreview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import com.android.messaging.ui.common.components.attachment.loadMediaThumbnailBitmap
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val MEDIA_PREVIEW_BACKGROUND_BITMAP_SIZE_PX = 40
private const val MEDIA_PREVIEW_BACKGROUND_OVERLAY_ALPHA = 0.5f
private const val MEDIA_PREVIEW_BACKGROUND_FALLBACK_ALPHA = 0.9f

@Composable
internal fun MediaPreviewBackground(
    items: ImmutableList<MediaPreviewItem>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val backgroundState = rememberMediaPreviewBackgroundState(
        items = items,
        settledPage = pagerState.settledPage,
    )

    MediaPreviewBackgroundContent(
        modifier = modifier,
        state = backgroundState,
    )
}

@Composable
private fun MediaPreviewBackgroundContent(
    modifier: Modifier = Modifier,
    state: MediaPreviewBackgroundState,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = state.fallbackBackgroundColor,
            ),
    ) {
        if (state.settledBackgroundImageBitmap != null) {
            Image(
                bitmap = state.settledBackgroundImageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.Black.copy(alpha = MEDIA_PREVIEW_BACKGROUND_OVERLAY_ALPHA),
                    ),
            )
        }
    }
}

@Composable
private fun rememberMediaPreviewBackgroundState(
    items: ImmutableList<MediaPreviewItem>,
    settledPage: Int,
): MediaPreviewBackgroundState {
    val backgroundSelection = remember(
        items,
        settledPage,
    ) {
        getMediaPreviewBackgroundSelection(
            items = items,
            settledPage = settledPage,
        )
    }

    val backgroundBitmapCache = rememberMediaPreviewBitmapCache(
        items = items,
        itemsToPrefetch = backgroundSelection.itemsToPrefetch,
    )

    val fallbackBackgroundColor = MaterialTheme
        .colorScheme
        .surfaceContainerHighest
        .copy(alpha = MEDIA_PREVIEW_BACKGROUND_FALLBACK_ALPHA)

    val settledBackgroundBitmap = backgroundSelection
        .itemsToPrefetch
        .firstOrNull()
        ?.let { item ->
            backgroundBitmapCache[item.contentUri]
        }

    val settledBackgroundImageBitmap = settledBackgroundBitmap?.asImageBitmap()

    return MediaPreviewBackgroundState(
        settledBackgroundImageBitmap = settledBackgroundImageBitmap,
        fallbackBackgroundColor = fallbackBackgroundColor,
    )
}

@Composable
private fun rememberMediaPreviewBitmapCache(
    items: ImmutableList<MediaPreviewItem>,
    itemsToPrefetch: ImmutableList<MediaPreviewItem>,
): MediaPreviewBitmapCache {
    val context = LocalContext.current

    val backgroundBitmapCache = remember {
        MediaPreviewBitmapCache()
    }

    LaunchedEffect(items) {
        backgroundBitmapCache.removeInactive(
            activeContentUris = items
                .asSequence()
                .map { it.contentUri }
                .toSet(),
        )
    }

    LaunchedEffect(itemsToPrefetch) {
        itemsToPrefetch
            .asSequence()
            .filter { backgroundBitmapCache[it.contentUri] == null }
            .forEach { item ->
                loadMediaThumbnailBitmap(
                    contentResolver = context.contentResolver,
                    contentUri = item.contentUri.toUri(),
                    contentType = item.contentType,
                    size = IntSize(
                        width = MEDIA_PREVIEW_BACKGROUND_BITMAP_SIZE_PX,
                        height = MEDIA_PREVIEW_BACKGROUND_BITMAP_SIZE_PX,
                    ),
                    softenBitmap = true,
                )?.let { bitmap ->
                    backgroundBitmapCache.put(
                        contentUri = item.contentUri,
                        bitmap = bitmap,
                    )
                }
            }
    }

    return backgroundBitmapCache
}

private fun getMediaPreviewBackgroundSelection(
    items: ImmutableList<MediaPreviewItem>,
    settledPage: Int,
): MediaPreviewBackgroundSelection {
    if (items.isEmpty()) {
        return MediaPreviewBackgroundSelection(
            itemsToPrefetch = persistentListOf(),
        )
    }

    val settledIndex = settledPage.coerceIn(
        minimumValue = 0,
        maximumValue = items.lastIndex,
    )

    val settledItem = items[settledIndex]

    val previousItem = items
        .getOrNull(index = settledIndex - 1)
        ?.takeIf { it.contentUri != settledItem.contentUri }

    val nextItem = items
        .getOrNull(index = settledIndex + 1)
        ?.takeIf { item ->
            item.contentUri != settledItem.contentUri &&
                item.contentUri != previousItem?.contentUri
        }

    val itemsToPrefetch = listOfNotNull(
        settledItem,
        previousItem,
        nextItem,
    ).toImmutableList()

    return MediaPreviewBackgroundSelection(
        itemsToPrefetch = itemsToPrefetch,
    )
}

@Immutable
private data class MediaPreviewBackgroundSelection(
    val itemsToPrefetch: ImmutableList<MediaPreviewItem>,
)

@Immutable
private data class MediaPreviewBackgroundState(
    val settledBackgroundImageBitmap: ImageBitmap?,
    val fallbackBackgroundColor: Color,
)

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
