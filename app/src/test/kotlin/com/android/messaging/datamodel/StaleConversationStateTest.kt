package com.android.messaging.datamodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BugleWidgetPrefs
import com.android.messaging.util.NotificationChannelUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * A restore brings back per-conversation state without the database it was keyed against, and a
 * rebuilt database counts its ids again. Whatever survives would attach itself to an unrelated
 * conversation, or at best clutter the system settings with conversations that no longer exist.
 */
@RunWith(RobolectricTestRunner::class)
class StaleConversationStateTest {

    private val context: Application = RuntimeEnvironment.getApplication()

    @Test
    fun clearsConversationPrefsAndKeepsTheRest() {
        val preferences = context.getSharedPreferences(
            BuglePrefs.SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        preferences.edit()
            .putLong("conversation_snooze_until_7", Long.MAX_VALUE)
            .putString("conversation_sim_selection_7", "3")
            .putBoolean("conversation_notification_channels_migrated_v1", true)
            .putBoolean("some_app_setting", true)
            .commit()

        clearStaleConversationState(context)

        assertEquals(
            setOf("conversation_notification_channels_migrated_v1", "some_app_setting"),
            preferences.all.keys,
        )
    }

    @Test
    fun clearsWidgetConversations() {
        val widgetPreferences = context.getSharedPreferences(
            BugleWidgetPrefs.SHARED_PREFERENCES_WIDGET_NAME,
            Context.MODE_PRIVATE,
        )
        widgetPreferences.edit().putString("conversation_id12", "7").commit()

        clearStaleConversationState(context)

        assertEquals(emptyMap<String, Any>(), widgetPreferences.all)
    }

    @Test
    fun deletesConversationChannelsAndKeepsTheAppChannels() {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        listOf(NotificationChannelUtil.INCOMING_MESSAGES, NotificationChannelUtil.ALERTS_CHANNEL)
            .forEach { id ->
                notificationManager.createNotificationChannel(
                    NotificationChannel(id, id, NotificationManager.IMPORTANCE_HIGH),
                )
            }
        val conversationChannel = NotificationChannel(
            "7",
            "Weekend plan",
            NotificationManager.IMPORTANCE_HIGH,
        )
        conversationChannel.setConversationId(NotificationChannelUtil.INCOMING_MESSAGES, "7")
        notificationManager.createNotificationChannel(conversationChannel)

        clearStaleConversationState(context)

        assertEquals(
            setOf(
                NotificationChannelUtil.INCOMING_MESSAGES,
                NotificationChannelUtil.ALERTS_CHANNEL,
            ),
            notificationManager.notificationChannels.map { it.id }.toSet(),
        )
    }

    /** It runs while the database is being created, so throwing here would fail that too. */
    @Test
    fun withoutSystemServices_stillClearsPrefsAndDoesNotThrow() {
        val preferences = context.getSharedPreferences(
            BuglePrefs.SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        preferences.edit().putLong("conversation_snooze_until_7", Long.MAX_VALUE).commit()
        val contextWithoutServices = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? {
                return null
            }
        }

        clearStaleConversationState(contextWithoutServices)

        assertFalse(preferences.contains("conversation_snooze_until_7"))
    }
}
