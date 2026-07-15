package com.android.messaging.ui.conversation.messages.ui.attachment.rendering

import android.media.MediaPlayer
import com.android.common.test.helpers.targetContext
import com.android.messaging.ui.conversation.messages.ui.attachment.ConversationInlineAudioAttachmentPlaybackState
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

private const val STATE_AUDIO_CONTENT_URI = "content://mms/part/test-audio"
private const val STATE_MISSING_AUDIO_CONTENT_URI = "content://mms/part/missing-audio"
private const val STATE_AUDIO_DURATION_MILLIS = 18_000
private const val STATE_PAUSED_POSITION_MILLIS = 4_500

@RunWith(RobolectricTestRunner::class)
internal class ConversationInlineAudioAttachmentPlaybackStateTest {

    private val onPlaybackFailure = mockk<() -> Unit>(relaxed = true)
    private lateinit var shadowMediaPlayer: ShadowMediaPlayer

    @Before
    fun setUp() {
        ShadowMediaPlayer.resetStaticState()
        ShadowMediaPlayer.setCreateListener { _, shadow ->
            shadowMediaPlayer = shadow
            shadow.setInvalidStateBehavior(ShadowMediaPlayer.InvalidStateBehavior.ASSERT)
        }
    }

    @After
    fun tearDown() {
        ShadowMediaPlayer.resetStaticState()
    }

    @Test
    fun initialState_reportsZeroDurationAndProgress() {
        val playbackState = playbackState()

        assertEquals(0L, playbackState.durationMillis)
        assertEquals(0L, playbackState.positionMillis)
        assertEquals(0f, playbackState.progress)
        assertEquals("00:00", playbackState.durationLabel)
        assertFalse(playbackState.isPlaying)
        verify(exactly = 0) {
            onPlaybackFailure.invoke()
        }
    }

    @Test
    fun togglePlayback_beforePreparedQueuesStartUntilPrepared() {
        addAudioMediaInfo(preparationDelayMillis = -1)
        val playbackState = playbackState()

        playbackState.togglePlayback(
            context = targetContext,
            contentUri = STATE_AUDIO_CONTENT_URI,
        )

        assertFalse(playbackState.isPlaying)
        assertEquals(0L, playbackState.durationMillis)
        assertEquals(ShadowMediaPlayer.State.PREPARING, shadowMediaPlayer.getState())

        shadowMediaPlayer.invokePreparedListener()

        assertTrue(playbackState.isPlaying)
        assertEquals(STATE_AUDIO_DURATION_MILLIS.toLong(), playbackState.durationMillis)
        assertEquals(0L, playbackState.positionMillis)
        assertEquals("00:18", playbackState.durationLabel)
        assertEquals(ShadowMediaPlayer.State.STARTED, shadowMediaPlayer.getState())
        verify(exactly = 0) {
            onPlaybackFailure.invoke()
        }
    }

    @Test
    fun togglePlaybackTwiceBeforePrepared_cancelsQueuedStart() {
        addAudioMediaInfo(preparationDelayMillis = -1)
        val playbackState = playbackState()

        playbackState.togglePlayback(
            context = targetContext,
            contentUri = STATE_AUDIO_CONTENT_URI,
        )
        playbackState.togglePlayback(
            context = targetContext,
            contentUri = STATE_AUDIO_CONTENT_URI,
        )

        shadowMediaPlayer.invokePreparedListener()

        assertFalse(playbackState.isPlaying)
        assertEquals(STATE_AUDIO_DURATION_MILLIS.toLong(), playbackState.durationMillis)
        assertEquals(ShadowMediaPlayer.State.PREPARED, shadowMediaPlayer.getState())
        verify(exactly = 0) {
            onPlaybackFailure.invoke()
        }
    }

    @Test
    fun preparedPlayback_togglesPauseAndResume() {
        val playbackState = startedPlaybackState()

        shadowMediaPlayer.setCurrentPosition(STATE_PAUSED_POSITION_MILLIS)
        shadowMediaPlayer.doStop()
        playbackState.togglePlayback(
            context = targetContext,
            contentUri = STATE_AUDIO_CONTENT_URI,
        )

        assertFalse(playbackState.isPlaying)
        assertEquals(STATE_PAUSED_POSITION_MILLIS.toLong(), playbackState.positionMillis)
        assertEquals(0.25f, playbackState.progress)
        assertEquals(ShadowMediaPlayer.State.PAUSED, shadowMediaPlayer.getState())

        playbackState.togglePlayback(
            context = targetContext,
            contentUri = STATE_AUDIO_CONTENT_URI,
        )

        assertTrue(playbackState.isPlaying)
        assertEquals(ShadowMediaPlayer.State.STARTED, shadowMediaPlayer.getState())
        verify(exactly = 0) {
            onPlaybackFailure.invoke()
        }
    }

    @Test
    fun playbackCompletion_resetsPositionAndCanRestart() {
        val playbackState = startedPlaybackState()

        shadowMediaPlayer.invokeCompletionListener()

        assertFalse(playbackState.isPlaying)
        assertEquals(0L, playbackState.positionMillis)
        assertEquals(ShadowMediaPlayer.State.PLAYBACK_COMPLETED, shadowMediaPlayer.getState())

        playbackState.togglePlayback(
            context = targetContext,
            contentUri = STATE_AUDIO_CONTENT_URI,
        )

        assertTrue(playbackState.isPlaying)
        assertEquals(0L, playbackState.positionMillis)
        assertEquals(ShadowMediaPlayer.State.STARTED, shadowMediaPlayer.getState())
        verify(exactly = 0) {
            onPlaybackFailure.invoke()
        }
    }

    @Test
    fun updateProgress_readsCurrentPlaybackPosition() {
        val playbackState = startedPlaybackState()

        shadowMediaPlayer.setCurrentPosition(STATE_PAUSED_POSITION_MILLIS)
        shadowMediaPlayer.doStop()
        playbackState.updateProgress()

        assertEquals(STATE_PAUSED_POSITION_MILLIS.toLong(), playbackState.positionMillis)
        assertEquals(0.25f, playbackState.progress)
    }

    @Test
    fun release_clearsPlayingStateAndStopsReadingProgress() {
        val playbackState = startedPlaybackState()

        playbackState.release()
        playbackState.updateProgress()

        assertFalse(playbackState.isPlaying)
        assertEquals(0L, playbackState.positionMillis)
        assertEquals(ShadowMediaPlayer.State.END, shadowMediaPlayer.getState())
        verify(exactly = 0) {
            onPlaybackFailure.invoke()
        }
    }

    @Test
    fun mediaError_reportsFailureAndResetsPlaybackState() {
        val playbackState = startedPlaybackState()

        shadowMediaPlayer.invokeErrorListener(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)

        assertEquals(0L, playbackState.durationMillis)
        assertEquals(0L, playbackState.positionMillis)
        assertEquals(0f, playbackState.progress)
        assertFalse(playbackState.isPlaying)
        assertEquals(ShadowMediaPlayer.State.END, shadowMediaPlayer.getState())
        verify(exactly = 1) {
            onPlaybackFailure.invoke()
        }
    }

    @Test
    fun invalidUri_reportsFailureAndResetsPlaybackState() {
        ShadowMediaPlayer.addException(
            DataSource.toDataSource(STATE_MISSING_AUDIO_CONTENT_URI),
            IOException("missing audio"),
        )
        val playbackState = playbackState()

        playbackState.togglePlayback(
            context = targetContext,
            contentUri = STATE_MISSING_AUDIO_CONTENT_URI,
        )

        assertEquals(0L, playbackState.durationMillis)
        assertEquals(0L, playbackState.positionMillis)
        assertEquals(0f, playbackState.progress)
        assertFalse(playbackState.isPlaying)
        assertEquals(ShadowMediaPlayer.State.END, shadowMediaPlayer.getState())
        verify(exactly = 1) {
            onPlaybackFailure.invoke()
        }
    }

    private fun playbackState(): ConversationInlineAudioAttachmentPlaybackState {
        return ConversationInlineAudioAttachmentPlaybackState(
            onPlaybackFailure = onPlaybackFailure,
        )
    }

    private fun startedPlaybackState(): ConversationInlineAudioAttachmentPlaybackState {
        addAudioMediaInfo(preparationDelayMillis = -1)
        val playbackState = playbackState()

        playbackState.togglePlayback(
            context = targetContext,
            contentUri = STATE_AUDIO_CONTENT_URI,
        )
        shadowMediaPlayer.invokePreparedListener()

        assertTrue(playbackState.isPlaying)
        return playbackState
    }

    private fun addAudioMediaInfo(preparationDelayMillis: Int) {
        ShadowMediaPlayer.addMediaInfo(
            DataSource.toDataSource(STATE_AUDIO_CONTENT_URI),
            ShadowMediaPlayer.MediaInfo(STATE_AUDIO_DURATION_MILLIS, preparationDelayMillis),
        )
    }
}
