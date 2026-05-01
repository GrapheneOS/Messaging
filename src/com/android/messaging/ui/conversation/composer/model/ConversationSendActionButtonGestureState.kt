package com.android.messaging.ui.conversation.composer.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationSendActionButtonGestureState(
    val cancelDragDistancePx: Float = 0f,
    val lockDragDistancePx: Float = 0f,
)
