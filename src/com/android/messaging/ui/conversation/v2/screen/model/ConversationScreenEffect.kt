package com.android.messaging.ui.conversation.v2.screen.model

import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData

internal sealed interface ConversationScreenEffect {
    data class LaunchForwardMessage(
        val message: MessageData,
    ) : ConversationScreenEffect

    data class OpenAttachmentPreview(
        val contentType: String,
        val contentUri: String,
        val imageCollectionUri: String?,
    ) : ConversationScreenEffect

    data class OpenExternalUri(
        val uri: String,
    ) : ConversationScreenEffect

    data class ShareMessage(
        val attachmentContentType: String?,
        val attachmentContentUri: String?,
        val text: String?,
    ) : ConversationScreenEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : ConversationScreenEffect

    data class ShowMessageDetails(
        val message: ConversationMessageData,
        val participants: ConversationParticipantsData,
        val selfParticipant: ParticipantData?,
    ) : ConversationScreenEffect
}
