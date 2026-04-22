package com.android.messaging.ui.conversation.v2.audio

import java.util.Locale

internal fun formatConversationAudioDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        minutes,
        seconds,
    )
}
