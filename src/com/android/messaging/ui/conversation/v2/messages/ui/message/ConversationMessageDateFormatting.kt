package com.android.messaging.ui.conversation.v2.messages.ui.message

import android.content.Context
import android.text.format.DateUtils
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

private const val MILLIS_PER_DAY = 86_400_000L

private const val COMMON_DATE_SEPARATOR_FORMAT_FLAGS = DateUtils.FORMAT_SHOW_WEEKDAY or
    DateUtils.FORMAT_SHOW_DATE or
    DateUtils.FORMAT_ABBREV_MONTH

internal fun conversationMessageDisplayEpochDay(
    displayTimestamp: Long,
    timeZone: TimeZone,
): Long? {
    return when {
        displayTimestamp <= 0 -> null

        else -> {
            val localTimestamp = displayTimestamp + timeZone.getOffset(displayTimestamp)
            Math.floorDiv(localTimestamp, MILLIS_PER_DAY)
        }
    }
}

private fun conversationMessageDisplayLocalDate(
    displayTimestamp: Long,
): LocalDate? {
    return when {
        displayTimestamp > 0 -> {
            Instant
                .ofEpochMilli(displayTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        else -> null
    }
}

internal fun formatDateSeparatorText(
    context: Context,
    message: ConversationMessageUiModel,
): String? {
    val timestamp = message.displayTimestamp

    if (timestamp <= 0L) {
        return null
    }

    val isSameYear = conversationMessageDisplayLocalDate(
        displayTimestamp = timestamp,
    )?.year == LocalDate.now().year

    val dateTimeFormatFlags = when {
        isSameYear -> COMMON_DATE_SEPARATOR_FORMAT_FLAGS or DateUtils.FORMAT_NO_YEAR
        else -> COMMON_DATE_SEPARATOR_FORMAT_FLAGS or DateUtils.FORMAT_SHOW_YEAR
    }

    return DateUtils.formatDateTime(
        context,
        timestamp,
        dateTimeFormatFlags,
    )
}
