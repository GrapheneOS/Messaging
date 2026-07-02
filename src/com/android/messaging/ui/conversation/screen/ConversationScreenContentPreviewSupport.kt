@file:Suppress("TooManyFunctions")

package com.android.messaging.ui.conversation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.preview.previewAudioPart
import com.android.messaging.ui.conversation.preview.previewComposerUiState
import com.android.messaging.ui.conversation.preview.previewFilePart
import com.android.messaging.ui.conversation.preview.previewGroupMetadata
import com.android.messaging.ui.conversation.preview.previewImagePart
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.conversation.preview.previewMessagesUiState
import com.android.messaging.ui.conversation.preview.previewMetadata
import com.android.messaging.ui.conversation.preview.previewMmsDownloadUiModel
import com.android.messaging.ui.conversation.preview.previewOutgoingMessage
import com.android.messaging.ui.conversation.preview.previewSubscription
import com.android.messaging.ui.conversation.preview.previewVCardPart
import com.android.messaging.ui.conversation.preview.previewVideoPart
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

private const val PREVIEW_SCREEN_CONTENT_NOW_MILLIS = 1_806_240_000_000L
private const val PREVIEW_SCREEN_CONTENT_YESTERDAY_MILLIS =
    PREVIEW_SCREEN_CONTENT_NOW_MILLIS - 86_400_000L
private const val PREVIEW_SCREEN_CONTENT_OLDER_MILLIS =
    PREVIEW_SCREEN_CONTENT_NOW_MILLIS - 172_800_000L
private const val PREVIEW_SCREEN_CONTENT_LONG_TEXT = "This longer conversation message checks " +
    "how the full content surface behaves when a row wraps over several lines, keeps metadata " +
    "readable, and still leaves enough room for grouped incoming sender identity."
private const val PREVIEW_SCREEN_CONTENT_OVERFLOW_TEXT =
    "ConversationScreenContentPreviewTokenWithoutBreaksABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" +
        "ConversationScreenContentPreviewTokenWithoutBreaksABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

@Composable
internal fun ConversationScreenContentPreview(
    uiState: ConversationScreenScaffoldUiState,
    conversationId: String? = "conversation-1",
) {
    MessagingPreviewTheme {
        ConversationScreenContent(
            modifier = Modifier.fillMaxSize(),
            conversationId = conversationId,
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
            contentPadding = PaddingValues(),
            pendingScrollPosition = null,
            onPendingScrollPositionConsumed = {},
            onAttachmentClick = { _, _ -> },
            onExternalUriClick = {},
            onMessageClick = {},
            onMessageAvatarClick = {},
            onMessageDownloadClick = {},
            onMessageLongClick = {},
            onMessageResendClick = {},
            onSimSelectorClick = {},
            onUnblockClick = {},
        )
    }
}

internal fun previewConversationScreenContentLoadingUiState(): ConversationScreenScaffoldUiState {
    return ConversationScreenScaffoldUiState()
}

internal fun previewConversationScreenContentSimLoadingUiState():
    ConversationScreenScaffoldUiState {
    return previewScreenContentUiState(
        metadata = previewMetadata(),
        messages = ConversationMessagesUiState.Present(
            messages = previewScreenContentDirectMessages(),
        ),
        composer = previewComposerUiState().copy(
            simSelector = ConversationSimSelectorUiState(isLoading = true),
        ),
    )
}

internal fun previewConversationScreenContentEmptyUiState(): ConversationScreenScaffoldUiState {
    return previewScreenContentUiState(
        metadata = previewMetadata(),
        messages = ConversationMessagesUiState.Present(messages = persistentListOf()),
        composer = previewSingleSimComposerUiState(),
    )
}

internal fun previewConversationScreenContentDirectUiState(): ConversationScreenScaffoldUiState {
    return previewScreenContentUiState(
        metadata = previewMetadata(),
        messages = ConversationMessagesUiState.Present(
            messages = previewScreenContentDirectMessages(),
        ),
        composer = previewSingleSimComposerUiState(),
    )
}

internal fun previewConversationScreenContentPresentUiState(): ConversationScreenScaffoldUiState {
    return ConversationScreenScaffoldUiState(
        metadata = previewGroupMetadata(),
        messages = previewMessagesUiState(),
        composer = previewComposerUiState(),
    )
}

internal fun previewConversationScreenContentGroupRichUiState(): ConversationScreenScaffoldUiState {
    return previewScreenContentUiState(
        metadata = previewGroupMetadata(),
        messages = ConversationMessagesUiState.Present(
            messages = previewScreenContentRichMessages(),
        ),
        composer = previewComposerUiState(),
    )
}

internal fun previewConversationScreenContentSelectionUiState(): ConversationScreenScaffoldUiState {
    return previewScreenContentUiState(
        metadata = previewGroupMetadata(),
        messages = ConversationMessagesUiState.Present(
            messages = previewScreenContentRichMessages(),
        ),
        composer = previewComposerUiState(),
        selection = ConversationMessageSelectionUiState(
            selectedMessageIds = persistentSetOf(
                "screen-group-attachments",
                "screen-group-failed",
            ),
            availableActions = persistentSetOf(
                ConversationMessageSelectionAction.Copy,
                ConversationMessageSelectionAction.Delete,
                ConversationMessageSelectionAction.Forward,
                ConversationMessageSelectionAction.SaveAttachment,
            ),
        ),
    )
}

internal fun previewConversationScreenContentDateSeparatedUiState():
    ConversationScreenScaffoldUiState {
    return previewScreenContentUiState(
        metadata = previewGroupMetadata(),
        messages = ConversationMessagesUiState.Present(
            messages = previewScreenContentDateSeparatedMessages(),
        ),
        composer = previewComposerUiState(),
    )
}

internal fun previewConversationScreenContentMmsDownloadUiState():
    ConversationScreenScaffoldUiState {
    return previewScreenContentUiState(
        metadata = previewGroupMetadata(),
        messages = ConversationMessagesUiState.Present(
            messages = previewScreenContentMmsDownloadMessages(),
        ),
        composer = previewSingleSimComposerUiState(),
    )
}

internal fun previewConversationScreenContentNoSendSimUiState(): ConversationScreenScaffoldUiState {
    return previewScreenContentUiState(
        metadata = ConversationMetadataUiState.Unavailable,
        messages = ConversationMessagesUiState.Present(
            messages = previewScreenContentDirectMessages(),
        ),
        composer = previewDisabledComposerUiState(),
    )
}

private fun previewScreenContentUiState(
    metadata: ConversationMetadataUiState,
    messages: ConversationMessagesUiState,
    composer: ConversationComposerUiState,
    selection: ConversationMessageSelectionUiState = ConversationMessageSelectionUiState(),
): ConversationScreenScaffoldUiState {
    return ConversationScreenScaffoldUiState(
        metadata = metadata,
        messages = messages,
        composer = composer,
        selection = selection,
    )
}

private fun previewSingleSimComposerUiState(): ConversationComposerUiState {
    val subscription = previewSubscription()
    return previewComposerUiState(messageText = "").copy(
        simSelector = ConversationSimSelectorUiState(
            subscriptions = persistentListOf(subscription),
            selectedSubscription = subscription,
            isLoading = false,
        ),
        isSendEnabled = false,
        shouldShowRecordAction = true,
        hasWorkingDraft = false,
    )
}

private fun previewDisabledComposerUiState(): ConversationComposerUiState {
    return previewSingleSimComposerUiState().copy(
        isMessageFieldEnabled = false,
        isAttachmentActionEnabled = false,
        isRecordActionEnabled = false,
        shouldShowRecordAction = false,
    )
}

private fun previewScreenContentDirectMessages(): ImmutableList<ConversationMessageUiModel> {
    return persistentListOf(
        previewIncomingMessage(
            messageId = "screen-direct-address",
            text = "The address is 1600 Amphitheatre Parkway. Meet near the main entrance.",
        ).withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_OLDER_MILLIS),
        previewOutgoingMessage(
            messageId = "screen-direct-delivered",
            text = "Got it. I am leaving in five minutes.",
            status = Status.Outgoing.Delivered,
        ).withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_YESTERDAY_MILLIS),
        previewIncomingMessage(
            messageId = "screen-direct-long",
            text = PREVIEW_SCREEN_CONTENT_LONG_TEXT,
        ).withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_NOW_MILLIS),
        previewOutgoingMessage(
            messageId = "screen-direct-overflow",
            text = PREVIEW_SCREEN_CONTENT_OVERFLOW_TEXT,
            status = Status.Outgoing.Sending,
        ).withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_NOW_MILLIS),
    )
}

private fun previewScreenContentRichMessages(): ImmutableList<ConversationMessageUiModel> {
    return persistentListOf(
        previewIncomingMessage(
            messageId = "screen-group-start",
            text = "I started a group thread so everyone has the same context.",
        )
            .withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_OLDER_MILLIS)
            .withPreviewGrouping(canClusterWithNext = true),
        previewIncomingMessage(
            messageId = "screen-group-clustered",
            text = "Second incoming row in the same participant cluster.",
        )
            .withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_OLDER_MILLIS)
            .withPreviewGrouping(canClusterWithPrevious = true),
        previewOutgoingMessage(
            messageId = "screen-group-work-sim",
            text = "Replying from the work SIM so the thread shows the SIM annotation.",
            status = Status.Outgoing.Delivered,
        )
            .withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_YESTERDAY_MILLIS)
            .withPreviewSelfParticipant(selfParticipantId = "self-2"),
        previewIncomingMessage(
            messageId = "screen-group-attachments",
            text = "Photos, a file, a vCard, and the voice note are attached.",
            parts = persistentListOf(
                ConversationMessagePartUiModel.Text(
                    text = "Photos, a file, a vCard, and the voice note are attached.",
                ),
                previewImagePart(text = "Front entrance"),
                previewVideoPart(text = "Walkthrough clip"),
                previewAudioPart(text = "Voice note"),
                previewFilePart(text = "Briefing.pdf"),
                previewVCardPart(),
            ),
            protocol = ConversationMessageUiModel.Protocol.MMS,
            canSaveAttachments = true,
        )
            .copy(mmsSubject = "Site visit")
            .withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_NOW_MILLIS),
        previewOutgoingMessage(
            messageId = "screen-group-failed",
            text = "This outgoing message failed and can be resent.",
            status = Status.Outgoing.Failed,
        ).withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_NOW_MILLIS),
    )
}

private fun previewScreenContentDateSeparatedMessages(): ImmutableList<ConversationMessageUiModel> {
    return persistentListOf(
        previewOutgoingMessage(
            messageId = "screen-history-old-outgoing",
            text = "Older outgoing message before a date boundary.",
            status = Status.Outgoing.Complete,
        ).withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_OLDER_MILLIS),
        previewIncomingMessage(
            messageId = "screen-history-yesterday-incoming",
            text = "Yesterday's incoming message starts another separated group.",
        ).withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_YESTERDAY_MILLIS),
        previewIncomingMessage(
            messageId = "screen-history-today-incoming",
            text = "First message today with sender identity visible.",
        )
            .withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_NOW_MILLIS)
            .withPreviewGrouping(canClusterWithNext = true),
        previewIncomingMessage(
            messageId = "screen-history-today-cluster",
            text = "Clustered continuation on the same day.",
        )
            .withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_NOW_MILLIS)
            .withPreviewGrouping(canClusterWithPrevious = true, canClusterWithNext = true),
        previewIncomingMessage(
            messageId = "screen-history-today-cluster-end",
            text = "Final clustered continuation row.",
        )
            .withPreviewDisplayTime(displayTimestamp = PREVIEW_SCREEN_CONTENT_NOW_MILLIS)
            .withPreviewGrouping(canClusterWithPrevious = true),
    )
}

private fun previewScreenContentMmsDownloadMessages(): ImmutableList<ConversationMessageUiModel> {
    return persistentListOf(
        previewMmsDownloadMessage(
            messageId = "screen-mms-awaiting",
            status = Status.Incoming.YetToManualDownload,
            state = MmsDownloadUiModel.State.AwaitingManualDownload,
            canDownloadMessage = true,
            displayTimestamp = PREVIEW_SCREEN_CONTENT_OLDER_MILLIS,
        ),
        previewMmsDownloadMessage(
            messageId = "screen-mms-manual-downloading",
            status = Status.Incoming.ManualDownloading,
            state = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
            displayTimestamp = PREVIEW_SCREEN_CONTENT_YESTERDAY_MILLIS,
        ),
        previewMmsDownloadMessage(
            messageId = "screen-mms-auto-downloading",
            status = Status.Incoming.AutoDownloading,
            state = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
            displayTimestamp = PREVIEW_SCREEN_CONTENT_YESTERDAY_MILLIS,
        ),
        previewMmsDownloadMessage(
            messageId = "screen-mms-failed",
            status = Status.Incoming.DownloadFailed,
            state = MmsDownloadUiModel.State.DownloadFailed,
            canDownloadMessage = true,
            displayTimestamp = PREVIEW_SCREEN_CONTENT_NOW_MILLIS,
        ),
        previewMmsDownloadMessage(
            messageId = "screen-mms-expired",
            status = Status.Incoming.ExpiredOrNotAvailable,
            state = MmsDownloadUiModel.State.ExpiredOrUnavailable,
            canDownloadMessage = false,
            displayTimestamp = PREVIEW_SCREEN_CONTENT_NOW_MILLIS,
        ),
    )
}

private fun previewMmsDownloadMessage(
    messageId: String,
    status: Status.Incoming,
    state: MmsDownloadUiModel.State,
    canDownloadMessage: Boolean,
    displayTimestamp: Long,
): ConversationMessageUiModel {
    return previewIncomingMessage(
        messageId = messageId,
        text = null,
        status = status,
        parts = persistentListOf(),
        mmsDownload = previewMmsDownloadUiModel(state = state),
        protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
        canDownloadMessage = canDownloadMessage,
    ).withPreviewDisplayTime(displayTimestamp = displayTimestamp)
}

private fun ConversationMessageUiModel.withPreviewDisplayTime(
    displayTimestamp: Long,
): ConversationMessageUiModel {
    return copy(
        sentTimestamp = displayTimestamp,
        receivedTimestamp = displayTimestamp,
        displayTimestamp = displayTimestamp,
    )
}

private fun ConversationMessageUiModel.withPreviewGrouping(
    canClusterWithPrevious: Boolean = false,
    canClusterWithNext: Boolean = false,
): ConversationMessageUiModel {
    return copy(
        canClusterWithPrevious = canClusterWithPrevious,
        canClusterWithNext = canClusterWithNext,
    )
}

private fun ConversationMessageUiModel.withPreviewSelfParticipant(
    selfParticipantId: String,
): ConversationMessageUiModel {
    return copy(
        senderParticipantId = selfParticipantId,
        selfParticipantId = selfParticipantId,
    )
}
