package com.android.messaging.data.conversationlist.model

internal data class ConversationListItem(
    val conversationId: String,
    val title: String?,
    val icon: String?,
    val subject: String?,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val participant: ConversationListParticipant,
    val latestMessage: ConversationListLatestMessage,
    val draft: ConversationListDraft,
    val notification: ConversationListNotification,
)

internal data class ConversationListParticipant(
    val contactId: Long,
    val lookupKey: String?,
    val otherNormalizedDestination: String?,
    val isGroup: Boolean,
    val isEnterprise: Boolean,
)

internal data class ConversationListLatestMessage(
    val isRead: Boolean,
    val timestamp: Long,
    val snippetText: String?,
    val previewUri: String?,
    val previewContentType: String?,
    val status: ConversationListMessageStatus,
    val isIncoming: Boolean,
    val senderName: String?,
)

internal data class ConversationListDraft(
    val isVisible: Boolean,
    val snippetText: String?,
    val previewUri: String?,
    val previewContentType: String?,
    val subject: String?,
)

internal data class ConversationListNotification(
    val isEnabled: Boolean,
    val snoozedUntilMillis: Long = SNOOZE_NOT_SET,
) {
    val isSnoozed: Boolean
        get() = snoozedUntilMillis > System.currentTimeMillis()

    internal companion object {
        const val SNOOZE_NOT_SET = 0L
    }
}
