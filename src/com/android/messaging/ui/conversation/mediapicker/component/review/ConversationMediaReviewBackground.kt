package com.android.messaging.ui.conversation.mediapicker.component.review

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
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.mediapicker.component.pickerOverlayContainerColor
import com.android.messaging.ui.conversation.preview.previewResolvedImageAttachment
import com.android.messaging.ui.conversation.preview.previewResolvedVideoAttachment
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val PICKER_REVIEW_BACKGROUND_BITMAP_SIZE_PX = 40

@Composable
internal fun ConversationMediaReviewBackground(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
) {
    val backgroundState = rememberConversationMediaReviewBackgroundState(
        pagerState = pagerState,
        attachments = attachments,
    )

    ConversationMediaReviewBackgroundContent(
        modifier = modifier,
        state = backgroundState,
    )
}

@Composable
private fun ConversationMediaReviewBackgroundContent(
    modifier: Modifier = Modifier,
    state: ConversationMediaReviewBackgroundState,
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
                        color = pickerOverlayContainerColor(alpha = 0.5f),
                    ),
            )
        }
    }
}

@Composable
private fun rememberConversationMediaReviewBackgroundState(
    pagerState: PagerState,
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
): ConversationMediaReviewBackgroundState {
    val backgroundSelection = remember(
        attachments,
        pagerState.settledPage,
    ) {
        getConversationMediaReviewBackgroundSelection(
            attachments = attachments,
            settledPage = pagerState.settledPage,
        )
    }

    val backgroundBitmapCache = rememberConversationMediaReviewBitmapCache(
        attachments = attachments,
        attachmentsToPrefetch = backgroundSelection.attachmentsToPrefetch,
    )

    val fallbackBackgroundColor = MaterialTheme
        .colorScheme
        .surfaceContainerHighest
        .copy(alpha = 0.9f)

    val settledBackgroundBitmap = backgroundSelection
        .attachmentsToPrefetch
        .firstOrNull()
        ?.let { attachment ->
            backgroundBitmapCache[attachment.contentUri]
        }

    val settledBackgroundImageBitmap = settledBackgroundBitmap?.asImageBitmap()

    return ConversationMediaReviewBackgroundState(
        settledBackgroundImageBitmap = settledBackgroundImageBitmap,
        fallbackBackgroundColor = fallbackBackgroundColor,
    )
}

@Composable
private fun rememberConversationMediaReviewBitmapCache(
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    attachmentsToPrefetch: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
): ConversationMediaReviewBitmapCache {
    val context = LocalContext.current

    val backgroundBitmapCache = remember {
        ConversationMediaReviewBitmapCache()
    }

    LaunchedEffect(attachments) {
        backgroundBitmapCache.removeInactive(
            activeContentUris = attachments
                .asSequence()
                .map { it.contentUri }
                .toSet(),
        )
    }

    LaunchedEffect(attachmentsToPrefetch) {
        attachmentsToPrefetch
            .asSequence()
            .filter { backgroundBitmapCache[it.contentUri] == null }
            .forEach { attachment ->
                loadMediaThumbnailBitmap(
                    contentResolver = context.contentResolver,
                    contentUri = attachment.contentUri.toUri(),
                    contentType = attachment.contentType,
                    size = IntSize(
                        width = PICKER_REVIEW_BACKGROUND_BITMAP_SIZE_PX,
                        height = PICKER_REVIEW_BACKGROUND_BITMAP_SIZE_PX,
                    ),
                    softenBitmap = true,
                )?.let { bitmap ->
                    backgroundBitmapCache.put(
                        contentUri = attachment.contentUri,
                        bitmap = bitmap,
                    )
                }
            }
    }

    return backgroundBitmapCache
}

private fun getConversationMediaReviewBackgroundSelection(
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    settledPage: Int,
): ConversationMediaReviewBackgroundSelection {
    if (attachments.isEmpty()) {
        return ConversationMediaReviewBackgroundSelection(
            attachmentsToPrefetch = persistentListOf(),
        )
    }

    val settledIndex = settledPage.coerceIn(
        minimumValue = 0,
        maximumValue = attachments.lastIndex,
    )

    val settledAttachment = attachments[settledIndex]

    val previousAttachment = attachments
        .getOrNull(index = settledIndex - 1)
        ?.takeIf { it.contentUri != settledAttachment.contentUri }

    val nextAttachment = attachments
        .getOrNull(index = settledIndex + 1)
        ?.takeIf { attachment ->
            attachment.contentUri != settledAttachment.contentUri &&
                attachment.contentUri != previousAttachment?.contentUri
        }

    val attachmentsToPrefetch = listOfNotNull(
        settledAttachment,
        previousAttachment,
        nextAttachment,
    ).toImmutableList()

    return ConversationMediaReviewBackgroundSelection(
        attachmentsToPrefetch = attachmentsToPrefetch,
    )
}

@Immutable
private data class ConversationMediaReviewBackgroundSelection(
    val attachmentsToPrefetch: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
)

@Immutable
private data class ConversationMediaReviewBackgroundState(
    val settledBackgroundImageBitmap: ImageBitmap?,
    val fallbackBackgroundColor: Color,
)

@PreviewLightDark
@Composable
private fun ConversationMediaReviewBackgroundPreview() {
    val attachments = persistentListOf(
        previewResolvedImageAttachment(),
        previewResolvedVideoAttachment(),
    )
    MessagingPreviewTheme {
        ConversationMediaReviewBackground(
            modifier = Modifier.fillMaxSize(),
            pagerState = rememberPagerState(pageCount = { attachments.size }),
            attachments = attachments,
        )
    }
}
