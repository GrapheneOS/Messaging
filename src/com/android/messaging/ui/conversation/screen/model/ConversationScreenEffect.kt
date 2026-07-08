package com.android.messaging.ui.conversation.screen.model

import android.content.Intent
import android.net.Uri
import com.android.messaging.datamodel.data.MessageData

internal sealed interface ConversationScreenEffect {
    data object CloseConversation : ConversationScreenEffect

    data class RequestDefaultSmsRole(
        val isSending: Boolean,
    ) : ConversationScreenEffect

    data class LaunchAddContactFlow(
        val destination: String,
    ) : ConversationScreenEffect

    data class LaunchDefaultSmsRoleRequest(
        val intent: Intent,
    ) : ConversationScreenEffect

    data class LaunchForwardMessage(
        val message: MessageData,
    ) : ConversationScreenEffect

    data object NotifyDraftSent : ConversationScreenEffect

    data class OpenAttachmentPreview(
        val contentType: String,
        val contentUri: String,
        val imageCollectionUri: String?,
        val initialPhotoOccurrenceIndex: Int = 0,
    ) : ConversationScreenEffect

    data class OpenExternalUri(
        val uri: String,
    ) : ConversationScreenEffect

    data class PlacePhoneCall(
        val phoneNumber: String,
    ) : ConversationScreenEffect

    data class ShowSaveAttachmentsResult(
        val imageCount: Int,
        val videoCount: Int,
        val otherCount: Int,
        val failCount: Int,
    ) : ConversationScreenEffect

    data class ShareMessage(
        val attachmentContentType: String?,
        val attachmentContentUri: String?,
        val text: String?,
    ) : ConversationScreenEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : ConversationScreenEffect

    data class ShowOrAddParticipantContact(
        val contactId: Long,
        val contactLookupKey: String?,
        val avatarUri: Uri?,
        val normalizedDestination: String?,
    ) : ConversationScreenEffect

    data class NavigateToMessageDetails(
        val messageId: String,
    ) : ConversationScreenEffect
}
