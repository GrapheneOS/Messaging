package com.android.messaging.ui.conversation.audio

import com.android.messaging.ui.common.components.attachment.formatAudioDuration
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConversationAudioDurationFormatterTest {

    @Test
    fun formatAudioDuration_truncatesSubSecondDurations() {
        assertEquals("00:00", formatAudioDuration(durationMillis = 0L))
        assertEquals("00:00", formatAudioDuration(durationMillis = 999L))
    }

    @Test
    fun formatAudioDuration_formatsMinutesAndSeconds() {
        assertEquals("00:01", formatAudioDuration(durationMillis = 1_000L))
        assertEquals("01:01", formatAudioDuration(durationMillis = 61_000L))
        assertEquals("59:59", formatAudioDuration(durationMillis = 3_599_000L))
        assertEquals("60:00", formatAudioDuration(durationMillis = 3_600_000L))
    }
}
