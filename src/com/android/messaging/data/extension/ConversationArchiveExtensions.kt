package com.android.messaging.data.extension

import androidx.core.content.contentValuesOf
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseWrapper

internal fun DatabaseWrapper.archiveDirectConversations(destination: String) {
    update(
        DatabaseHelper.CONVERSATIONS_TABLE,
        contentValuesOf(ConversationColumns.ARCHIVE_STATUS to 1),
        "${ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION}=?",
        arrayOf(destination),
    )
}
