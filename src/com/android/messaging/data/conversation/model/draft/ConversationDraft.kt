package com.android.messaging.data.conversation.model.draft

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.ParticipantId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationDraft(
    val messageText: String = "",
    val subjectText: String = "",
    val selfParticipantId: ParticipantId = ParticipantId(""),
    val attachments: ImmutableList<ConversationDraftAttachment> = persistentListOf(),
    val isCheckingDraft: Boolean = false,
    val isSending: Boolean = false,
) {
    val hasContent: Boolean
        get() = messageText.isNotBlank() ||
            subjectText.isNotBlank() ||
            attachments.isNotEmpty()

    val isMms: Boolean
        get() = subjectText.isNotBlank() || attachments.isNotEmpty()
}
