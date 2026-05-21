package com.android.messaging.data.conversationsettings.repository

import android.content.ContentResolver
import com.android.messaging.data.conversationsettings.model.LegacyConversationNotificationPrefs
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.util.BuglePrefs
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface ConversationNotificationRepository {
    suspend fun getLegacyNotificationPrefs(
        conversationId: String,
    ): LegacyConversationNotificationPrefs

    fun getSnoozeUntilMillis(conversationId: String): Long

    fun isSnoozed(conversationId: String): Boolean

    fun snooze(conversationId: String, option: SnoozeOption)

    fun clearSnooze(conversationId: String)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Provider {
        fun conversationNotificationRepository(): ConversationNotificationRepository
    }
}

internal class ConversationNotificationRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:MessagingDbDispatcher private val messagingDbDispatcher: CoroutineDispatcher,
) : ConversationNotificationRepository {

    override suspend fun getLegacyNotificationPrefs(
        conversationId: String,
    ): LegacyConversationNotificationPrefs {
        return withContext(messagingDbDispatcher) {
            contentResolver.query(
                MessagingContentProvider.buildConversationMetadataUri(conversationId),
                LEGACY_NOTIFICATION_PROJECTION,
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor == null || !cursor.moveToFirst()) {
                    LegacyConversationNotificationPrefs.Default
                } else {
                    LegacyConversationNotificationPrefs(
                        notificationsEnabled = cursor.getInt(INDEX_NOTIFICATION_ENABLED) == 1,
                        ringtoneString = cursor.getString(INDEX_NOTIFICATION_SOUND_URI),
                        vibrationEnabled = cursor.getInt(INDEX_NOTIFICATION_VIBRATION) == 1,
                    )
                }
            }
        }
    }

    override fun getSnoozeUntilMillis(conversationId: String): Long {
        val prefs = BuglePrefs.getApplicationPrefs()
        return prefs.getLong(snoozeKey(conversationId), SNOOZE_NOT_SET)
    }

    override fun isSnoozed(conversationId: String): Boolean {
        return getSnoozeUntilMillis(conversationId) > System.currentTimeMillis()
    }

    override fun snooze(conversationId: String, option: SnoozeOption) {
        val prefs = BuglePrefs.getApplicationPrefs()
        val untilMillis = when (option) {
            SnoozeOption.Always -> Long.MAX_VALUE
            else -> addSafely(System.currentTimeMillis(), option.duration.inWholeMilliseconds)
        }
        prefs.putLong(snoozeKey(conversationId), untilMillis)
    }

    override fun clearSnooze(conversationId: String) {
        val prefs = BuglePrefs.getApplicationPrefs()
        prefs.remove(snoozeKey(conversationId))
    }

    private fun snoozeKey(conversationId: String): String {
        return "$SNOOZE_KEY_PREFIX$conversationId"
    }

    private fun addSafely(
        base: Long,
        delta: Long,
    ): Long {
        val result = base + delta

        return if (result < base) {
            Long.MAX_VALUE
        } else {
            result
        }
    }

    private companion object {
        const val SNOOZE_KEY_PREFIX = "conversation_snooze_until_"
        const val SNOOZE_NOT_SET = 0L

        val LEGACY_NOTIFICATION_PROJECTION = arrayOf(
            ConversationColumns.NOTIFICATION_ENABLED,
            ConversationColumns.NOTIFICATION_SOUND_URI,
            ConversationColumns.NOTIFICATION_VIBRATION,
        )

        const val INDEX_NOTIFICATION_ENABLED = 0
        const val INDEX_NOTIFICATION_SOUND_URI = 1
        const val INDEX_NOTIFICATION_VIBRATION = 2
    }
}
