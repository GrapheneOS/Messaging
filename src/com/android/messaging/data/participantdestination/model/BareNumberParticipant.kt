package com.android.messaging.data.participantdestination.model

import com.android.messaging.data.conversation.model.ParticipantId

internal data class BareNumberParticipant(
    val participantId: ParticipantId,
    val destination: String,
    val isBlocked: Boolean,
)
