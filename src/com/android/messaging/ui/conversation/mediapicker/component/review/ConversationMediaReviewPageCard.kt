package com.android.messaging.ui.conversation.mediapicker.component.review

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.android.messaging.R
import com.android.messaging.ui.conversation.attachment.ui.ConversationMediaThumbnail
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.mediapicker.component.PickerOverlayBackgroundButton
import com.android.messaging.ui.conversation.mediapicker.component.pickerOverlayContainerColor
import com.android.messaging.ui.conversation.mediapicker.component.pickerOverlayContentColor
import com.android.messaging.ui.conversation.preview.previewResolvedImageAttachment
import com.android.messaging.ui.conversation.preview.previewResolvedVideoAttachment
import com.android.messaging.ui.core.MessagingPreviewColumn
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay

private const val PICKER_REVIEW_PAGE_REMOVE_ANIMATION_DURATION_MILLIS = 160

@Composable
internal fun ConversationMediaReviewPageCard(
    attachment: ComposerAttachmentUiModel.Resolved.VisualMedia,
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    page: Int,
    pageHeight: Dp,
    pageWidth: Dp,
    pagerState: PagerState,
    previewSize: IntSize,
    shouldShowDeleteChip: Boolean,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onClearReview: () -> Unit,
) {
    val pageCardState = rememberConversationMediaReviewPageCardState(
        attachment = attachment,
        attachments = attachments,
        shouldShowDeleteChip = shouldShowDeleteChip,
        onAttachmentRemove = onAttachmentRemove,
        onClearReview = onClearReview,
    )

    ConversationMediaReviewPageCardContent(
        attachment = attachment,
        page = page,
        pageHeight = pageHeight,
        pageWidth = pageWidth,
        pagerState = pagerState,
        previewSize = previewSize,
        contentState = pageCardState.contentState,
        onAttachmentPreviewClick = onAttachmentPreviewClick,
        onAttachmentRemoveClick = { pageCardState.markPendingRemoval() },
    )
}

@Composable
private fun rememberConversationMediaReviewPageCardState(
    attachment: ComposerAttachmentUiModel.Resolved.VisualMedia,
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    shouldShowDeleteChip: Boolean,
    onAttachmentRemove: (String) -> Unit,
    onClearReview: () -> Unit,
): ConversationMediaReviewPageCardState {
    var isPendingRemoval by remember(attachment.contentUri) {
        mutableStateOf(false)
    }

    val deleteChipVisibilityProgress by animateFloatAsState(
        targetValue = when {
            shouldShowDeleteChip && !isPendingRemoval -> 1f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 120),
        label = "reviewDeleteChipVisibility",
    )

    val removalVisibilityProgress by animateFloatAsState(
        targetValue = when {
            isPendingRemoval -> 0f
            else -> 1f
        },
        animationSpec = tween(durationMillis = PICKER_REVIEW_PAGE_REMOVE_ANIMATION_DURATION_MILLIS),
        label = "reviewPageRemovalVisibility",
    )

    val shouldClearReviewAfterRemoval = attachments.size == 1

    LaunchedEffect(isPendingRemoval) {
        if (!isPendingRemoval) {
            return@LaunchedEffect
        }

        delay(timeMillis = PICKER_REVIEW_PAGE_REMOVE_ANIMATION_DURATION_MILLIS.toLong())
        onAttachmentRemove(attachment.contentUri)

        if (shouldClearReviewAfterRemoval) {
            onClearReview()
        }
    }

    return ConversationMediaReviewPageCardState(
        contentState = ConversationMediaReviewPageCardContentState(
            isPreviewEnabled = !isPendingRemoval,
            isDeleteChipVisible = deleteChipVisibilityProgress > 0f,
            deleteChipVisibilityProgress = deleteChipVisibilityProgress,
            removalVisibilityProgress = removalVisibilityProgress,
        ),
        markPendingRemoval = {
            if (!isPendingRemoval) {
                isPendingRemoval = true
            }
        },
    )
}

@Composable
private fun ConversationMediaReviewPageCardContent(
    attachment: ComposerAttachmentUiModel.Resolved.VisualMedia,
    page: Int,
    pageHeight: Dp,
    pageWidth: Dp,
    pagerState: PagerState,
    previewSize: IntSize,
    contentState: ConversationMediaReviewPageCardContentState,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onAttachmentRemoveClick: () -> Unit,
) {
    val pageCardModifier = Modifier
        .fillMaxSize()
        .padding(vertical = 4.dp)
        .wrapContentSize(align = Alignment.Center)
        .width(width = pageWidth)
        .height(height = pageHeight)
        .graphicsLayer {
            val pageOffset = resolveReviewPageOffset(
                page = page,
                pagerState = pagerState,
            )
            val pageScale = lerp(
                start = 0.98f,
                stop = 1f,
                fraction = 1f - pageOffset,
            )
            val removalScale = lerp(
                start = 0.9f,
                stop = 1f,
                fraction = contentState.removalVisibilityProgress,
            )
            alpha = contentState.removalVisibilityProgress
            scaleX = pageScale * removalScale
            scaleY = pageScale * removalScale
        }

    Box(
        modifier = pageCardModifier,
    ) {
        ConversationMediaReviewPreview(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = contentState.isPreviewEnabled,
                    onClick = { onAttachmentPreviewClick(attachment) },
                ),
            attachment = attachment,
            previewSize = previewSize,
        )

        if (contentState.isDeleteChipVisible) {
            ConversationMediaReviewDeleteButton(
                modifier = Modifier
                    .align(alignment = Alignment.TopEnd)
                    .zIndex(zIndex = 1f)
                    .padding(
                        top = 8.dp,
                        end = 8.dp,
                    ),
                visibilityProgress = contentState.deleteChipVisibilityProgress,
                onClick = onAttachmentRemoveClick,
            )
        }
    }
}

@Composable
private fun ConversationMediaReviewPreview(
    attachment: ComposerAttachmentUiModel.Resolved.VisualMedia,
    modifier: Modifier = Modifier,
    previewSize: IntSize,
) {
    val previewShape = MaterialTheme.shapes.large

    Surface(
        modifier = modifier
            .clip(previewShape),
        shape = previewShape,
        color = MaterialTheme
            .colorScheme
            .surfaceColorAtElevation(elevation = 6.dp)
            .copy(alpha = 0.25f),
        shadowElevation = 20.dp,
        tonalElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ConversationMediaThumbnail(
                modifier = Modifier.fillMaxSize(),
                contentUri = attachment.contentUri,
                contentType = attachment.contentType,
                size = previewSize,
                contentScale = ContentScale.Crop,
                backgroundColor = Color.Transparent,
            )

            if (attachment is ComposerAttachmentUiModel.Resolved.VisualMedia.Video) {
                ConversationMediaReviewVideoBadge()
            }
        }
    }
}

@Composable
private fun ConversationMediaReviewVideoBadge(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = pickerOverlayContainerColor(alpha = 0.5f),
    ) {
        Icon(
            modifier = Modifier.padding(12.dp),
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = pickerOverlayContentColor(),
        )
    }
}

@Composable
private fun ConversationMediaReviewDeleteButton(
    modifier: Modifier = Modifier,
    visibilityProgress: Float,
    onClick: () -> Unit,
) {
    val scale = lerp(
        start = 0.9f,
        stop = 1f,
        fraction = visibilityProgress,
    )

    PickerOverlayBackgroundButton(
        modifier = modifier.graphicsLayer {
            alpha = visibilityProgress
            scaleX = scale
            scaleY = scale
        },
        containerColor = pickerOverlayContainerColor(alpha = 0.5f),
        contentDescription = stringResource(
            id = R.string.conversation_media_picker_remove_attachment_content_description,
        ),
        buttonSize = 32.dp,
        iconSize = 18.dp,
        imageVector = Icons.Rounded.Close,
        onClick = onClick,
    )
}

private fun resolveReviewPageOffset(
    page: Int,
    pagerState: PagerState,
): Float {
    val rawPageOffset = when {
        pagerState.isScrollInProgress -> {
            pagerState.currentPage - page + pagerState.currentPageOffsetFraction
        }

        else -> (pagerState.settledPage - page).toFloat()
    }

    return rawPageOffset.absoluteValue.coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
}

@Immutable
private data class ConversationMediaReviewPageCardState(
    val contentState: ConversationMediaReviewPageCardContentState,
    val markPendingRemoval: () -> Unit,
)

@Immutable
private data class ConversationMediaReviewPageCardContentState(
    val isPreviewEnabled: Boolean,
    val isDeleteChipVisible: Boolean,
    val deleteChipVisibilityProgress: Float,
    val removalVisibilityProgress: Float,
)

@PreviewLightDark
@Composable
private fun ConversationMediaReviewPageCardPreview() {
    val attachments = persistentListOf(
        previewResolvedImageAttachment(),
        previewResolvedVideoAttachment(),
    )
    val pagerState = rememberPagerState(pageCount = { attachments.size })
    MessagingPreviewColumn {
        ConversationMediaReviewPageCard(
            attachment = attachments.first(),
            attachments = attachments,
            page = 0,
            pageHeight = 240.dp,
            pageWidth = 192.dp,
            pagerState = pagerState,
            previewSize = IntSize(width = 384, height = 480),
            shouldShowDeleteChip = true,
            onAttachmentPreviewClick = { _ -> },
            onAttachmentRemove = { _ -> },
            onClearReview = {},
        )
    }
}
