package com.android.messaging.data.participantdestination.model

import com.android.messaging.data.conversation.model.ParticipantId

internal data class ParticipantTwin(
    val participantId: ParticipantId,
    val isBlocked: Boolean,
)
