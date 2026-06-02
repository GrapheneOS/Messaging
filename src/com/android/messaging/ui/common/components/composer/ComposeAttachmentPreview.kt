package com.android.messaging.ui.common.components.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList

internal const val COMPOSE_ATTACHMENT_PREVIEW_LIST_TEST_TAG = "compose_attachment_preview_list"

internal fun composeAttachmentPreviewItemTestTag(id: String): String {
    return "compose_attachment_preview_item_$id"
}

internal fun composeAttachmentPreviewRemoveButtonTestTag(id: String): String {
    return "compose_attachment_preview_remove_button_$id"
}

private val AttachmentPreviewCardSize = 90.dp
private val RemoveButtonSize = 28.dp
private val RemoveButtonPadding = 6.dp
private val AttachmentIconSize = 28.dp

@Immutable
internal enum class ComposeAttachmentType {
    Image,
    Video,
    Audio,
    File,
}

@Immutable
internal data class ComposeAttachmentPreviewItem(
    val id: String,
    val uri: String,
    val type: ComposeAttachmentType,
)

@Composable
internal fun ComposeAttachmentPreview(
    attachments: ImmutableList<ComposeAttachmentPreviewItem>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AttachmentPreviewRow(
        attachments = attachments,
        key = { attachment -> attachment.id },
        modifier = modifier.testTag(COMPOSE_ATTACHMENT_PREVIEW_LIST_TEST_TAG),
    ) { attachment ->
        ComposeAttachmentPreviewCard(
            attachment = attachment,
            onRemove = {
                onRemove(attachment.id)
            },
        )
    }
}

@Composable
private fun ComposeAttachmentPreviewCard(
    attachment: ComposeAttachmentPreviewItem,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(size = AttachmentPreviewCardSize)
            .clip(shape = MaterialTheme.shapes.medium)
            .testTag(composeAttachmentPreviewItemTestTag(id = attachment.id)),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box {
            when (attachment.type) {
                ComposeAttachmentType.Image,
                ComposeAttachmentType.Video,
                -> {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = attachment.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )

                    if (attachment.type == ComposeAttachmentType.Video) {
                        ComposeAttachmentIconBadge(imageVector = Icons.Rounded.PlayArrow)
                    }
                }

                ComposeAttachmentType.Audio -> {
                    ComposeAttachmentIconBadge(imageVector = Icons.Rounded.GraphicEq)
                }

                ComposeAttachmentType.File -> {
                    ComposeAttachmentIconBadge(imageVector = Icons.Rounded.Description)
                }
            }

            AttachmentPreviewRemoveButton(
                onClick = onRemove,
                size = RemoveButtonSize,
                modifier = Modifier
                    .align(alignment = Alignment.TopEnd)
                    .padding(all = RemoveButtonPadding)
                    .testTag(composeAttachmentPreviewRemoveButtonTestTag(id = attachment.id)),
            )
        }
    }
}

@Composable
private fun BoxScope.ComposeAttachmentIconBadge(
    imageVector: ImageVector,
) {
    Icon(
        modifier = Modifier
            .align(alignment = Alignment.Center)
            .size(size = AttachmentIconSize),
        imageVector = imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
