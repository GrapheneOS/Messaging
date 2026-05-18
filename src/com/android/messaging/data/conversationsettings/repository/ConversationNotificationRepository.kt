package com.android.messaging.data.conversationsettings.repository

import android.content.ContentResolver
import com.android.messaging.data.conversationsettings.model.LegacyConversationNotificationPrefs
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.PeopleOptionsItemData
import com.android.messaging.util.BuglePrefs
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

internal interface ConversationNotificationRepository {
    fun getLegacyNotificationPrefs(conversationId: String): LegacyConversationNotificationPrefs

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
) : ConversationNotificationRepository {

    override fun getLegacyNotificationPrefs(
        conversationId: String,
    ): LegacyConversationNotificationPrefs {
        val cursor = contentResolver.query(
            MessagingContentProvider.buildConversationMetadataUri(conversationId),
            PeopleOptionsItemData.PROJECTION,
            null,
            null,
            null,
        )

        return cursor.use {
            if (it == null || !it.moveToFirst()) {
                LegacyConversationNotificationPrefs.Default
            } else {
                LegacyConversationNotificationPrefs(
                    notificationsEnabled = it.getInt(
                        PeopleOptionsItemData.INDEX_NOTIFICATION_ENABLED,
                    ) == 1,
                    ringtoneString = it.getString(
                        PeopleOptionsItemData.INDEX_NOTIFICATION_SOUND_URI,
                    ),
                    vibrationEnabled = it.getInt(
                        PeopleOptionsItemData.INDEX_NOTIFICATION_VIBRATION,
                    ) == 1,
                )
            }
        }
    }

    override fun getSnoozeUntilMillis(conversationId: String): Long {
        val prefs = BuglePrefs.getApplicationPrefs()
        return prefs.getLong(snoozeKey(conversationId), SNOOZE_NOT_SET)
    }

    override fun isSnoozed(conversationId: String): Boolean {
        val until = getSnoozeUntilMillis(conversationId)
        return until == Long.MAX_VALUE || until > System.currentTimeMillis()
    }

    override fun snooze(conversationId: String, option: SnoozeOption) {
        val prefs = BuglePrefs.getApplicationPrefs()
        val untilMillis = when (option) {
            SnoozeOption.Always -> Long.MAX_VALUE
            else -> addSafely(System.currentTimeMillis(), option.durationMillis)
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
    }
}
