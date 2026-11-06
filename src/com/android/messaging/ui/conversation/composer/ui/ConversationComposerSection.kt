package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.preview.previewComposerWithAttachments
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun ConversationComposerSection(
    modifier: Modifier = Modifier,
    uiState: ConversationComposerUiState,
    messageFieldFocusRequester: FocusRequester,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onMessageTextChange: (text: String, textRevision: Int) -> Unit,
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
        val satelliteNotice = uiState.satelliteNotice
        if (satelliteNotice != null) {
            ConversationSatelliteNoticeBar(notice = satelliteNotice)
        }

        ConversationAttachmentPreview(
            attachments = uiState.attachments,
            onPendingAttachmentRemove = onPendingAttachmentRemove,
            onResolvedAttachmentClick = onResolvedAttachmentClick,
            onResolvedAttachmentRemove = onResolvedAttachmentRemove,
        )

        ConversationComposeBar(
            uiState = uiState,
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
    MessagingPreviewTheme {
        ConversationComposerSection(
            uiState = previewComposerWithAttachments(),
            messageFieldFocusRequester = FocusRequester(),
            onContactAttachClick = {},
            onMediaPickerClick = {},
            onMessageTextChange = { _, _ -> },
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
