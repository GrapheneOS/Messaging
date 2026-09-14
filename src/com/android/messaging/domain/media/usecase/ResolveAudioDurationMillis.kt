package com.android.messaging.domain.media.usecase

import androidx.core.net.toUri
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UriUtil
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface ResolveAudioDurationMillis {
    suspend operator fun invoke(contentUri: String): Long
}

internal class ResolveAudioDurationMillisImpl @Inject constructor(
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ResolveAudioDurationMillis {

    private val lock = Mutex()

    private val cachedDurations = object : LinkedHashMap<String, Long>(
        MAX_CACHED_DURATIONS,
        LOAD_FACTOR,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, Long>,
        ): Boolean {
            return size > MAX_CACHED_DURATIONS
        }
    }

    override suspend operator fun invoke(contentUri: String): Long {
        if (contentUri.isBlank()) {
            return 0L
        }

        return cached(contentUri) ?: resolveAndCache(contentUri = contentUri)
    }

    private suspend fun resolveAndCache(contentUri: String): Long {
        val durationMillis = withContext(ioDispatcher) {
            readDurationMillis(contentUri)
        }

        if (durationMillis > 0L) {
            lock.withLock {
                cachedDurations[contentUri] = durationMillis
            }
        }

        return durationMillis
    }

    @Suppress("TooGenericExceptionCaught")
    private fun readDurationMillis(contentUri: String): Long {
        return try {
            contentUri
                .toUri()
                .let(UriUtil::getMediaDurationMs)
                .toLong()
                .coerceAtLeast(0L)
        } catch (exception: Exception) {
            LogUtil.w(TAG, "Failed to resolve audio duration for $contentUri", exception)
            0L
        }
    }

    private suspend fun cached(contentUri: String): Long? {
        return lock.withLock {
            cachedDurations[contentUri]
        }
    }

    private companion object {
        private const val TAG = "ResolveAudioDuration"
        private const val MAX_CACHED_DURATIONS = 256
        private const val LOAD_FACTOR = 0.75f
    }
}
