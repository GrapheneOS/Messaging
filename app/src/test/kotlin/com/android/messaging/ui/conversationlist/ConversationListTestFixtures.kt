package com.android.messaging.ui.conversationlist

import com.android.messaging.data.conversationlist.model.ConversationListDraft
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListLatestMessage
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListNotification
import com.android.messaging.data.conversationlist.model.ConversationListParticipant
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationsettings.model.SNOOZE_NEVER_EXPIRES
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

internal fun snapshotOfIds(vararg conversationIds: String): ConversationListSnapshot {
    return snapshotOfItems(*conversationIds.map(::conversationItem).toTypedArray())
}

internal fun snapshotOf(vararg items: ConversationListItem): ConversationListSnapshot {
    return snapshotOfItems(*items)
}

internal fun snapshotOfItems(vararg items: ConversationListItem): ConversationListSnapshot {
    return ConversationListSnapshot(
        items = persistentListOf(*items),
        blockedDestinations = persistentSetOf(),
        hasFirstSyncCompleted = true,
    )
}

internal fun conversationItem(
    conversationId: String,
    isArchived: Boolean = false,
    isPinned: Boolean = false,
    isSnoozed: Boolean = false,
    isRead: Boolean = true,
    timestamp: Long = 1_000L,
    senderName: String? = null,
    contactId: Long = -1L,
    lookupKey: String? = null,
    isDraftVisible: Boolean = false,
    draftSnippet: String? = null,
    draftSubject: String? = null,
): ConversationListItem {
    return ConversationListItem(
        conversationId = conversationId,
        title = "Title $conversationId",
        icon = null,
        subject = null,
        isArchived = isArchived,
        isPinned = isPinned,
        participant = ConversationListParticipant(
            contactId = contactId,
            lookupKey = lookupKey,
            otherNormalizedDestination = "+1555000$conversationId",
            isGroup = false,
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
            senderName = senderName,
        ),
        draft = ConversationListDraft(
            isVisible = isDraftVisible,
            snippetText = draftSnippet,
            previewUri = null,
            previewContentType = null,
            subject = draftSubject,
        ),
        notification = ConversationListNotification(
            isEnabled = true,
            snoozedUntilMillis = when {
                isSnoozed -> SNOOZE_NEVER_EXPIRES
                else -> ConversationListNotification.SNOOZE_NOT_SET
            },
        ),
    )
}
