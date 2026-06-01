package com.android.messaging.ui.conversation.mediapicker.component.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.messaging.R
import com.android.messaging.ui.common.components.mediapreview.MediaPreviewCard
import com.android.messaging.ui.common.components.mediapreview.MediaReviewDeleteChip
import com.android.messaging.ui.common.components.mediapreview.mediaReviewPageTransform
import com.android.messaging.ui.common.components.mediapreview.rememberMediaReviewRemovalState
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.conversationMediaReviewPreviewTestTag
import com.android.messaging.ui.conversation.preview.previewResolvedImageAttachment
import com.android.messaging.ui.conversation.preview.previewResolvedVideoAttachment
import com.android.messaging.ui.core.MessagingPreviewColumn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal const val PICKER_REVIEW_PAGE_REMOVE_ANIMATION_DURATION_MILLIS = 160

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
    val removalState = rememberMediaReviewRemovalState(
        itemKey = attachment.contentUri,
        isOnlyItem = attachments.size == 1,
        shouldShowDeleteChip = shouldShowDeleteChip,
        onRemove = { onAttachmentRemove(attachment.contentUri) },
        onClearAfterLastRemoval = onClearReview,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .wrapContentSize(align = Alignment.Center)
            .width(width = pageWidth)
            .height(height = pageHeight)
            .mediaReviewPageTransform(
                page = page,
                pagerState = pagerState,
                removalProgress = removalState.removalProgress,
            ),
    ) {
        ConversationMediaReviewPreview(
            modifier = Modifier
                .fillMaxSize()
                .testTag(
                    tag = conversationMediaReviewPreviewTestTag(
                        contentUri = attachment.contentUri,
                    ),
                )
                .clickable(
                    enabled = removalState.isInteractionEnabled,
                    onClick = { onAttachmentPreviewClick(attachment) },
                ),
            attachment = attachment,
            previewSize = previewSize,
        )

        if (removalState.isDeleteChipVisible) {
            MediaReviewDeleteChip(
                modifier = Modifier
                    .align(alignment = Alignment.TopEnd)
                    .zIndex(zIndex = 1f)
                    .padding(
                        top = 8.dp,
                        end = 8.dp,
                    ),
                visibilityProgress = removalState.deleteChipVisibilityProgress,
                contentDescription = stringResource(
                    id = R.string.conversation_media_picker_remove_attachment_content_description,
                ),
                onClick = removalState.markPendingRemoval,
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
    MediaPreviewCard(
        modifier = modifier,
        contentUri = attachment.contentUri,
        contentType = attachment.contentType,
        isVideo = attachment is ComposerAttachmentUiModel.Resolved.VisualMedia.Video,
        previewSize = previewSize,
    )
}

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
