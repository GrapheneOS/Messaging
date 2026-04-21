package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.android.messaging.R
import com.android.messaging.util.UiUtils
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

private val audioProgressUpdateIntervalMs = 250L.milliseconds

@Composable
internal fun rememberConversationInlineAudioAttachmentPlaybackState(
    contentUri: String,
): ConversationInlineAudioAttachmentPlaybackState {
    val playbackState = remember(contentUri) {
        ConversationInlineAudioAttachmentPlaybackState(
            onPlaybackFailure = {
                UiUtils.showToastAtBottom(R.string.audio_recording_replay_failed)
            },
        )
    }

    DisposableEffect(contentUri) {
        onDispose {
            playbackState.release()
        }
    }

    LaunchedEffect(playbackState.isPlaying, contentUri) {
        while (playbackState.isPlaying) {
            playbackState.updateProgress()
            delay(audioProgressUpdateIntervalMs)
        }
    }

    return playbackState
}

@Stable
internal class ConversationInlineAudioAttachmentPlaybackState(
    private val onPlaybackFailure: () -> Unit,
) {
    var durationMillis by mutableLongStateOf(0L)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var positionMillis by mutableLongStateOf(0L)
        private set

    private var hasPlaybackCompleted by mutableStateOf(false)
    private var isPrepared by mutableStateOf(false)
    private var mediaPlayer by mutableStateOf<MediaPlayer?>(null)
    private var shouldStartPlaybackWhenPrepared by mutableStateOf(false)

    val durationLabel: String
        get() {
            return formatAudioDuration(
                durationMillis = when {
                    durationMillis > 0L -> durationMillis
                    else -> positionMillis
                },
                positionMillis = positionMillis,
            )
        }

    val progress: Float
        get() {
            return calculateAudioProgress(
                durationMillis = durationMillis,
                positionMillis = positionMillis,
            )
        }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
        isPlaying = false
        shouldStartPlaybackWhenPrepared = false
    }

    fun togglePlayback(
        context: Context,
        contentUri: String,
    ) {
        val currentMediaPlayer = mediaPlayer

        if (currentMediaPlayer == null) {
            shouldStartPlaybackWhenPrepared = true
            ensureMediaPlayer(
                context = context,
                contentUri = contentUri,
            )
            return
        }

        if (!isPrepared) {
            shouldStartPlaybackWhenPrepared = !shouldStartPlaybackWhenPrepared
            return
        }

        if (isPlaying) {
            currentMediaPlayer.pause()
            positionMillis = currentMediaPlayer.currentPosition.toLong().coerceAtLeast(0L)
            isPlaying = false
            return
        }

        startPlayback()
    }

    fun updateProgress() {
        val currentMediaPlayer = mediaPlayer ?: return
        positionMillis = currentMediaPlayer.currentPosition.toLong().coerceAtLeast(0L)
    }

    private fun ensureMediaPlayer(
        context: Context,
        contentUri: String,
    ) {
        if (mediaPlayer != null) {
            return
        }

        val createdMediaPlayer = MediaPlayer()
        mediaPlayer = createdMediaPlayer

        try {
            createdMediaPlayer.setDataSource(context, contentUri.toUri())
            createdMediaPlayer.setOnCompletionListener {
                isPlaying = false
                hasPlaybackCompleted = true
                positionMillis = 0L
            }
            createdMediaPlayer.setOnErrorListener { _, _, _ ->
                handlePlaybackFailure()
                true
            }
            createdMediaPlayer.setOnPreparedListener { preparedMediaPlayer ->
                isPrepared = true
                durationMillis = preparedMediaPlayer.duration.toLong().coerceAtLeast(0L)
                positionMillis = 0L
                if (shouldStartPlaybackWhenPrepared) {
                    shouldStartPlaybackWhenPrepared = false
                    startPlayback()
                }
            }
            createdMediaPlayer.prepareAsync()
        } catch (_: Exception) {
            handlePlaybackFailure()
        }
    }

    private fun handlePlaybackFailure() {
        onPlaybackFailure()
        release()
        durationMillis = 0L
        positionMillis = 0L
        hasPlaybackCompleted = false
    }

    private fun startPlayback() {
        val currentMediaPlayer = mediaPlayer ?: return

        if (hasPlaybackCompleted) {
            currentMediaPlayer.seekTo(0)
            positionMillis = 0L
            hasPlaybackCompleted = false
        }

        currentMediaPlayer.start()
        isPlaying = true
    }
}

private fun calculateAudioProgress(
    durationMillis: Long,
    positionMillis: Long,
): Float {
    return when {
        durationMillis <= 0L -> 0f
        else -> {
            (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
        }
    }
}

private fun formatAudioDuration(
    durationMillis: Long,
    positionMillis: Long,
): String {
    val displayedMillis = when {
        positionMillis > 0L -> positionMillis
        else -> durationMillis
    }
    val totalSeconds = displayedMillis / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
