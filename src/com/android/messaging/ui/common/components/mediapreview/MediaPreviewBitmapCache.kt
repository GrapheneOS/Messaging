package com.android.messaging.ui.common.components.mediapreview

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.android.messaging.util.LogUtil
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Stable
internal class MediaPreviewBitmapCache {
    private val cachedBackgroundBitmapsByContentUri = mutableStateMapOf<String, ImageBitmap>()

    operator fun get(contentUri: String): ImageBitmap? {
        return cachedBackgroundBitmapsByContentUri[contentUri]
    }

    fun put(contentUri: String, bitmap: Bitmap) {
        cachedBackgroundBitmapsByContentUri[contentUri] = bitmap.asImageBitmap()
    }

    fun removeInactive(activeContentUris: Set<String>) {
        cachedBackgroundBitmapsByContentUri
            .keys
            .asSequence()
            .filterNot { it in activeContentUris }
            .toSet()
            .let { inactiveContentUris ->
                cachedBackgroundBitmapsByContentUri -= inactiveContentUris
            }
    }
}

internal class MediaPreviewBitmapPrefetcher(
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val requestSignal = Channel<Unit>(capacity = Channel.CONFLATED)
    private val stateMutex = Mutex()
    private val candidateItemsByContentUri = linkedMapOf<String, MediaPreviewItem>()
    private val inFlightContentUris = mutableSetOf<String>()
    private val attemptedItemsByContentUri = mutableMapOf<String, MediaPreviewItem>()
    private var activeContentUris = emptySet<String>()

    suspend fun updateRequests(
        items: ImmutableList<MediaPreviewItem>,
        candidates: ImmutableList<MediaPreviewItem>,
        bitmapCache: MediaPreviewBitmapCache,
    ) {
        val updatedActiveContentUris = items
            .asSequence()
            .map { item -> item.contentUri }
            .toSet()

        stateMutex.withLock {
            activeContentUris = updatedActiveContentUris
            updateCandidates(
                candidates = candidates,
                activeContentUris = updatedActiveContentUris,
            )
        }
        bitmapCache.removeInactive(activeContentUris = updatedActiveContentUris)
        requestSignal.trySend(Unit)
    }

    suspend fun runWorkers(
        bitmapCache: MediaPreviewBitmapCache,
        bitmapLoader: suspend (MediaPreviewItem) -> Bitmap?,
    ) {
        coroutineScope {
            repeat(times = MEDIA_PREVIEW_BACKGROUND_LOAD_CONCURRENCY) {
                launch(workerDispatcher) {
                    runWorker(
                        bitmapCache = bitmapCache,
                        bitmapLoader = bitmapLoader,
                    )
                }
            }
        }
    }

    private fun updateCandidates(
        candidates: ImmutableList<MediaPreviewItem>,
        activeContentUris: Set<String>,
    ) {
        val previousCandidateContentUris = candidateItemsByContentUri.keys.toSet()
        candidateItemsByContentUri.clear()
        candidates
            .asSequence()
            .filter { item -> item.contentUri in activeContentUris }
            .forEach { item -> candidateItemsByContentUri.putIfAbsent(item.contentUri, item) }

        val removedCandidateContentUris = previousCandidateContentUris -
            candidateItemsByContentUri.keys
        attemptedItemsByContentUri.keys.removeAll(removedCandidateContentUris)
    }

    private suspend fun runWorker(
        bitmapCache: MediaPreviewBitmapCache,
        bitmapLoader: suspend (MediaPreviewItem) -> Bitmap?,
    ) {
        while (true) {
            val item = takeNextItem(bitmapCache = bitmapCache)
            when (item) {
                null -> requestSignal.receive()
                else -> loadItem(
                    item = item,
                    bitmapCache = bitmapCache,
                    bitmapLoader = bitmapLoader,
                )
            }
        }
    }

    private suspend fun takeNextItem(
        bitmapCache: MediaPreviewBitmapCache,
    ): MediaPreviewItem? {
        return stateMutex.withLock {
            candidateItemsByContentUri
                .values
                .firstOrNull { item ->
                    bitmapCache[item.contentUri] == null &&
                        item.contentUri !in inFlightContentUris &&
                        attemptedItemsByContentUri[item.contentUri] != item
                }
                ?.also { item ->
                    inFlightContentUris += item.contentUri
                    attemptedItemsByContentUri[item.contentUri] = item
                    requestSignal.trySend(Unit)
                }
        }
    }

    private suspend fun loadItem(
        item: MediaPreviewItem,
        bitmapCache: MediaPreviewBitmapCache,
        bitmapLoader: suspend (MediaPreviewItem) -> Bitmap?,
    ) {
        val bitmap = try {
            bitmapLoader(item)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            failedBitmap(exception = exception)
        } catch (exception: IllegalArgumentException) {
            failedBitmap(exception = exception)
        } catch (exception: IllegalStateException) {
            failedBitmap(exception = exception)
        } catch (exception: SecurityException) {
            failedBitmap(exception = exception)
        }

        val shouldCacheBitmap = stateMutex.withLock {
            inFlightContentUris -= item.contentUri
            item.contentUri in activeContentUris
        }

        if (bitmap != null && shouldCacheBitmap) {
            bitmapCache.put(
                contentUri = item.contentUri,
                bitmap = bitmap,
            )
        }

        requestSignal.trySend(Unit)
    }

    private fun failedBitmap(exception: Exception): Bitmap? {
        LogUtil.w(TAG, "Failed to load a media preview background", exception)
        return null
    }

    private companion object {
        private const val TAG = "MediaPreviewBitmap"

        private const val MEDIA_PREVIEW_BACKGROUND_LOAD_CONCURRENCY = 2
    }
}
