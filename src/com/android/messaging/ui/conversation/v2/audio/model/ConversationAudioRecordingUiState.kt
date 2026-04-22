package com.android.messaging.ui.conversation.v2.audio.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationAudioRecordingUiState(
    val isRecording: Boolean = false,
    val durationMillis: Long = 0L,
)
