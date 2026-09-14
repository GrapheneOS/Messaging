package com.android.messaging.ui.conversation.screen.model

import android.content.Intent
import com.android.messaging.ui.contact.model.AddContactRequest

internal sealed interface ConversationScreenEffect {
    data class RequestDefaultSmsRole(
        val isSending: Boolean,
    ) : ConversationScreenEffect

    data class LaunchAddContactFlow(
        val destination: String,
    ) : ConversationScreenEffect

    data class LaunchDefaultSmsRoleRequest(
        val intent: Intent,
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

    data class ShowParticipantContactCard(
        val contactId: Long,
        val contactLookupKey: String,
    ) : ConversationScreenEffect

    data class AddParticipantContact(
        val request: AddContactRequest,
    ) : ConversationScreenEffect

    data class NavigateToVCardDetail(
        val uri: String,
    ) : ConversationScreenEffect
}
