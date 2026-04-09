package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.v2.mediapicker.component.ConversationMediaThumbnail
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.util.ContentType
import kotlinx.collections.immutable.ImmutableList

internal val MESSAGE_ATTACHMENT_CORNER_RADIUS = 18.dp
internal val MESSAGE_ATTACHMENT_GRID_SPACING = 6.dp
private const val MESSAGE_ATTACHMENT_DEFAULT_IMAGE_ASPECT_RATIO = 4f / 3f
private const val MESSAGE_ATTACHMENT_DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f
private const val MESSAGE_ATTACHMENT_MAX_ASPECT_RATIO = 1.8f
private const val MESSAGE_ATTACHMENT_MIN_ASPECT_RATIO = 0.75f
private const val MESSAGE_ATTACHMENT_MIN_PREVIEW_SIZE_PX = 1

@Composable
internal fun ConversationGalleryVisualAttachments(
    attachments: ImmutableList<ConversationMessageAttachment>,
    hasTextAboveVisualAttachments: Boolean,
    hasTextBelowVisualAttachments: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
) {
    when (attachments.size) {
        0 -> {}
        1 -> {
            ConversationVisualAttachmentCard(
                modifier = Modifier.fillMaxWidth(),
                attachment = attachments.first(),
                aspectRatio = resolveAttachmentAspectRatio(
                    attachment = attachments.first(),
                ),
                attachmentShape = visualAttachmentShape(
                    hasTextAboveVisualAttachments = hasTextAboveVisualAttachments,
                    hasTextBelowVisualAttachments = hasTextBelowVisualAttachments,
                ),
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
            )
        }

        else -> {
            ConversationVisualAttachmentGrid(
                attachments = attachments,
                hasTextAboveVisualAttachments = hasTextAboveVisualAttachments,
                hasTextBelowVisualAttachments = hasTextBelowVisualAttachments,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
            )
        }
    }
}

@Composable
internal fun ConversationStandaloneVisualAttachment(
    attachment: ConversationMessageAttachment,
    hasTextAboveVisualAttachments: Boolean,
    hasTextBelowVisualAttachments: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
) {
    ConversationVisualAttachmentCard(
        modifier = Modifier.fillMaxWidth(),
        attachment = attachment,
        aspectRatio = resolveAttachmentAspectRatio(
            attachment = attachment,
        ),
        attachmentShape = visualAttachmentShape(
            hasTextAboveVisualAttachments = hasTextAboveVisualAttachments,
            hasTextBelowVisualAttachments = hasTextBelowVisualAttachments,
        ),
        onAttachmentClick = onAttachmentClick,
        onExternalUriClick = onExternalUriClick,
    )
}

@Composable
private fun ConversationVisualAttachmentGrid(
    attachments: ImmutableList<ConversationMessageAttachment>,
    hasTextAboveVisualAttachments: Boolean,
    hasTextBelowVisualAttachments: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
) {
    val attachmentRows = remember(attachments) {
        attachments.chunked(size = 2)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(space = MESSAGE_ATTACHMENT_GRID_SPACING),
    ) {
        attachmentRows.forEachIndexed { rowIndex, attachmentRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = MESSAGE_ATTACHMENT_GRID_SPACING,
                ),
            ) {
                attachmentRow.forEachIndexed { columnIndex, attachment ->
                    Box(
                        modifier = Modifier.weight(weight = 1f),
                    ) {
                        ConversationVisualAttachmentCard(
                            modifier = Modifier.fillMaxWidth(),
                            attachment = attachment,
                            aspectRatio = 1f,
                            attachmentShape = visualAttachmentShape(
                                hasTextAboveVisualAttachments = hasTextAboveVisualAttachments &&
                                    rowIndex == 0,
                                hasTextBelowVisualAttachments = hasTextBelowVisualAttachments &&
                                    rowIndex == attachmentRows.lastIndex,
                                hasRoundedStartCorners = columnIndex == 0,
                                hasRoundedEndCorners = columnIndex == attachmentRow.lastIndex,
                            ),
                            onAttachmentClick = onAttachmentClick,
                            onExternalUriClick = onExternalUriClick,
                        )
                    }
                }

                if (attachmentRow.size == 1) {
                    Box(
                        modifier = Modifier.weight(weight = 1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationVisualAttachmentCard(
    modifier: Modifier,
    attachment: ConversationMessageAttachment,
    aspectRatio: Float,
    attachmentShape: RoundedCornerShape,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
) {
    ConversationVisualAttachmentSurface(
        modifier = modifier.aspectRatio(ratio = aspectRatio),
        attachment = attachment,
        attachmentShape = attachmentShape,
        contentScale = ContentScale.Crop,
        onAttachmentClick = onAttachmentClick,
        onExternalUriClick = onExternalUriClick,
        overlay = {
            if (attachment.requiresPlaybackAffordance()) {
                CenterPlayAffordance()
            }
        },
    )
}

@Composable
private fun ConversationVisualAttachmentSurface(
    modifier: Modifier,
    attachment: ConversationMessageAttachment,
    attachmentShape: RoundedCornerShape,
    contentScale: ContentScale,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val openAction = remember(attachment) {
        attachment.toConversationAttachmentOpenActionOrNull()
    }

    Surface(
        modifier = modifier
            .clip(shape = attachmentShape)
            .clickable(
                enabled = openAction != null,
                onClick = {
                    openAction?.let { action ->
                        dispatchConversationAttachmentOpenAction(
                            action = action,
                            onAttachmentClick = onAttachmentClick,
                            onExternalUriClick = onExternalUriClick,
                        )
                    }
                },
            ),
        shape = attachmentShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val thumbnailSize = remember(maxWidth, maxHeight, density) {
                with(density) {
                    IntSize(
                        width = maxWidth.roundToPx().coerceAtLeast(
                            minimumValue = MESSAGE_ATTACHMENT_MIN_PREVIEW_SIZE_PX,
                        ),
                        height = maxHeight.roundToPx().coerceAtLeast(
                            minimumValue = MESSAGE_ATTACHMENT_MIN_PREVIEW_SIZE_PX,
                        ),
                    )
                }
            }

            ConversationAttachmentThumbnail(
                modifier = Modifier.fillMaxSize(),
                attachment = attachment,
                contentScale = contentScale,
                thumbnailSize = thumbnailSize,
            )

            overlay()
        }
    }
}

@Composable
private fun ConversationAttachmentThumbnail(
    modifier: Modifier,
    attachment: ConversationMessageAttachment,
    contentScale: ContentScale,
    thumbnailSize: IntSize,
) {
    when (attachment) {
        is ConversationMessageAttachment.Media -> {
            ConversationMediaThumbnail(
                modifier = modifier,
                contentUri = attachment.part.contentUri.toString(),
                contentType = attachment.part.contentType,
                size = thumbnailSize,
                contentScale = contentScale,
            )
        }

        is ConversationMessageAttachment.YouTubePreview -> {
            ConversationMediaThumbnail(
                modifier = modifier,
                contentUri = attachment.thumbnailUrl,
                contentType = ContentType.IMAGE_JPEG,
                size = thumbnailSize,
                contentScale = contentScale,
            )
        }

        is ConversationMessageAttachment.Unsupported -> {
            Box(
                modifier = modifier.background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                )
            }
        }
    }
}

private fun visualAttachmentShape(
    hasTextAboveVisualAttachments: Boolean,
    hasTextBelowVisualAttachments: Boolean,
    hasRoundedStartCorners: Boolean = true,
    hasRoundedEndCorners: Boolean = true,
): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = roundedAttachmentCornerSize(
            shouldRoundCorner = hasRoundedStartCorners && !hasTextAboveVisualAttachments,
        ),
        topEnd = roundedAttachmentCornerSize(
            shouldRoundCorner = hasRoundedEndCorners && !hasTextAboveVisualAttachments,
        ),
        bottomStart = roundedAttachmentCornerSize(
            shouldRoundCorner = hasRoundedStartCorners && !hasTextBelowVisualAttachments,
        ),
        bottomEnd = roundedAttachmentCornerSize(
            shouldRoundCorner = hasRoundedEndCorners && !hasTextBelowVisualAttachments,
        ),
    )
}

private fun roundedAttachmentCornerSize(shouldRoundCorner: Boolean): Dp {
    return when {
        shouldRoundCorner -> MESSAGE_ATTACHMENT_CORNER_RADIUS
        else -> 0.dp
    }
}

@Composable
private fun BoxScope.CenterPlayAffordance() {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(size = 999.dp),
        modifier = Modifier.align(alignment = Alignment.Center),
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = Modifier
                .padding(all = 10.dp)
                .size(size = 26.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun ConversationMessageAttachment.requiresPlaybackAffordance(): Boolean {
    return when (this) {
        is ConversationMessageAttachment.Media -> part.isVideoAttachment
        is ConversationMessageAttachment.YouTubePreview -> true
        is ConversationMessageAttachment.Unsupported -> false
    }
}

private fun resolveAttachmentAspectRatio(
    attachment: ConversationMessageAttachment,
): Float {
    val preferredAspectRatio = when (attachment) {
        is ConversationMessageAttachment.Media -> {
            resolvePartAspectRatio(part = attachment.part)
        }

        is ConversationMessageAttachment.YouTubePreview -> {
            MESSAGE_ATTACHMENT_DEFAULT_VIDEO_ASPECT_RATIO
        }

        is ConversationMessageAttachment.Unsupported -> {
            MESSAGE_ATTACHMENT_DEFAULT_IMAGE_ASPECT_RATIO
        }
    }

    return preferredAspectRatio.coerceIn(
        minimumValue = MESSAGE_ATTACHMENT_MIN_ASPECT_RATIO,
        maximumValue = MESSAGE_ATTACHMENT_MAX_ASPECT_RATIO,
    )
}

private fun resolvePartAspectRatio(
    part: ConversationMessagePartUiModel,
): Float {
    val hasMeasuredSize = part.width > 0 && part.height > 0

    return when {
        hasMeasuredSize -> part.width.toFloat() / part.height.toFloat()
        part.isVideoAttachment -> MESSAGE_ATTACHMENT_DEFAULT_VIDEO_ASPECT_RATIO
        else -> MESSAGE_ATTACHMENT_DEFAULT_IMAGE_ASPECT_RATIO
    }
}
