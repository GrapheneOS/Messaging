package com.android.messaging.ui.conversation.messagedetails

import android.content.Context
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.util.Dates

@Composable
internal fun messageDetailsTypeText(type: ConversationMessageDetails.Type): String {
    return when (type) {
        ConversationMessageDetails.Type.SMS -> stringResource(id = R.string.text_message)
        ConversationMessageDetails.Type.MMS -> stringResource(id = R.string.multimedia_message)
    }
}

@Composable
internal fun messageDetailsPriorityText(priority: ConversationMessageDetails.Priority): String {
    return when (priority) {
        ConversationMessageDetails.Priority.HIGH -> stringResource(id = R.string.priority_high)
        ConversationMessageDetails.Priority.NORMAL -> stringResource(id = R.string.priority_normal)
        ConversationMessageDetails.Priority.LOW -> stringResource(id = R.string.priority_low)
    }
}

internal fun formatMessageDetailsTimestamp(timestampMillis: Long): String {
    return Dates.getMessageDetailsTimeString(timestampMillis).toString()
}

internal fun formatMessageDetailsSize(
    context: Context,
    sizeBytes: Long,
): String {
    return Formatter.formatFileSize(context, sizeBytes)
}
