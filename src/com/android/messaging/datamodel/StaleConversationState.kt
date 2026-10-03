@file:JvmName("StaleConversationState")

package com.android.messaging.datamodel

import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ShortcutManager
import androidx.core.content.edit
import com.android.messaging.data.conversationsettings.repository.ConversationNotificationRepositoryImpl
import com.android.messaging.data.subscription.repository.ConversationSimSelectionRepositoryImpl
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BugleWidgetPrefs
import com.android.messaging.util.LogUtil
import com.android.messaging.widget.WidgetConversationProvider

private const val SHORTCUT_MATCH_FLAGS = ShortcutManager.FLAG_MATCH_DYNAMIC or
    ShortcutManager.FLAG_MATCH_CACHED or
    ShortcutManager.FLAG_MATCH_PINNED

private val CONVERSATION_PREF_KEY_PREFIXES = listOf(
    ConversationNotificationRepositoryImpl.SNOOZE_KEY_PREFIX,
    ConversationSimSelectionRepositoryImpl.PREF_KEY_PREFIX,
)

/**
 * Clears what a new database leaves behind keyed by the conversation ids of the one before it:
 * a restore brings this back without the database, and a rebuild doesn't touch it. Seeded
 * conversation ids already keep it from reaching new conversations, this takes it out of the
 * user's way. Each step is best effort, as a failure here must not fail creating the database.
 */
internal fun clearStaleConversationState(context: Context) {
    bestEffort(step = "conversation prefs") { clearConversationPrefs(context) }

    bestEffort(step = "widgets") { resetWidgets(context) }
    bestEffort(step = "notifications") { clearConversationNotifications(context) }
    bestEffort(step = "shortcuts") { removeConversationShortcuts(context) }
}

private fun clearConversationPrefs(context: Context) {
    val preferences = context.getSharedPreferences(
        BuglePrefs.SHARED_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    preferences.edit {
        preferences
            .all
            .keys
            .filter { key ->
                CONVERSATION_PREF_KEY_PREFIXES.any(key::startsWith)
            }
            .forEach(::remove)
    }
}

private fun resetWidgets(context: Context) {
    context
        .getSharedPreferences(
            BugleWidgetPrefs.SHARED_PREFERENCES_WIDGET_NAME,
            Context.MODE_PRIVATE,
        )
        .edit {
            clear()
        }

    val provider = ComponentName(context, WidgetConversationProvider::class.java)

    AppWidgetManager.getInstance(context)?.getAppWidgetIds(provider)?.forEach { appWidgetId ->
        WidgetConversationProvider.rebuildWidget(context, appWidgetId)
    }
}

private fun clearConversationNotifications(context: Context) {
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager.cancelAll()
    notificationManager.notificationChannels
        .filter { it.conversationId != null }
        .forEach { notificationManager.deleteNotificationChannel(it.id) }
}

private fun removeConversationShortcuts(context: Context) {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    val shortcutIds = shortcutManager.getShortcuts(SHORTCUT_MATCH_FLAGS)
        .filterNot { it.isImmutable }
        .map { it.id }

    shortcutManager.removeLongLivedShortcuts(shortcutIds)

    // Pinned shortcuts can't be removed, only disabled
    shortcutManager.disableShortcuts(shortcutIds)
}

private inline fun bestEffort(step: String, block: () -> Unit) {
    runCatching(block).onFailure {
        LogUtil.w(LogUtil.BUGLE_TAG, "Could not clear stale conversation $step", it)
    }
}
