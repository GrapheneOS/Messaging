package com.android.messaging.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val MINUTE_MILLIS = 60_000L

private val LocalWallClockMillis = compositionLocalOf<Long?> { null }

internal fun wallClockMinuteTicks(
    currentTimeMillis: () -> Long = System::currentTimeMillis,
): Flow<Long> {
    return flow {
        while (true) {
            val nowMillis = currentTimeMillis()
            emit(nowMillis)
            delay(MINUTE_MILLIS - nowMillis % MINUTE_MILLIS)
        }
    }
}

@Composable
internal fun ProvideWallClock(
    ticks: Flow<Long> = remember { wallClockMinuteTicks() },
    content: @Composable () -> Unit,
) {
    val wallClockMillis by ticks.collectAsStateWithLifecycle(
        initialValue = System.currentTimeMillis(),
    )

    CompositionLocalProvider(
        LocalWallClockMillis provides wallClockMillis,
        content = content,
    )
}

@Composable
internal fun currentWallClockMillis(): Long {
    return LocalWallClockMillis.current ?: System.currentTimeMillis()
}
