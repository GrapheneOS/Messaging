package com.android.messaging.datamodel.action.mms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import androidx.core.net.toUri
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.mmslib.SqliteWrapper
import com.android.messaging.mmslib.pdu.PduHeaders
import com.android.messaging.sms.MmsUtils
import com.android.messaging.util.LogUtil

internal class IsMmsNotificationDownloadCountSynchronized(
    private val context: Context,
    private val database: DatabaseWrapper,
) {

    operator fun invoke(
        localCount: Int,
        remoteCount: Int,
        localSelection: String,
        localSelectionArgs: Array<String>?,
        smsSelection: String,
        smsSelectionArgs: Array<String>?,
        mmsSelection: String,
        mmsSelectionArgs: Array<String>?,
    ): Boolean {
        val localSqlSelection = SqlSelection(
            selection = localSelection,
            selectionArgs = localSelectionArgs ?: emptyArray(),
        )
        val remoteSmsSelection = SqlSelection(
            selection = smsSelection,
            selectionArgs = smsSelectionArgs ?: emptyArray(),
        )
        val remoteMmsSelection = SqlSelection(
            selection = mmsSelection,
            selectionArgs = mmsSelectionArgs ?: emptyArray(),
        )

        val syncState = when {
            localCount > 0 -> {
                getSyncState(
                    localSelection = localSqlSelection,
                    remoteMmsSelection = remoteMmsSelection,
                )
            }

            else -> SyncState()
        }

        val ignoredMessageIds = syncState.ignoredMessageIds
        val adjustedLocalCount = localCount - ignoredMessageIds.size
        val isInSync = when {
            syncState.hasRemoteReplacement -> false
            ignoredMessageIds.isEmpty() -> localCount == remoteCount
            adjustedLocalCount != remoteCount -> false
            remoteCount == 0 -> true

            else -> {
                remainingLocalMessagesMatchRemote(
                    localSelection = localSqlSelection,
                    ignoredMessageIds = ignoredMessageIds,
                    remoteSmsSelection = remoteSmsSelection,
                    remoteMmsSelection = remoteMmsSelection,
                )
            }
        }

        logMessageCounts(
            isInSync = isInSync,
            localCount = localCount,
            adjustedLocalCount = adjustedLocalCount,
            ignoredLocalCount = ignoredMessageIds.size,
            hasRemoteReplacement = syncState.hasRemoteReplacement,
            remoteCount = remoteCount,
        )

        return isInSync
    }

    private fun getSyncState(
        localSelection: SqlSelection,
        remoteMmsSelection: SqlSelection,
    ): SyncState {
        val localNotifications = queryLocalNotifications(
            localSelection = localSelection,
        )

        val existingRemoteMmsIds = queryExistingRemoteMmsIds(
            remoteMmsIds = collectRemoteMmsIds(
                localNotifications = localNotifications,
            ),
        )

        val missingRemoteNotifications = collectMissingRemoteNotifications(
            localNotifications = localNotifications,
            existingRemoteMmsIds = existingRemoteMmsIds,
        )

        val replacementTransactionIds = queryRemoteReplacementTransactionIds(
            transactionIds = collectTransactionIds(
                localNotifications = missingRemoteNotifications,
            ),
            remoteMmsSelection = remoteMmsSelection,
        )

        return resolveSyncState(
            missingRemoteNotifications = missingRemoteNotifications,
            replacementTransactionIds = replacementTransactionIds,
        )
    }

    private fun resolveSyncState(
        missingRemoteNotifications: List<LocalNotification>,
        replacementTransactionIds: Set<String>,
    ): SyncState {
        val ignoredMessageIds = mutableListOf<Long>()
        val nowMillis = System.currentTimeMillis()

        for (notification in missingRemoteNotifications) {
            val transactionId = notification.transactionId
            val notificationHasRemoteReplacement = !transactionId.isNullOrEmpty() &&
                replacementTransactionIds.contains(transactionId)

            when {
                notificationHasRemoteReplacement -> {
                    return SyncState(hasRemoteReplacement = true)
                }

                isMmsNotificationExpired(
                    mmsExpiry = notification.mmsExpiry,
                    nowMillis = nowMillis,
                ) -> {
                    // Leave it counted so normal sync repair can delete the orphaned row
                }

                else -> {
                    ignoredMessageIds.add(notification.messageId)
                }
            }
        }

        return SyncState(ignoredMessageIds = ignoredMessageIds)
    }

    private fun remainingLocalMessagesMatchRemote(
        localSelection: SqlSelection,
        ignoredMessageIds: List<Long>,
        remoteSmsSelection: SqlSelection,
        remoteMmsSelection: SqlSelection,
    ): Boolean {
        val remoteTimestamps = queryRemoteTimestamps(
            remoteSmsSelection = remoteSmsSelection,
            remoteMmsSelection = remoteMmsSelection,
        )

        val localTimestamps = when {
            remoteTimestamps == null -> null
            else -> queryLocalTimestampsExcludingIgnored(
                localSelection = localSelection,
                ignoredMessageIds = ignoredMessageIds,
            )
        }

        return remoteTimestamps == null ||
            localTimestamps?.sorted() == remoteTimestamps.sorted()
    }

    private fun queryRemoteTimestamps(
        remoteSmsSelection: SqlSelection,
        remoteMmsSelection: SqlSelection,
    ): List<Long>? {
        return try {
            val smsTimestamps = queryRemoteTimestamps(
                contentUri = Sms.CONTENT_URI,
                timestampColumn = Sms.DATE,
                selection = remoteSmsSelection,
            )
            val mmsTimestamps = queryRemoteTimestamps(
                contentUri = Mms.CONTENT_URI,
                timestampColumn = Mms.DATE,
                selection = remoteMmsSelection,
            )

            when {
                smsTimestamps == null || mmsTimestamps == null -> null
                else -> {
                    smsTimestamps + mmsTimestamps.map { timestamp -> timestamp * 1000L }
                }
            }
        } catch (exception: RuntimeException) {
            LogUtil.w(TAG, "SyncCursorPair: failed to query remote message timestamps", exception)
            null
        }
    }

    private fun queryRemoteTimestamps(
        contentUri: Uri,
        timestampColumn: String,
        selection: SqlSelection,
    ): List<Long>? {
        return SqliteWrapper
            .query(
                context,
                context.contentResolver,
                contentUri,
                arrayOf(timestampColumn),
                selection.selection,
                selection.selectionArgs,
                null,
            )
            ?.use { cursor ->
                readTimestamps(cursor = cursor, timestampColumn = timestampColumn)
            }
            ?: run {
                LogUtil.w(TAG, "SyncCursorPair: remote timestamp query returned no cursor")
                null
            }
    }

    private fun queryLocalTimestampsExcludingIgnored(
        localSelection: SqlSelection,
        ignoredMessageIds: List<Long>,
    ): List<Long> {
        val remainingSelection = localSelection + SqlSelection(
            selection = "${BaseColumns._ID} NOT IN " +
                MmsUtils.getSqlInOperand(ignoredMessageIds.size),
            selectionArgs = Array(ignoredMessageIds.size) { ignoredMessageIds[it].toString() },
        )

        return database.query(
            DatabaseHelper.MESSAGES_TABLE,
            arrayOf(MessageColumns.RECEIVED_TIMESTAMP),
            remainingSelection.selection,
            remainingSelection.selectionArgs,
            null,
            null,
            null,
        ).use { cursor ->
            readTimestamps(
                cursor = cursor,
                timestampColumn = MessageColumns.RECEIVED_TIMESTAMP,
            )
        }
    }

    private fun readTimestamps(cursor: Cursor, timestampColumn: String): List<Long> {
        val timestampIndex = cursor.getColumnIndexOrThrow(timestampColumn)

        return buildList {
            while (cursor.moveToNext()) {
                add(cursor.getLong(timestampIndex))
            }
        }
    }

    private fun logMessageCounts(
        isInSync: Boolean,
        localCount: Int,
        adjustedLocalCount: Int,
        ignoredLocalCount: Int,
        hasRemoteReplacement: Boolean,
        remoteCount: Int,
    ) {
        if (isInSync) {
            if (LogUtil.isLoggable(TAG, LogUtil.DEBUG)) {
                LogUtil.d(
                    TAG,
                    "SyncCursorPair: Same # of local and remote messages = $adjustedLocalCount",
                )
            }
        } else {
            LogUtil.i(
                TAG,
                "SyncCursorPair: Not in sync; # local messages = $localCount" +
                    ", # ignored local MMS notifications = $ignoredLocalCount" +
                    ", has remote MMS replacement = $hasRemoteReplacement" +
                    ", # remote message = $remoteCount",
            )
        }
    }

    private fun queryLocalNotifications(localSelection: SqlSelection): List<LocalNotification> {
        val notificationSelection = localSelection + SqlSelection(
            selection = "${MessageColumns.PROTOCOL}=? AND " +
                "${MessageColumns.STATUS} IN " +
                mmsNotificationDownloadStatusInOperand,
            selectionArgs = arrayOf(
                MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION.toString(),
                *mmsNotificationDownloadStatusArgs,
            ),
        )

        return database.query(
            DatabaseHelper.MESSAGES_TABLE,
            arrayOf(
                BaseColumns._ID,
                MessageColumns.SMS_MESSAGE_URI,
                MessageColumns.MMS_TRANSACTION_ID,
                MessageColumns.MMS_EXPIRY,
            ),
            notificationSelection.selection,
            notificationSelection.selectionArgs,
            null,
            null,
            null,
        ).use(::readLocalNotifications)
    }

    private fun queryExistingRemoteMmsIds(remoteMmsIds: Set<Long>): Set<Long>? {
        if (remoteMmsIds.isEmpty()) {
            return emptySet()
        }

        return try {
            queryRemoteMmsIds(remoteMmsIds = remoteMmsIds)
        } catch (exception: RuntimeException) {
            LogUtil.w(TAG, "SyncCursorPair: failed to query remote MMS rows", exception)
            null
        }
    }

    private fun queryRemoteMmsIds(remoteMmsIds: Set<Long>): Set<Long>? {
        val remoteMmsCursor = SqliteWrapper.query(
            context,
            context.contentResolver,
            Mms.CONTENT_URI,
            arrayOf(Mms._ID),
            "${Mms._ID} IN ${MmsUtils.getSqlInOperand(remoteMmsIds.size)}",
            createRemoteMmsIdSelectionArgs(remoteMmsIds = remoteMmsIds),
            null,
        )

        return remoteMmsCursor
            ?.use(::readRemoteMmsIds)
            ?: run {
                LogUtil.w(TAG, "SyncCursorPair: remote MMS row query returned no cursor")
                null
            }
    }

    private fun readRemoteMmsIds(cursor: Cursor): Set<Long> {
        val remoteMmsIdIndex = cursor.getColumnIndexOrThrow(Mms._ID)

        return buildSet {
            while (cursor.moveToNext()) {
                add(cursor.getLong(remoteMmsIdIndex))
            }
        }
    }

    private fun createRemoteMmsIdSelectionArgs(remoteMmsIds: Set<Long>): Array<String> {
        return remoteMmsIds
            .map { remoteMmsId -> remoteMmsId.toString() }
            .toTypedArray()
    }

    private fun queryRemoteReplacementTransactionIds(
        transactionIds: Set<String>,
        remoteMmsSelection: SqlSelection,
    ): Set<String> {
        if (transactionIds.isEmpty()) {
            return emptySet()
        }

        return try {
            queryTransactionIds(
                selection = createRemoteReplacementSelection(
                    transactionIds = transactionIds,
                    remoteMmsSelection = remoteMmsSelection,
                ),
            )
        } catch (exception: RuntimeException) {
            LogUtil.w(
                TAG,
                "SyncCursorPair: failed to query remote MMS replacements",
                exception,
            )
            // Retry later instead of forcing a full sync on a transient telephony query failure
            emptySet()
        }
    }

    private fun createRemoteReplacementSelection(
        transactionIds: Set<String>,
        remoteMmsSelection: SqlSelection,
    ): SqlSelection {
        return remoteMmsSelection + SqlSelection(
            selection = "${Mms.MESSAGE_BOX}=? AND ${Mms.MESSAGE_TYPE}=? AND " +
                "${Mms.TRANSACTION_ID} IN " +
                MmsUtils.getSqlInOperand(transactionIds.size),
            selectionArgs = arrayOf(
                Mms.MESSAGE_BOX_INBOX.toString(),
                PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF.toString(),
                *transactionIds.toTypedArray(),
            ),
        )
    }

    private fun queryTransactionIds(selection: SqlSelection): Set<String> {
        return SqliteWrapper
            .query(
                context,
                context.contentResolver,
                Mms.CONTENT_URI,
                arrayOf(Mms.TRANSACTION_ID),
                selection.selection,
                selection.selectionArgs,
                null,
            )
            ?.use(::readTransactionIds)
            ?: emptySet()
    }

    private fun readTransactionIds(cursor: Cursor): Set<String> {
        val transactionIdIndex = cursor.getColumnIndexOrThrow(Mms.TRANSACTION_ID)

        return buildSet {
            while (cursor.moveToNext()) {
                cursor
                    .getString(transactionIdIndex)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(::add)
            }
        }
    }

    private fun readLocalNotifications(cursor: Cursor): List<LocalNotification> {
        val messageIdIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
        val messageUriIndex = cursor.getColumnIndexOrThrow(MessageColumns.SMS_MESSAGE_URI)
        val transactionIdIndex = cursor
            .getColumnIndexOrThrow(MessageColumns.MMS_TRANSACTION_ID)
        val mmsExpiryIndex = cursor.getColumnIndexOrThrow(MessageColumns.MMS_EXPIRY)

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    LocalNotification(
                        messageId = cursor.getLong(messageIdIndex),
                        remoteMmsId = parseRemoteMmsId(
                            messageUri = cursor.getString(messageUriIndex),
                        ),
                        transactionId = cursor.getString(transactionIdIndex),
                        mmsExpiry = cursor.getLong(mmsExpiryIndex),
                    ),
                )
            }
        }
    }

    private fun parseRemoteMmsId(messageUri: String?): Long? {
        return messageUri
            ?.takeIf { it.isNotEmpty() }
            ?.toUri()
            ?.let(MmsUtils::parseRowIdFromMessageUri)
            ?.takeIf { it >= 0L }
    }

    private fun collectRemoteMmsIds(localNotifications: List<LocalNotification>): Set<Long> {
        return localNotifications.mapNotNullTo(destination = LinkedHashSet()) { it.remoteMmsId }
    }

    private fun collectMissingRemoteNotifications(
        localNotifications: List<LocalNotification>,
        existingRemoteMmsIds: Set<Long>?,
    ): List<LocalNotification> {
        return localNotifications.filterNot { notification ->
            remoteMmsMayExist(
                remoteMmsId = notification.remoteMmsId,
                existingRemoteMmsIds = existingRemoteMmsIds,
            )
        }
    }

    private fun collectTransactionIds(
        localNotifications: List<LocalNotification>,
    ): Set<String> {
        return localNotifications.mapNotNullTo(destination = LinkedHashSet()) { notification ->
            notification.transactionId?.takeIf { it.isNotEmpty() }
        }
    }

    private fun remoteMmsMayExist(
        remoteMmsId: Long?,
        existingRemoteMmsIds: Set<Long>?,
    ): Boolean {
        return existingRemoteMmsIds == null ||
            remoteMmsId == null ||
            existingRemoteMmsIds.contains(remoteMmsId)
    }

    private data class SyncState(
        val ignoredMessageIds: List<Long> = emptyList(),
        val hasRemoteReplacement: Boolean = false,
    )

    private class SqlSelection(
        val selection: String,
        val selectionArgs: Array<String> = emptyArray(),
    ) {

        operator fun plus(other: SqlSelection): SqlSelection {
            return SqlSelection(
                selection = "($selection) AND (${other.selection})",
                selectionArgs = selectionArgs + other.selectionArgs,
            )
        }
    }

    private data class LocalNotification(
        val messageId: Long,
        val remoteMmsId: Long?,
        val transactionId: String?,
        val mmsExpiry: Long,
    )

    private companion object {
        private const val TAG = LogUtil.BUGLE_TAG
    }
}
