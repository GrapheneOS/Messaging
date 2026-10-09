package com.android.messaging.data.conversationstate.model

internal data class MirroredConversationState(
    val blockedDestinations: Set<String>,
    val archivedThreadIds: Set<String>,
    val mirroredAt: Long,
    val databaseVersion: Int,
) {
    val isEmpty: Boolean
        get() {
            return blockedDestinations.isEmpty() && archivedThreadIds.isEmpty()
        }
}
