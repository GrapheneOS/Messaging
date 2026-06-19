package com.android.messaging.ui.conversationlist.redesign.ui

import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAvatarUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListPreviewUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListSnippetUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal const val PREVIEW_TIMESTAMP_MILLIS = 1_806_240_000_000L

internal fun previewConversationListItem(
    conversationId: String,
    title: String,
    snippetText: String?,
    status: ConversationListMessageStatus = ConversationListMessageStatus.Normal,
    preview: ConversationListPreviewUiModel? = null,
    subject: String? = null,
    isOutgoing: Boolean = false,
    isUnread: Boolean = false,
    isGroup: Boolean = false,
    isEnterprise: Boolean = false,
    isMuted: Boolean = false,
    isDraft: Boolean = false,
    isSelected: Boolean = false,
): ConversationListItemUiModel {
    val normalizedDestination = "+3161234$conversationId".takeUnless { isGroup }

    return ConversationListItemUiModel(
        conversationId = conversationId,
        title = title,
        avatar = ConversationListAvatarUiModel(
            uri = null,
            contactId = -1L,
            lookupKey = null,
            normalizedDestination = normalizedDestination,
            isGroup = isGroup,
            details = normalizedDestination,
            canCall = !isGroup,
            canShowContact = !isGroup,
            isContactSaved = false,
        ),
        snippet = ConversationListSnippetUiModel(
            text = snippetText,
            senderName = null,
            preview = preview,
            isDraft = isDraft,
        ),
        subject = subject,
        timestampMillis = PREVIEW_TIMESTAMP_MILLIS,
        status = status,
        isOutgoing = isOutgoing,
        isUnread = isUnread,
        isGroup = isGroup,
        isEnterprise = isEnterprise,
        isMuted = isMuted,
        isArchived = false,
        isSelected = isSelected,
    )
}

internal fun previewConversationListItems(): ImmutableList<ConversationListItemUiModel> {
    return persistentListOf(
        previewConversationListItem(
            conversationId = "1",
            title = "Jane Doe",
            snippetText = "Are we still on for tomorrow?",
            isUnread = true,
        ),
        previewConversationListItem(
            conversationId = "2",
            title = "Ada Lovelace",
            snippetText = "Sounds good, thanks!",
        ),
        previewConversationListItem(
            conversationId = "3",
            title = "Grace Hopper",
            snippetText = "I was thinking that we could",
            status = ConversationListMessageStatus.Draft,
            isDraft = true,
        ),
        previewConversationListItem(
            conversationId = "4",
            title = "Weekend plans",
            snippetText = "Jane: I can bring snacks",
            isGroup = true,
            isMuted = true,
        ),
        previewConversationListItem(
            conversationId = "5",
            title = "Marina Silva",
            snippetText = "Did you get my last message?",
            status = ConversationListMessageStatus.Failed(rawTelephonyStatus = 0),
            isOutgoing = true,
        ),
    )
}
