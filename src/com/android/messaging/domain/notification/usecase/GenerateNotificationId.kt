package com.android.messaging.domain.notification.usecase

import com.android.messaging.util.core.ElapsedRealtimeProvider
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

internal fun interface GenerateNotificationId {
    operator fun invoke(): Int
}

internal class GenerateNotificationIdImpl @Inject constructor(
    private val elapsedRealtimeProvider: ElapsedRealtimeProvider,
) : GenerateNotificationId {

    private val sequence = AtomicInteger()

    override operator fun invoke(): Int {
        val elapsedRealtimeMillis = elapsedRealtimeProvider.elapsedRealtimeMillis()
        val sequenceValue = sequence.incrementAndGet()

        return hashNotificationSeed(
            elapsedRealtimeMillis = elapsedRealtimeMillis,
            sequenceValue = sequenceValue,
        )
    }

    private fun hashNotificationSeed(
        elapsedRealtimeMillis: Long,
        sequenceValue: Int,
    ): Int {
        val elapsedRealtimeHash = elapsedRealtimeMillis.hashCode()
        return HASH_MULTIPLIER * elapsedRealtimeHash + sequenceValue
    }

    private companion object {
        private const val HASH_MULTIPLIER = 31
    }
}
