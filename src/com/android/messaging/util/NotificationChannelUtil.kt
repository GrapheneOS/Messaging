package com.android.messaging.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
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
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                ALERTS_CHANNEL,
                context.getString(R.string.alerts_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    /**
     * Creates a notification channel with the user's old preferences.
     * @param conversationId The id of the conversation channel.
     * @param conversationTitle The title of the conversation channel.
     * @param legacyNotificationsEnabled Whether notifications are enabled in the channel. Pulled
     * from the old notifications system.
     * @param legacyRingtoneString The [Uri] of the ringtone to use for notifications. Pulled from the
     * old notifications system.
     * @param legacyVibrationEnabled Whether vibration is enabled in the channel. Pulled from the
     * old notifications system.
     */
    fun createConversationChannel(
        conversationId: String,
        conversationTitle: String,
        legacyNotificationsEnabled: Boolean = true,
        legacyRingtoneString: String? = null,
        legacyVibrationEnabled: Boolean = false
    ): NotificationChannel {
        val notificationManager = getNotificationManager()
        val defaultNotificationChannel =
            notificationManager.getNotificationChannel(INCOMING_MESSAGES)
        val existingChannel = getConversationChannel(conversationId)
        val channel = existingChannel
            ?: NotificationChannel(
                conversationId,
                conversationTitle,
                if (legacyNotificationsEnabled) {
                    defaultNotificationChannel.importance
                } else {
                    NotificationManager.IMPORTANCE_NONE
                }
            )
        val ringtoneUri =
            RingtoneUtil.getNotificationRingtoneUri(conversationId, legacyRingtoneString)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        channel.setSound(ringtoneUri, audioAttributes)
        channel.enableVibration(
            if (legacyVibrationEnabled) {
                // Only return false if there is no existing channel
                existingChannel?.shouldVibrate() == true
            } else {
                defaultNotificationChannel.shouldVibrate()
            }
        )
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

    /**
     * Deletes a notification channel.
     * @param id The id of the channel to delete.
     * @return True if the channel was deleted successfully, false otherwise.
     */
    fun deleteChannel(id: String) {
        val notificationManager = getNotificationManager()
        ShortcutManagerCompat.removeDynamicShortcuts(
            Factory.get().getApplicationContext(),
            listOf(id)
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
