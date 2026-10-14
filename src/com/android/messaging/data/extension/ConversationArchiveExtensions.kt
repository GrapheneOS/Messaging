package com.android.messaging.data.extension

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns

internal fun SQLiteDatabase.archiveDirectConversations(
    destination: String,
    exceptThreadIds: Set<String> = emptySet(),
) {
    val destinationCondition = "${ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION}=?"
    val selection = when {
        exceptThreadIds.isEmpty() -> destinationCondition
        else -> {
            val placeholders = exceptThreadIds.joinToString(separator = ",") { "?" }
            "$destinationCondition AND ${ConversationColumns.SMS_THREAD_ID} NOT IN ($placeholders)"
        }
    }
    update(
        DatabaseHelper.CONVERSATIONS_TABLE,
        contentValuesOf(ConversationColumns.ARCHIVE_STATUS to 1),
        selection,
        arrayOf(destination) + exceptThreadIds,
    )
}
