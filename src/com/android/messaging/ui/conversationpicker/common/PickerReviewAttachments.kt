package com.android.messaging.ui.conversationpicker.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.messaging.R
import com.android.messaging.ui.common.components.attachment.AudioAttachmentCell
import com.android.messaging.ui.common.components.attachment.VCardAttachmentCell
import com.android.messaging.ui.common.components.mediapreview.MediaPreviewCard
import com.android.messaging.ui.common.components.mediapreview.MediaReviewDeleteChip
import com.android.messaging.ui.common.components.mediapreview.mediaReviewPageTransform
import com.android.messaging.ui.common.components.mediapreview.mediaReviewVisibleDeleteChipPage
import com.android.messaging.ui.common.components.mediapreview.rememberMediaReviewRemovalState
import com.android.messaging.ui.conversationpicker.model.AttachmentUiModel
import kotlinx.collections.immutable.ImmutableList

private val ReviewPageSpacing = 12.dp
private val ReviewPageHorizontalPadding = 24.dp
private val ReviewPageVerticalPadding = 12.dp
private val ReviewDeleteChipPadding = 8.dp

@Composable
internal fun PickerReviewAttachments(
    attachments: ImmutableList<AttachmentUiModel>,
    pagerState: PagerState,
    onAttachmentClick: (String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) {
        return
    }

    val visibleDeleteChipPage = mediaReviewVisibleDeleteChipPage(
        pagerState = pagerState,
        pageCount = attachments.size,
    )

    HorizontalPager(
        modifier = modifier.testTag(ATTACHMENT_PREVIEW_LIST_TEST_TAG),
        state = pagerState,
        contentPadding = PaddingValues(horizontal = ReviewPageHorizontalPadding),
        pageSpacing = ReviewPageSpacing,
        key = { page -> attachments[page].id },
        verticalAlignment = Alignment.CenterVertically,
    ) { page ->
        val attachment = attachments[page]

        PickerReviewAttachmentPage(
            attachment = attachment,
            page = page,
            pagerState = pagerState,
            isOnlyItem = attachments.size == 1,
            shouldShowDeleteChip = page == visibleDeleteChipPage,
            onClick = { onAttachmentClick(attachment.id) },
            onRemove = { onAttachmentRemove(attachment.id) },
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = ReviewPageVerticalPadding)
                .testTag(attachmentItemTestTag(id = attachment.id)),
        )
    }
}

@Composable
private fun PickerReviewAttachmentPage(
    attachment: AttachmentUiModel,
    page: Int,
    pagerState: PagerState,
    isOnlyItem: Boolean,
    shouldShowDeleteChip: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (attachment) {
        is AttachmentUiModel.Media -> {
            ReviewMediaPage(
                attachment = attachment,
                page = page,
                pagerState = pagerState,
                isOnlyItem = isOnlyItem,
                shouldShowDeleteChip = shouldShowDeleteChip,
                removeButtonTestTag = attachmentRemoveButtonTestTag(id = attachment.id),
                onClick = onClick,
                onRemove = onRemove,
                modifier = modifier,
            )
        }

        is AttachmentUiModel.Audio -> {
            ReviewCenteredCard(
                modifier = modifier.mediaReviewPageTransform(
                    page = page,
                    pagerState = pagerState,
                    removalProgress = 1f,
                ),
            ) {
                AudioAttachmentCell(
                    durationMillis = attachment.durationMillis,
                    onClick = onClick,
                    onRemove = onRemove,
                    removeButtonTestTag = attachmentRemoveButtonTestTag(id = attachment.id),
                )
            }
        }

        is AttachmentUiModel.VCard -> {
            ReviewCenteredCard(
                modifier = modifier.mediaReviewPageTransform(
                    page = page,
                    pagerState = pagerState,
                    removalProgress = 1f,
                ),
            ) {
                VCardAttachmentCell(
                    kind = attachment.kind,
                    avatarUri = null,
                    avatarName = attachment.title,
                    title = attachment.title
                        ?: stringResource(id = R.string.mediapicker_contact_title),
                    subtitle = null,
                    onClick = onClick,
                    onRemove = onRemove,
                    removeButtonTestTag = attachmentRemoveButtonTestTag(id = attachment.id),
                )
            }
        }
    }
}

@Composable
private fun ReviewMediaPage(
    attachment: AttachmentUiModel.Media,
    page: Int,
    pagerState: PagerState,
    isOnlyItem: Boolean,
    shouldShowDeleteChip: Boolean,
    removeButtonTestTag: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val removalState = rememberMediaReviewRemovalState(
        itemKey = attachment.id,
        isOnlyItem = isOnlyItem,
        shouldShowDeleteChip = shouldShowDeleteChip,
        onRemove = onRemove,
        onClearAfterLastRemoval = {},
    )

    Box(
        modifier = modifier.mediaReviewPageTransform(
            page = page,
            pagerState = pagerState,
            removalProgress = removalState.removalProgress,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = MaterialTheme.shapes.large)
                .clickable(
                    enabled = removalState.isInteractionEnabled,
                    onClick = onClick,
                ),
        ) {
            val density = LocalDensity.current
            val previewSize = remember(maxWidth, maxHeight, density) {
                with(density) {
                    IntSize(
                        width = maxWidth.roundToPx().coerceAtLeast(minimumValue = 1),
                        height = maxHeight.roundToPx().coerceAtLeast(minimumValue = 1),
                    )
                }
            }

            MediaPreviewCard(
                modifier = Modifier.fillMaxSize(),
                contentUri = attachment.id,
                contentType = attachment.contentType,
                isVideo = attachment.isVideo,
                previewSize = previewSize,
            )
        }

        if (removalState.isDeleteChipVisible) {
            MediaReviewDeleteChip(
                modifier = Modifier
                    .align(alignment = Alignment.TopEnd)
                    .zIndex(zIndex = 1f)
                    .padding(all = ReviewDeleteChipPadding)
                    .testTag(removeButtonTestTag),
                visibilityProgress = removalState.deleteChipVisibilityProgress,
                contentDescription = pluralStringResource(
                    id = R.plurals.attachment_preview_close_content_description,
                    count = 1,
                ),
                onClick = removalState.markPendingRemoval,
            )
        }
    }
}

@Composable
private fun ReviewCenteredCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
