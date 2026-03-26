package com.android.messaging.ui.conversation.v2.messages.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

private const val MILLIS_PER_DAY = 86_400_000L

internal fun conversationMessageDisplayEpochDay(
    displayTimestamp: Long,
    timeZone: TimeZone,
): Long? {
    if (displayTimestamp <= 0L) {
        return null
    }

    val localTimestamp = displayTimestamp + timeZone.getOffset(displayTimestamp)

    return Math.floorDiv(localTimestamp, MILLIS_PER_DAY)
}

internal fun conversationMessageDisplayLocalDate(
    displayTimestamp: Long,
): LocalDate? {
    if (displayTimestamp <= 0L) {
        return null
    }

    return Instant
        .ofEpochMilli(displayTimestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
