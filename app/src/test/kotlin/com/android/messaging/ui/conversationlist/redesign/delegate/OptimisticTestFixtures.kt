package com.android.messaging.ui.conversationlist.redesign.delegate

import com.android.messaging.data.conversationlist.model.ConversationListDraft
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListLatestMessage
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListNotification
import com.android.messaging.data.conversationlist.model.ConversationListParticipant
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

internal fun snapshot(vararg conversationIds: String): ConversationListSnapshot {
    return snapshot(*conversationIds.map { conversationItem(it) }.toTypedArray())
}

internal fun snapshot(vararg items: ConversationListItem): ConversationListSnapshot {
    return ConversationListSnapshot(
        items = items.toList().toImmutableList(),
        blockedDestinations = persistentSetOf(),
        hasFirstSyncCompleted = true,
    )
}

internal fun conversationItem(
    conversationId: String,
    isPinned: Boolean = false,
    isRead: Boolean = true,
    timestamp: Long = 1_000L,
): ConversationListItem {
    return ConversationListItem(
        conversationId = conversationId,
        title = "Title $conversationId",
        icon = null,
        subject = null,
        isArchived = false,
        isPinned = isPinned,
        participant = ConversationListParticipant(
            contactId = -1L,
            lookupKey = null,
            otherNormalizedDestination = "+1555000$conversationId",
            selfId = null,
            count = 1,
            isGroup = false,
            includeEmailAddress = false,
            isEnterprise = false,
        ),
        latestMessage = ConversationListLatestMessage(
            isRead = isRead,
            timestamp = timestamp,
            snippetText = "Snippet $conversationId",
            previewUri = null,
            previewContentType = null,
            status = ConversationListMessageStatus.Normal,
            isIncoming = true,
            senderName = null,
        ),
        draft = ConversationListDraft(
            isVisible = false,
            snippetText = null,
            previewUri = null,
            previewContentType = null,
            subject = null,
        ),
        notification = ConversationListNotification(isEnabled = true),
    )
}
