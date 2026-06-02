package com.android.messaging.ui.common.components.attachment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private val AttachmentPreviewCardHeight = 88.dp
private val AttachmentPreviewCardWidth = 220.dp
private val MediaAttachmentCardSize = 90.dp
private val AudioRemoveButtonMargin = 4.dp
private val AudioRemoveButtonSize = 24.dp
private val ThumbnailRemoveButtonSize = 28.dp
private val ThumbnailRemoveButtonPadding = 6.dp
private const val ATTACHMENT_PREVIEW_SIZE_PX = 256

@Composable
internal fun MediaAttachmentCell(
    contentUri: String,
    contentType: String,
    isVideo: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    removeButtonTestTag: String,
    modifier: Modifier = Modifier,
) {
    AttachmentPreviewCard(
        modifier = modifier.size(size = MediaAttachmentCardSize),
        onClick = onClick,
    ) {
        MediaThumbnail(
            modifier = Modifier.fillMaxSize(),
            contentUri = contentUri,
            contentType = contentType,
            size = IntSize(
                width = ATTACHMENT_PREVIEW_SIZE_PX,
                height = ATTACHMENT_PREVIEW_SIZE_PX,
            ),
        )

        if (isVideo) {
            VideoAttachmentOverlay()
        }

        ThumbnailRemoveButton(
            onRemove = onRemove,
            testTag = removeButtonTestTag,
        )
    }
}

@Composable
internal fun AudioAttachmentCell(
    durationMillis: Long,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    removeButtonTestTag: String,
    modifier: Modifier = Modifier,
) {
    AttachmentPreviewCard(
        modifier = modifier
            .height(height = AttachmentPreviewCardHeight)
            .wrapContentWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .height(height = AttachmentPreviewCardHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier
                    .size(size = 40.dp)
                    .clip(shape = CircleShape)
                    .background(color = MaterialTheme.colorScheme.primary)
                    .padding(all = 8.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )

            Text(
                modifier = Modifier.wrapContentWidth(),
                text = formatAudioDuration(durationMillis = durationMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

            Box(
                modifier = Modifier
                    .width(width = AudioRemoveButtonSize + AudioRemoveButtonMargin)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                AttachmentPreviewRemoveButton(
                    onClick = onRemove,
                    size = AudioRemoveButtonSize,
                    modifier = Modifier.testTag(removeButtonTestTag),
                )
            }
        }
    }
}

@Composable
internal fun VCardAttachmentCell(
    kind: VCardAttachmentKind,
    avatarUri: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    removeButtonTestTag: String,
    modifier: Modifier = Modifier,
) {
    AttachmentPreviewCard(
        modifier = modifier.size(
            width = AttachmentPreviewCardWidth,
            height = AttachmentPreviewCardHeight,
        ),
        onClick = onClick,
    ) {
        VCardAttachmentCard(
            modifier = Modifier
                .fillMaxWidth()
                .align(alignment = Alignment.CenterStart)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            kind = kind,
            avatarUri = avatarUri,
            title = title,
            subtitle = subtitle,
        )

        ThumbnailRemoveButton(
            onRemove = onRemove,
            testTag = removeButtonTestTag,
        )
    }
}

@Composable
internal fun AttachmentPreviewCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(content = content)
    }
}

@Composable
private fun BoxScope.ThumbnailRemoveButton(
    onRemove: () -> Unit,
    testTag: String,
) {
    AttachmentPreviewRemoveButton(
        onClick = onRemove,
        size = ThumbnailRemoveButtonSize,
        modifier = Modifier
            .align(alignment = Alignment.TopEnd)
            .padding(all = ThumbnailRemoveButtonPadding)
            .testTag(testTag),
    )
}

@Composable
private fun BoxScope.VideoAttachmentOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(size = 28.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
