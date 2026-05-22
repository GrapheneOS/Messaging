package com.android.messaging.data.blockedparticipants.model

import com.android.messaging.datamodel.data.ParticipantData

internal data class BlockedDirectChat(
    val participant: ParticipantData,
    val conversationId: String,
)
