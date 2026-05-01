package com.android.messaging.ui.conversation.audio.model

import androidx.compose.runtime.Immutable

internal enum class ConversationAudioRecordingPhase {
    Idle,
    Recording,
    Finalizing,
}

@Immutable
internal data class ConversationAudioRecordingUiState(
    val phase: ConversationAudioRecordingPhase = ConversationAudioRecordingPhase.Idle,
    val durationMillis: Long = 0L,
    val isLocked: Boolean = false,
)
