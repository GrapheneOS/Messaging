package com.android.messaging.data.conversationsettings.repository

import android.content.Context
import android.content.SharedPreferences
import com.android.messaging.data.conversationsettings.model.SNOOZE_NEVER_EXPIRES
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.util.BuglePrefs
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal interface ConversationNotificationRepository {

    fun observeSnoozeChanges(): Flow<Unit>

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
    @param:ApplicationContext private val context: Context,
) : ConversationNotificationRepository {

    override fun observeSnoozeChanges(): Flow<Unit> {
        return callbackFlow {
            val prefs = context.getSharedPreferences(
                BuglePrefs.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == null || key.startsWith(SNOOZE_KEY_PREFIX)) {
                    trySend(Unit)
                }
            }

            prefs.registerOnSharedPreferenceChangeListener(listener)

            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
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
            SnoozeOption.Always -> SNOOZE_NEVER_EXPIRES
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
            SNOOZE_NEVER_EXPIRES
        } else {
            result
        }
    }

    private companion object {
        const val SNOOZE_KEY_PREFIX = "conversation_snooze_until_"
        const val SNOOZE_NOT_SET = 0L
    }
}
