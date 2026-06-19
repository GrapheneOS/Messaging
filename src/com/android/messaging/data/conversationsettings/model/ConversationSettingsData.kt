package com.android.messaging.data.conversationsettings.model

import com.android.messaging.datamodel.data.ParticipantData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class ConversationSettingsData(
    val conversationId: String = "",
    val conversationTitle: String = "",
    val isArchived: Boolean = false,
    val isSnoozed: Boolean = false,
    val participants: ImmutableList<ParticipantData> = persistentListOf(),
    val dbSelfParticipantId: String = "",
)
