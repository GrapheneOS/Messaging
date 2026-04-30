package com.android.messaging.ui.conversation.v2.audio

import java.util.Locale

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

internal fun formatConversationAudioDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / MILLIS_PER_SECOND
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE

    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        minutes,
        seconds,
    )
}
