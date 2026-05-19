package com.android.messaging.data.conversationsettings.model

import java.util.concurrent.TimeUnit

private const val ONE_HOUR = 1L
private const val EIGHT_HOURS = 8L
private const val TWENTY_FOUR_HOURS = 24L

internal enum class SnoozeOption(
    val durationMillis: Long,
) {
    OneHour(TimeUnit.HOURS.toMillis(ONE_HOUR)),
    EightHours(TimeUnit.HOURS.toMillis(EIGHT_HOURS)),
    TwentyFourHours(TimeUnit.HOURS.toMillis(TWENTY_FOUR_HOURS)),
    Always(Long.MAX_VALUE),
}
