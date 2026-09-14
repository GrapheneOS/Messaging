package com.android.messaging.ui.subscription.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.android.messaging.data.conversation.model.ParticipantId

@Immutable
internal data class SimOptionUiModel(
    val id: ParticipantId,
    val label: String,
    val destination: String?,
    val slotLabel: String,
    val accentColor: Color?,
)
