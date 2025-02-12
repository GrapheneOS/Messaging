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

    private fun getNotificationManager(): NotificationManager {
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
     * @param notificationsEnabled Whether notifications are enabled in the channel.
     * @param ringtoneUri The [Uri] of the ringtone to use for notifications.
     * @param vibrationEnabled Whether vibration is enabled in the channel.
     */
    fun createConversationChannel(
        conversationId: String,
        conversationTitle: String,
        notificationsEnabled: Boolean,
        ringtoneUri: Uri?,
        vibrationEnabled: Boolean
    ) {
        val notificationManager = getNotificationManager()
        val channel = NotificationChannel(
            conversationId,
            conversationTitle,
            if (notificationsEnabled) {
                // Ensure that notifications create a banner by default
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_NONE
            }
        )
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        channel.setSound(ringtoneUri, audioAttributes)
        channel.enableVibration(vibrationEnabled)
        channel.setConversationId(INCOMING_MESSAGES, conversationId)
        notificationManager.createNotificationChannel(channel)
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
