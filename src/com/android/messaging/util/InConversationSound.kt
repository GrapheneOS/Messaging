package com.android.messaging.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import com.android.messaging.Factory
import com.android.messaging.R
import com.android.messaging.data.conversationsettings.repository.ConversationSnoozeQuery
import com.android.messaging.datamodel.BugleNotifications
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InConversationSound internal constructor(
    private val dispatcher: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(
        SupervisorJob() + dispatcher + CoroutineExceptionHandler { _, throwable ->
            LogUtil.e(
                LogUtil.BUGLE_TAG,
                "InConversationSound: failed to play the chime",
                throwable,
            )
        },
    )

    private var playing: Ringtone? = null

    private var stopJob: Job? = null

    internal fun post(conversationId: String?) {
        if (conversationId.isNullOrEmpty()) {
            return
        }

        scope.launch(dispatcher) {
            play(conversationId)
        }
    }

    private fun play(conversationId: String) {
        if (!releasePreviousChime()) {
            return
        }

        val ringtone = prepare(conversationId) ?: return

        playing = ringtone

        stopJob = scope.launch(dispatcher) {
            delay(MAX_DURATION_MS.milliseconds)
            stopPlaying()
        }

        ringtone.play()
    }

    private fun releasePreviousChime(): Boolean {
        val current = playing
        if (current != null && current.isPlaying) {
            return false
        }

        stopJob?.cancel()
        stopPlaying()
        return true
    }

    private fun prepare(conversationId: String): Ringtone? {
        val context = Factory.get().applicationContext
        if (!isEnabled(context) || !isAudible(context, conversationId)) {
            return null
        }

        return RingtoneUtil.getNotificationRingtoneUri(conversationId, null)
            ?.let { RingtoneManager.getRingtone(context, it) }
            ?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                volume = VOLUME
                isLooping = false
            }
    }

    private fun stopPlaying() {
        val current = playing
        playing = null
        current?.stop()
    }

    private fun isEnabled(context: Context): Boolean {
        return BuglePrefs.getApplicationPrefs().getBoolean(
            context.getString(R.string.in_conversation_sound_pref_key),
            context.resources.getBoolean(R.bool.in_conversation_sound_pref_default),
        )
    }

    private fun isAudible(context: Context, conversationId: String): Boolean {
        val notificationManager = NotificationChannelUtil.getNotificationManager()
        val interruptionFilter = notificationManager.currentInterruptionFilter
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // A conversation gets its own channel only once a notification has been posted for it,
        // so fall back to the channel it would have inherited from.
        val channel = NotificationChannelUtil.getConversationChannel(conversationId)
            ?: notificationManager
                .getNotificationChannel(NotificationChannelUtil.INCOMING_MESSAGES)

        return notificationManager.areNotificationsEnabled() &&
            interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL &&
            audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
            // IMPORTANCE_NONE means blocked, below IMPORTANCE_DEFAULT means silent.
            (channel == null || channel.importance >= NotificationManager.IMPORTANCE_DEFAULT) &&
            !BugleNotifications.isConversationBlocked(conversationId) &&
            !ConversationSnoozeQuery.isConversationSnoozed(conversationId)
    }

    companion object {

        private const val VOLUME = 0.25f
        private const val MAX_DURATION_MS = 5_000L

        private val instance = InConversationSound(
            dispatcher = Dispatchers.IO.limitedParallelism(
                parallelism = 1,
                name = "InConversationSound",
            ),
        )

        /** Entry point for `BugleActionToasts`, which stays Java. */
        @JvmStatic
        fun playIfEnabled(conversationId: String?) {
            instance.post(conversationId)
        }
    }
}
