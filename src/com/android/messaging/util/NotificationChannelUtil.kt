package com.android.messaging.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.core.content.pm.ShortcutManagerCompat
import com.android.messaging.Factory
import com.android.messaging.R

object NotificationChannelUtil {
    const val INCOMING_MESSAGES = "Conversations"
    const val ALERTS_CHANNEL = "Alerts"

    fun getNotificationManager(): NotificationManager {
        val context = Factory.get().applicationContext
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun onCreate(context: Context) {
        val notificationManager = getNotificationManager()
        notificationManager.createNotificationChannel(
            NotificationChannel(
                INCOMING_MESSAGES,
                context.getString(R.string.incoming_messages_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                ALERTS_CHANNEL,
                context.getString(R.string.alerts_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    // Migrate this and related functions to a use-case when BugleNotifications refactored
    fun createConversationChannelForRuntime(
        conversationId: String,
        conversationTitle: String,
    ): NotificationChannel {
        val notificationManager = getNotificationManager()
        val existingChannel = getConversationChannel(conversationId)
        if (existingChannel != null) {
            return existingChannel
        }

        val parentChannel = getOrCreateIncomingMessagesChannel(notificationManager)
        val channel = NotificationChannel(
            conversationId,
            conversationTitle,
            parentChannel.importance,
        )
        channel.setSound(parentChannel.sound, parentChannel.audioAttributes)
        channel.enableVibration(parentChannel.shouldVibrate())
        channel.setConversationId(INCOMING_MESSAGES, conversationId)
        notificationManager.createNotificationChannel(channel)
        return channel
    }

    fun createConversationChannelFromLegacy(
        conversationId: String,
        conversationTitle: String,
        legacyNotificationsEnabled: Boolean,
        legacyRingtoneString: String? = null,
        legacyVibrationEnabled: Boolean,
    ): NotificationChannel {
        val notificationManager = getNotificationManager()
        val existingChannel = getConversationChannel(conversationId)
        if (existingChannel != null) {
            return existingChannel
        }

        val parentChannel = getOrCreateIncomingMessagesChannel(notificationManager)
        val channel = NotificationChannel(
            conversationId,
            conversationTitle,
            if (legacyNotificationsEnabled) {
                parentChannel.importance
            } else {
                NotificationManager.IMPORTANCE_NONE
            },
        )
        val ringtoneUri =
            RingtoneUtil.getNotificationRingtoneUri(conversationId, legacyRingtoneString)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        channel.setSound(ringtoneUri, audioAttributes)
        channel.enableVibration(legacyVibrationEnabled)
        channel.setConversationId(INCOMING_MESSAGES, conversationId)
        notificationManager.createNotificationChannel(channel)
        return channel
    }

    /**
     * Retrieves a notification channel by its id.
     * @param conversationId The id of the channel to retrieve.
     * @return The notification channel with the given id, or null if it does not exist.
     */
    fun getConversationChannel(conversationId: String): NotificationChannel? {
        val notificationManager = getNotificationManager()
        val channel = notificationManager.getNotificationChannel(INCOMING_MESSAGES, conversationId)
        if (channel != null && channel.conversationId != null) {
            return channel
        }
        return null
    }

    private fun getOrCreateIncomingMessagesChannel(
        notificationManager: NotificationManager,
    ): NotificationChannel {
        val existingChannel = notificationManager.getNotificationChannel(INCOMING_MESSAGES)
        if (existingChannel != null) {
            return existingChannel
        }

        val context = Factory.get().applicationContext
        val channel = NotificationChannel(
            INCOMING_MESSAGES,
            context.getString(R.string.incoming_messages_channel),
            NotificationManager.IMPORTANCE_HIGH,
        )
        notificationManager.createNotificationChannel(channel)

        return notificationManager
            .getNotificationChannel(INCOMING_MESSAGES)
            ?: channel
    }

    /**
     * Deletes a notification channel.
     * @param id The id of the channel to delete.
     * @return True if the channel was deleted successfully, false otherwise.
     */
    fun deleteChannel(id: String) {
        val notificationManager = getNotificationManager()
        ShortcutManagerCompat.removeDynamicShortcuts(
            Factory.get().getApplicationContext(),
            listOf(id),
        )
        notificationManager.deleteNotificationChannel(id)
    }

    /**
     * Retrieves the active notification for a channel.
     * @param channelId The id of the channel to retrieve the active notification for.
     * @return The active notification for the channel, or null if it does not exist.
     */
    fun getActiveNotification(channelId: String): Notification? {
        val notificationManager = getNotificationManager()
        return notificationManager.getActiveNotifications().find {
            it.notification.channelId == channelId
        }?.notification
    }
}
