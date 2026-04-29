package com.android.messaging.ui.conversation.v2.mediapicker.component.review

import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList

internal data class ConversationMediaReviewPagerState(
    val attachmentContentUris: ImmutableList<String>,
    val currentAttachment: ComposerAttachmentUiModel.Resolved.VisualMedia,
    val pagerState: PagerState,
    val visibleDeleteChipPage: Int?,
)

@Composable
internal fun rememberConversationMediaReviewPagerState(
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    initiallyReviewedContentUri: String?,
    reviewRequestSequence: Int,
    photoPickerSourceContentUriByAttachmentContentUri: ImmutableMap<String, String>,
): ConversationMediaReviewPagerState {
    val attachmentContentUris = remember(attachments) {
        attachments
            .asSequence()
            .map { it.contentUri }
            .toImmutableList()
    }

    val initiallyReviewedPage = resolveInitialReviewPage(
        attachments = attachments,
        initiallyReviewedContentUri = initiallyReviewedContentUri,
        photoPickerSourceContentUriByAttachmentContentUri =
            photoPickerSourceContentUriByAttachmentContentUri,
    )

    val pagerState = rememberPagerState(
        initialPage = initiallyReviewedPage,
        pageCount = { attachments.size },
    )

    val reviewPagerCoordinator = remember {
        ConversationMediaReviewPagerCoordinator(
            initialReviewRequestSequence = reviewRequestSequence,
        )
    }
    val settledReviewPage = clampAttachmentPage(
        page = pagerState.settledPage,
        attachments = attachments,
    )

    LaunchedEffect(
        attachmentContentUris,
        initiallyReviewedContentUri,
        photoPickerSourceContentUriByAttachmentContentUri,
        reviewRequestSequence,
        settledReviewPage,
    ) {
        reviewPagerCoordinator.syncTargetPage(
            attachmentContentUris = attachmentContentUris,
            attachments = attachments,
            initiallyReviewedContentUri = initiallyReviewedContentUri,
            reviewRequestSequence = reviewRequestSequence,
            photoPickerSourceContentUriByAttachmentContentUri =
                photoPickerSourceContentUriByAttachmentContentUri,
            pagerState = pagerState,
        )
    }
    val visibleDeleteChipPage = resolveVisibleDeleteChipPage(
        attachments = attachments,
        pagerState = pagerState,
    )

    return ConversationMediaReviewPagerState(
        attachmentContentUris = attachmentContentUris,
        currentAttachment = attachments[settledReviewPage],
        pagerState = pagerState,
        visibleDeleteChipPage = visibleDeleteChipPage,
    )
}

private class ConversationMediaReviewPagerCoordinator(
    initialReviewRequestSequence: Int,
) {
    private var pendingRequestedReviewContentUri: String? = null
    private var latestReviewRequestSequence: Int = initialReviewRequestSequence

    suspend fun syncTargetPage(
        attachmentContentUris: List<String>,
        attachments: List<ComposerAttachmentUiModel.Resolved.VisualMedia>,
        initiallyReviewedContentUri: String?,
        reviewRequestSequence: Int,
        photoPickerSourceContentUriByAttachmentContentUri: Map<String, String>,
        pagerState: PagerState,
    ) {
        if (reviewRequestSequence != latestReviewRequestSequence) {
            pendingRequestedReviewContentUri = initiallyReviewedContentUri
            latestReviewRequestSequence = reviewRequestSequence
        }

        val requestedAttachmentPage = resolveReviewedAttachmentPage(
            attachmentContentUris = attachmentContentUris,
            attachments = attachments,
            requestedReviewContentUri = pendingRequestedReviewContentUri,
            photoPickerSourceContentUriByAttachmentContentUri =
                photoPickerSourceContentUriByAttachmentContentUri,
        )

        val targetPage = requestedAttachmentPage ?: clampAttachmentPage(
            page = pagerState.currentPage,
            attachments = attachments,
        )

        if (pagerState.currentPage != targetPage) {
            when {
                requestedAttachmentPage != null -> {
                    pagerState.animateScrollToPage(
                        page = targetPage,
                        animationSpec = tween(durationMillis = 220),
                    )
                }

                else -> {
                    pagerState.scrollToPage(page = targetPage)
                }
            }
        }

        if (requestedAttachmentPage != null) {
            pendingRequestedReviewContentUri = null
        }
    }

    private fun resolveReviewedAttachmentPage(
        attachmentContentUris: List<String>,
        attachments: List<ComposerAttachmentUiModel.Resolved.VisualMedia>,
        requestedReviewContentUri: String?,
        photoPickerSourceContentUriByAttachmentContentUri: Map<String, String>,
    ): Int? {
        if (requestedReviewContentUri == null) {
            return null
        }

        return attachmentContentUris
            .indexOf(element = requestedReviewContentUri)
            .takeIf { it >= 0 }
            ?: attachments.indexOfFirst { attachment ->
                photoPickerSourceContentUriByAttachmentContentUri[attachment.contentUri] ==
                    requestedReviewContentUri
            }.takeIf { it >= 0 }
    }
}

private fun resolveInitialReviewPage(
    attachments: List<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    initiallyReviewedContentUri: String?,
    photoPickerSourceContentUriByAttachmentContentUri: Map<String, String>,
): Int {
    if (initiallyReviewedContentUri == null) {
        return attachments.lastIndex
    }

    return attachments
        .indexOfFirst { attachment ->
            attachment.contentUri == initiallyReviewedContentUri ||
                photoPickerSourceContentUriByAttachmentContentUri[attachment.contentUri] ==
                    initiallyReviewedContentUri
        }
        .takeIf { it >= 0 }
        ?: attachments.lastIndex
}

private fun clampAttachmentPage(
    page: Int,
    attachments: List<ComposerAttachmentUiModel.Resolved.VisualMedia>,
): Int {
    return page.coerceIn(
        minimumValue = 0,
        maximumValue = attachments.lastIndex,
    )
}

private fun resolveVisibleDeleteChipPage(
    attachments: List<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    pagerState: PagerState,
): Int? {
    val clampedCurrentPage = clampAttachmentPage(
        page = pagerState.currentPage,
        attachments = attachments,
    )

    val clampedSettledPage = clampAttachmentPage(
        page = pagerState.settledPage,
        attachments = attachments,
    )

    return when {
        !pagerState.isScrollInProgress -> clampedCurrentPage
        clampedCurrentPage == clampedSettledPage -> null
        else -> clampedCurrentPage
    }
}
