package com.android.messaging.ui.conversation.v2.mediapicker.component.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.composer.ui.ConversationSendActionButton
import com.android.messaging.ui.conversation.v2.composer.ui.ConversationSendActionButtonMode
import com.android.messaging.ui.conversation.v2.mediapicker.component.PickerOverlayIconButton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

private const val PICKER_REVIEW_PAGE_ASPECT_RATIO = 0.8f
private const val PICKER_REVIEW_PAGE_MAX_HEIGHT_FRACTION = 0.95f
private const val PICKER_REVIEW_PAGE_WIDTH_FRACTION = 0.8f

@Composable
internal fun ConversationMediaReviewScene(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    conversationTitle: String?,
    initiallyReviewedContentUri: String?,
    reviewRequestSequence: Int,
    isSendActionEnabled: Boolean,
    photoPickerSourceContentUriByAttachmentContentUri: ImmutableMap<String, String> =
        persistentMapOf(),
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onCaptionChange: (String, String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onAddMoreClick: () -> Unit,
    onClearReview: () -> Unit,
    onCloseClick: () -> Unit,
    onSendClick: () -> Unit,
) {
    if (attachments.isEmpty()) {
        return
    }

    val reviewPagerState = rememberConversationMediaReviewPagerState(
        attachments = attachments,
        initiallyReviewedContentUri = initiallyReviewedContentUri,
        reviewRequestSequence = reviewRequestSequence,
        photoPickerSourceContentUriByAttachmentContentUri =
            photoPickerSourceContentUriByAttachmentContentUri,
    )

    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    val reviewBottomPadding = maxOf(
        contentPadding.calculateBottomPadding(),
        imeBottomPadding,
    ) + 12.dp

    Box(
        modifier = modifier,
    ) {
        ConversationMediaReviewBackground(
            modifier = Modifier
                .fillMaxSize(),
            pagerState = reviewPagerState.pagerState,
            attachments = attachments,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 12.dp,
                    bottom = reviewBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ConversationMediaReviewTopBar(
                conversationTitle = conversationTitle,
                onAddMoreClick = onAddMoreClick,
                onCloseClick = onCloseClick,
            )

            ConversationMediaReviewPager(
                modifier = Modifier
                    .weight(weight = 1f)
                    .fillMaxWidth(),
                attachmentContentUris = reviewPagerState.attachmentContentUris,
                attachments = attachments,
                pagerState = reviewPagerState.pagerState,
                visibleDeleteChipPage = reviewPagerState.visibleDeleteChipPage,
                onAttachmentPreviewClick = onAttachmentPreviewClick,
                onAttachmentRemove = onAttachmentRemove,
                onClearReview = onClearReview,
            )

            ConversationMediaReviewBottomBar(
                attachment = reviewPagerState.currentAttachment,
                isSendActionEnabled = isSendActionEnabled,
                onCaptionChange = onCaptionChange,
                onSendClick = onSendClick,
            )
        }
    }
}

@Composable
private fun ConversationMediaReviewTopBar(
    conversationTitle: String?,
    onAddMoreClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickerOverlayIconButton(
            contentDescription = stringResource(
                id = R.string.conversation_media_picker_close_content_description,
            ),
            imageVector = Icons.Rounded.Close,
            onClick = onCloseClick,
        )
        Text(
            modifier = Modifier
                .weight(weight = 1f),
            text = conversationTitle.orEmpty(),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
        )
        PickerOverlayIconButton(
            contentDescription = stringResource(
                id = R.string.conversation_media_picker_add_more_content_description,
            ),
            imageVector = Icons.Rounded.AddAPhoto,
            onClick = onAddMoreClick,
        )
    }
}

@Composable
private fun ConversationMediaReviewPager(
    modifier: Modifier = Modifier,
    attachmentContentUris: ImmutableList<String>,
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    pagerState: PagerState,
    visibleDeleteChipPage: Int?,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onClearReview: () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val maxPageWidth = maxWidth * PICKER_REVIEW_PAGE_WIDTH_FRACTION
        val maxPageHeight = maxHeight * PICKER_REVIEW_PAGE_MAX_HEIGHT_FRACTION
        val pageWidthFromHeight = maxPageHeight * PICKER_REVIEW_PAGE_ASPECT_RATIO

        val pageWidth = when {
            maxPageWidth <= pageWidthFromHeight -> maxPageWidth
            else -> pageWidthFromHeight
        }

        val pageHeight = pageWidth / PICKER_REVIEW_PAGE_ASPECT_RATIO
        val pageHorizontalInset = (maxWidth - pageWidth) / 2
        val density = LocalDensity.current
        val currentPreviewSize = remember(pageWidth, pageHeight, density) {
            with(density) {
                IntSize(
                    width = pageWidth.roundToPx().coerceAtLeast(minimumValue = 1),
                    height = pageHeight.roundToPx().coerceAtLeast(minimumValue = 1),
                )
            }
        }

        val previewSize = rememberLargestReviewPreviewSize(
            currentPreviewSize = currentPreviewSize,
        )

        HorizontalPager(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            state = pagerState,
            contentPadding = PaddingValues(horizontal = pageHorizontalInset),
            pageSize = PageSize.Fixed(pageWidth),
            pageSpacing = 12.dp,
            key = { page ->
                attachmentContentUris.getOrElse(index = page) {
                    "stale-review-page-$page"
                }
            },
        ) { page ->
            val attachment = attachments.getOrNull(index = page)

            when {
                attachment != null -> {
                    ConversationMediaReviewPageCard(
                        attachment = attachment,
                        attachments = attachments,
                        page = page,
                        pageHeight = pageHeight,
                        pageWidth = pageWidth,
                        pagerState = pagerState,
                        previewSize = previewSize,
                        shouldShowDeleteChip = page == visibleDeleteChipPage,
                        onAttachmentPreviewClick = onAttachmentPreviewClick,
                        onAttachmentRemove = onAttachmentRemove,
                        onClearReview = onClearReview,
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberLargestReviewPreviewSize(
    currentPreviewSize: IntSize,
): IntSize {
    var largestPreviewSize by remember {
        mutableStateOf(value = currentPreviewSize)
    }

    SideEffect {
        val updatedPreviewSize = IntSize(
            width = maxOf(
                largestPreviewSize.width,
                currentPreviewSize.width,
            ),
            height = maxOf(
                largestPreviewSize.height,
                currentPreviewSize.height,
            ),
        )

        if (updatedPreviewSize != largestPreviewSize) {
            largestPreviewSize = updatedPreviewSize
        }
    }

    return largestPreviewSize
}

@Composable
private fun ReviewCaptionTextField(
    modifier: Modifier = Modifier,
    captionText: String,
    onCaptionChange: (String) -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f)

    TextField(
        modifier = modifier
            .fillMaxWidth(),
        value = captionText,
        onValueChange = onCaptionChange,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.8f,
            ),
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.5f,
            ),
        ),
        placeholder = {
            Text(
                text = stringResource(R.string.conversation_media_picker_caption_hint),
            )
        },
        singleLine = true,
    )
}

@Composable
private fun ConversationMediaReviewBottomBar(
    attachment: ComposerAttachmentUiModel.Resolved.VisualMedia,
    isSendActionEnabled: Boolean,
    onCaptionChange: (String, String) -> Unit,
    onSendClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReviewCaptionTextField(
            modifier = Modifier.weight(weight = 1f),
            captionText = attachment.captionText,
            onCaptionChange = { captionText ->
                onCaptionChange(
                    attachment.contentUri,
                    captionText,
                )
            },
        )

        ConversationSendActionButton(
            enabled = isSendActionEnabled,
            mode = ConversationSendActionButtonMode.Send,
            isRecordingActive = false,
            isRecordingLocked = false,
            onClick = onSendClick,
            onLockedStopClick = {},
            onRecordGestureStart = {},
            onRecordGestureMove = { _ -> },
            onRecordGestureLock = { false },
            onRecordGestureFinish = {},
        )
    }
}
