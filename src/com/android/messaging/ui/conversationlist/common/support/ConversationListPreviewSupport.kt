package com.android.messaging.ui.conversationlist.common.support

import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.ui.conversationlist.mapper.conversationListMmsDownloadTitleResId
import com.android.messaging.ui.conversationlist.model.ConversationListAvatarUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListPreviewUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListSnippetUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal const val PREVIEW_TIMESTAMP_MILLIS = 1_806_240_000_000L

internal fun previewConversationListItem(
    conversationId: String,
    title: String,
    snippetText: String?,
    senderName: String? = null,
    status: ConversationListMessageStatus = ConversationListMessageStatus.Normal,
    preview: ConversationListPreviewUiModel? = null,
    subject: String? = null,
    isOutgoing: Boolean = false,
    isUnread: Boolean = false,
    isGroup: Boolean = false,
    isEnterprise: Boolean = false,
    isMuted: Boolean = false,
    isSnoozed: Boolean = false,
    isPinned: Boolean = false,
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
            subtitle = normalizedDestination,
            canCall = !isGroup,
            canShowContact = !isGroup,
            isContactSaved = false,
        ),
        snippet = ConversationListSnippetUiModel(
            text = snippetText,
            senderName = senderName,
            preview = preview,
            isDraft = isDraft,
        ),
        subject = subject,
        timestampMillis = PREVIEW_TIMESTAMP_MILLIS,
        status = status,
        mmsDownloadTitleResId = conversationListMmsDownloadTitleResId(
            status = status,
            isSecondaryUser = false,
        ),
        isOutgoing = isOutgoing,
        isUnread = isUnread,
        isEnterprise = isEnterprise,
        isMuted = isMuted,
        isSnoozed = isSnoozed,
        isPinned = isPinned,
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
            snippetText = "I can bring snacks",
            senderName = "Jane",
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
