package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationSegmentCounterUiState
import com.android.messaging.ui.conversation.preview.previewComposerWithAttachments
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ConversationComposerSection(
    modifier: Modifier = Modifier,
    audioRecording: ConversationAudioRecordingUiState,
    attachments: ImmutableList<ComposerAttachmentUiModel>,
    messageText: String,
    subjectText: String,
    sendProtocol: ConversationDraftSendProtocol,
    segmentCounter: ConversationSegmentCounterUiState?,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isRecordActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    shouldShowRecordAction: Boolean,
    messageFieldFocusRequester: FocusRequester,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onPendingAttachmentRemove: (String) -> Unit,
    onResolvedAttachmentClick: (ComposerAttachmentUiModel.Resolved) -> Unit,
    onResolvedAttachmentRemove: (String) -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingFinish: () -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingCancel: () -> Unit,
    onSendClick: () -> Unit,
    onSendActionLongClick: () -> Unit,
    onSubjectChipClick: () -> Unit,
    onSubjectChipClear: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        ConversationAttachmentPreview(
            attachments = attachments,
            onPendingAttachmentRemove = onPendingAttachmentRemove,
            onResolvedAttachmentClick = onResolvedAttachmentClick,
            onResolvedAttachmentRemove = onResolvedAttachmentRemove,
        )

        ConversationComposeBar(
            audioRecording = audioRecording,
            messageText = messageText,
            subjectText = subjectText,
            sendProtocol = sendProtocol,
            segmentCounter = segmentCounter,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            isSendActionEnabled = isSendActionEnabled,
            shouldShowRecordAction = shouldShowRecordAction,
            messageFieldFocusRequester = messageFieldFocusRequester,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            onMessageTextChange = onMessageTextChange,
            onAudioRecordingStartRequest = onAudioRecordingStartRequest,
            onAudioRecordingFinish = onAudioRecordingFinish,
            onAudioRecordingLock = onAudioRecordingLock,
            onAudioRecordingCancel = onAudioRecordingCancel,
            onSendClick = onSendClick,
            onSendActionLongClick = onSendActionLongClick,
            onSubjectChipClick = onSubjectChipClick,
            onSubjectChipClear = onSubjectChipClear,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationComposerSectionPreview() {
    val uiState = previewComposerWithAttachments()
    MessagingPreviewTheme {
        ConversationComposerSection(
            audioRecording = uiState.audioRecording,
            attachments = uiState.attachments,
            messageText = uiState.messageText,
            subjectText = uiState.subjectText,
            sendProtocol = uiState.sendProtocol,
            segmentCounter = uiState.segmentCounter,
            isMessageFieldEnabled = uiState.isMessageFieldEnabled,
            isAttachmentActionEnabled = uiState.isAttachmentActionEnabled,
            isRecordActionEnabled = uiState.isRecordActionEnabled,
            isSendActionEnabled = uiState.isSendEnabled,
            shouldShowRecordAction = uiState.shouldShowRecordAction,
            messageFieldFocusRequester = FocusRequester(),
            onContactAttachClick = {},
            onMediaPickerClick = {},
            onMessageTextChange = { _ -> },
            onPendingAttachmentRemove = { _ -> },
            onResolvedAttachmentClick = { _ -> },
            onResolvedAttachmentRemove = { _ -> },
            onAudioRecordingStartRequest = {},
            onLockedAudioRecordingStartRequest = {},
            onAudioRecordingFinish = {},
            onAudioRecordingLock = { true },
            onAudioRecordingCancel = {},
            onSendClick = {},
            onSendActionLongClick = {},
            onSubjectChipClick = {},
            onSubjectChipClear = {},
        )
    }
}
