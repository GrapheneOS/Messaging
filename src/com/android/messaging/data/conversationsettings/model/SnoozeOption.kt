package com.android.messaging.data.conversationsettings.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

internal enum class SnoozeOption(
    val duration: Duration,
) {
    OneHour(1.hours),
    EightHours(8.hours),
    TwentyFourHours(24.hours),
    Always(Duration.INFINITE),
}
