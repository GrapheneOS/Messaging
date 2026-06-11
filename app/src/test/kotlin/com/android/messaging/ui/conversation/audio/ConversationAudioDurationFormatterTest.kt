package com.android.messaging.ui.conversation.audio

import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConversationAudioDurationFormatterTest {

    @Test
    fun formatConversationAudioDuration_truncatesSubSecondDurations() {
        assertEquals("00:00", formatConversationAudioDuration(durationMillis = 0L))
        assertEquals("00:00", formatConversationAudioDuration(durationMillis = 999L))
    }

    @Test
    fun formatConversationAudioDuration_formatsMinutesAndSeconds() {
        assertEquals("00:01", formatConversationAudioDuration(durationMillis = 1_000L))
        assertEquals("01:01", formatConversationAudioDuration(durationMillis = 61_000L))
        assertEquals("59:59", formatConversationAudioDuration(durationMillis = 3_599_000L))
        assertEquals("60:00", formatConversationAudioDuration(durationMillis = 3_600_000L))
    }
}
