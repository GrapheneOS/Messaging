package com.android.messaging.util.core

internal fun interface ElapsedRealtimeProvider {
    fun elapsedRealtimeMillis(): Long
}
