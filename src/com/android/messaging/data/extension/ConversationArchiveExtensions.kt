package com.android.messaging.data.extension

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns

internal fun SQLiteDatabase.archiveDirectConversations(destination: String) {
    update(
        DatabaseHelper.CONVERSATIONS_TABLE,
        contentValuesOf(ConversationColumns.ARCHIVE_STATUS to 1),
        "${ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION}=?",
        arrayOf(destination),
    )
}
