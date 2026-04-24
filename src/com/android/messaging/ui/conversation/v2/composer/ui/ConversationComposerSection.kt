package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ConversationComposerSection(
    modifier: Modifier = Modifier,
    audioRecording: ConversationAudioRecordingUiState,
    attachments: ImmutableList<ComposerAttachmentUiModel>,
    messageText: String,
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
        )
    }
}
