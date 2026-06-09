package com.android.messaging.data.conversationlist.model

internal data class ConversationListItem(
    val conversationId: String,
    val title: String?,
    val icon: String?,
    val subject: String?,
    val isArchived: Boolean,
    val participant: ConversationListParticipant,
    val latestMessage: ConversationListLatestMessage,
    val draft: ConversationListDraft,
    val notification: ConversationListNotification,
)

internal data class ConversationListParticipant(
    val contactId: Long,
    val lookupKey: String?,
    val otherNormalizedDestination: String?,
    val selfId: String?,
    val count: Int,
    val isGroup: Boolean,
    val includeEmailAddress: Boolean,
    val isEnterprise: Boolean,
)

internal data class ConversationListLatestMessage(
    val isRead: Boolean,
    val timestamp: Long,
    val snippetText: String?,
    val previewUri: String?,
    val previewContentType: String?,
    val status: Int,
    val rawTelephonyStatus: Int,
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
)
