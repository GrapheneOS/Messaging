package com.android.messaging.ui.common.components.mediapreview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.common.components.attachment.MediaThumbnail
import com.android.messaging.ui.core.MessagingPreviewColumn

private val MediaPreviewCardElevation = 6.dp
private val MediaPreviewCardShadowElevation = 20.dp
private const val MEDIA_PREVIEW_CARD_SURFACE_ALPHA = 0.25f
private const val MEDIA_PREVIEW_VIDEO_BADGE_BACKGROUND_ALPHA = 0.5f
private val MediaPreviewVideoBadgePadding = 12.dp

@Composable
internal fun MediaPreviewCard(
    contentUri: String,
    contentType: String,
    isVideo: Boolean,
    previewSize: IntSize,
    modifier: Modifier = Modifier,
) {
    val previewShape = MaterialTheme.shapes.large

    Surface(
        modifier = modifier
            .clip(previewShape),
        shape = previewShape,
        color = MaterialTheme
            .colorScheme
            .surfaceColorAtElevation(elevation = MediaPreviewCardElevation)
            .copy(alpha = MEDIA_PREVIEW_CARD_SURFACE_ALPHA),
        shadowElevation = MediaPreviewCardShadowElevation,
        tonalElevation = MediaPreviewCardElevation,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            MediaThumbnail(
                modifier = Modifier.fillMaxSize(),
                contentUri = contentUri,
                contentType = contentType,
                size = previewSize,
                contentScale = ContentScale.Crop,
                backgroundColor = Color.Transparent,
            )

            if (isVideo) {
                MediaPreviewVideoBadge()
            }
        }
    }
}

@Composable
private fun MediaPreviewVideoBadge(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = MEDIA_PREVIEW_VIDEO_BADGE_BACKGROUND_ALPHA),
    ) {
        Icon(
            modifier = Modifier.padding(all = MediaPreviewVideoBadgePadding),
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
        )
    }
}

@PreviewLightDark
@Composable
private fun MediaPreviewCardPreview() {
    MessagingPreviewColumn {
        MediaPreviewCard(
            contentUri = "content://com.android.messaging.preview/video.mp4",
            contentType = "video/mp4",
            isVideo = true,
            previewSize = IntSize(
                width = 384,
                height = 480,
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
