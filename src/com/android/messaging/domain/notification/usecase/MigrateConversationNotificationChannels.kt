package com.android.messaging.domain.notification.usecase

import android.database.Cursor
import android.database.SQLException
import androidx.core.database.getStringOrNull
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.LogUtil
import com.android.messaging.util.NotificationChannelUtil
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal interface MigrateConversationNotificationChannels {
    operator fun invoke(): Boolean
}

internal class MigrateConversationNotificationChannelsImpl @Inject constructor(
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : MigrateConversationNotificationChannels {

    override operator fun invoke(): Boolean {
        val prefs = BuglePrefs.getApplicationPrefs()
        if (isMigrationComplete(prefs = prefs)) {
            return false
        }

        return runBlocking {
            migrateIfNeeded(prefs = prefs)
        }
    }

    private suspend fun migrateIfNeeded(prefs: BuglePrefs): Boolean {
        return withContext(context = messagingDbDispatcher) {
            val database = DataModel.get().database

            // Double-check locked
            if (isMigrationComplete(prefs = prefs)) {
                return@withContext false
            }

            try {
                val migratedCount = migrateLegacyConversationChannels(database = database)
                prefs.putBoolean(BuglePrefsKeys.CONVERSATION_NOTIFICATION_CHANNELS_MIGRATED, true)
                LogUtil.i(TAG, "Migrated $migratedCount legacy conversation notification channels")

                true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SQLException) {
                logMigrationFailure(exception = exception)
            } catch (exception: IllegalArgumentException) {
                logMigrationFailure(exception = exception)
            } catch (exception: IllegalStateException) {
                logMigrationFailure(exception = exception)
            } catch (exception: SecurityException) {
                logMigrationFailure(exception = exception)
            }
        }
    }

    private fun isMigrationComplete(prefs: BuglePrefs): Boolean {
        return prefs.getBoolean(
            BuglePrefsKeys.CONVERSATION_NOTIFICATION_CHANNELS_MIGRATED,
            false,
        )
    }

    private fun logMigrationFailure(exception: Exception): Boolean {
        LogUtil.e(
            TAG,
            "Failed to migrate legacy conversation notification channels",
            exception,
        )
        return false
    }

    private fun migrateLegacyConversationChannels(database: DatabaseWrapper): Int {
        var migratedCount = 0

        database.query(
            DatabaseHelper.CONVERSATIONS_TABLE,
            migrationProjection,
            LEGACY_CUSTOMIZATION_SELECTION,
            null,
            null,
            null,
            null,
        ).use { cursor ->
            val columnIndexes = cursor.resolveMigrationColumnIndexes()

            while (cursor.moveToNext()) {
                val isMigrated = migrateLegacyConversationChannel(
                    cursor = cursor,
                    columnIndexes = columnIndexes,
                )

                if (isMigrated) {
                    migratedCount++
                }
            }
        }

        return migratedCount
    }

    private fun Cursor.resolveMigrationColumnIndexes(): MigrationColumnIndexes {
        return MigrationColumnIndexes(
            conversationId = getColumnIndexOrThrow(ConversationColumns._ID),
            conversationName = getColumnIndexOrThrow(ConversationColumns.NAME),
            notificationEnabled = getColumnIndexOrThrow(ConversationColumns.NOTIFICATION_ENABLED),
            notificationSoundUri = getColumnIndexOrThrow(
                ConversationColumns.NOTIFICATION_SOUND_URI
            ),
            notificationVibration = getColumnIndexOrThrow(
                ConversationColumns.NOTIFICATION_VIBRATION
            ),
        )
    }

    private fun migrateLegacyConversationChannel(
        cursor: Cursor,
        columnIndexes: MigrationColumnIndexes,
    ): Boolean {
        val conversationId = cursor.getString(columnIndexes.conversationId)
        val existingChannel = NotificationChannelUtil.getConversationChannel(conversationId)
        if (existingChannel != null) {
            return false
        }

        val conversationTitle = cursor.getString(columnIndexes.conversationName)
            ?.takeIf { it.isNotBlank() }
            ?: conversationId

        val legacyRingtoneString = cursor.getStringOrNull(columnIndexes.notificationSoundUri)

        NotificationChannelUtil.createConversationChannelFromLegacy(
            conversationId = conversationId,
            conversationTitle = conversationTitle,
            legacyNotificationsEnabled = cursor.getInt(columnIndexes.notificationEnabled) == 1,
            legacyRingtoneString = legacyRingtoneString,
            legacyVibrationEnabled = cursor.getInt(columnIndexes.notificationVibration) == 1,
        )

        return true
    }

    private data class MigrationColumnIndexes(
        val conversationId: Int,
        val conversationName: Int,
        val notificationEnabled: Int,
        val notificationSoundUri: Int,
        val notificationVibration: Int,
    )

    private companion object {
        private const val TAG = "ConversationChannelMigration"

        private val migrationProjection = arrayOf(
            ConversationColumns._ID,
            ConversationColumns.NAME,
            ConversationColumns.NOTIFICATION_ENABLED,
            ConversationColumns.NOTIFICATION_SOUND_URI,
            ConversationColumns.NOTIFICATION_VIBRATION,
        )

        private const val LEGACY_CUSTOMIZATION_SELECTION =
            "${ConversationColumns.NOTIFICATION_ENABLED}=0 OR " +
                "${ConversationColumns.NOTIFICATION_SOUND_URI} IS NOT NULL OR " +
                "${ConversationColumns.NOTIFICATION_VIBRATION}=0"
    }
}
