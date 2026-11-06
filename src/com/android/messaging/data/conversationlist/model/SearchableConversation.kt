package com.android.messaging.data.conversationlist.model

import kotlinx.collections.immutable.ImmutableList

internal data class SearchableConversation(
    val item: ConversationListItem,
    val participants: ImmutableList<SearchableParticipant>,
)

internal data class SearchableParticipant(
    val fullName: String?,
    val firstName: String?,
    val displayDestination: String?,
    val normalizedDestination: String?,
)
