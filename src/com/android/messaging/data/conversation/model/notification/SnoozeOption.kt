package com.android.messaging.data.conversation.model.notification

import java.util.concurrent.TimeUnit

internal enum class SnoozeOption(
    val durationMillis: Long,
) {
    OneHour(TimeUnit.HOURS.toMillis(1)),
    EightHours(TimeUnit.HOURS.toMillis(8)),
    TwentyFourHours(TimeUnit.HOURS.toMillis(24)),
    Always(Long.MAX_VALUE),
}
