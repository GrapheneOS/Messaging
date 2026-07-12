package com.android.messaging.ui.common.components.mediapreview

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MediaPreviewBitmapPrefetcherTest {

    @Test
    fun updateRequests_preservesTwoActiveLoadsAndStartsNextCandidateByPriority() {
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val bitmapCache = MediaPreviewBitmapCache()
            val prefetcher = MediaPreviewBitmapPrefetcher(workerDispatcher = testDispatcher)
            val releases = mapOf(
                "a" to CompletableDeferred<Unit>(),
                "b" to CompletableDeferred(),
                "c" to CompletableDeferred(),
            )
            val startedContentUris = mutableListOf<String>()
            var activeLoadCount = 0
            var maximumActiveLoadCount = 0
            backgroundScope.launch(testDispatcher) {
                prefetcher.runWorkers(
                    bitmapCache = bitmapCache,
                    bitmapLoader = { item ->
                        startedContentUris += item.contentUri
                        activeLoadCount += 1
                        maximumActiveLoadCount = maxOf(
                            maximumActiveLoadCount,
                            activeLoadCount,
                        )
                        try {
                            releases.getValue(item.contentUri).await()
                            solidBitmap()
                        } finally {
                            activeLoadCount -= 1
                        }
                    },
                )
            }
            val items = mediaPreviewItems("a", "b", "c")

            prefetcher.updateRequests(
                items = items,
                candidates = items,
                bitmapCache = bitmapCache,
            )
            testScheduler.runCurrent()

            assertEquals(listOf("a", "b"), startedContentUris)
            assertEquals(2, maximumActiveLoadCount)

            prefetcher.updateRequests(
                items = items,
                candidates = mediaPreviewItems("c", "b", "a"),
                bitmapCache = bitmapCache,
            )
            testScheduler.runCurrent()
            assertEquals(listOf("a", "b"), startedContentUris)

            releases.getValue("a").complete(Unit)
            testScheduler.runCurrent()

            assertEquals(listOf("a", "b", "c"), startedContentUris)
            assertEquals(2, maximumActiveLoadCount)

            releases.getValue("b").complete(Unit)
            releases.getValue("c").complete(Unit)
            testScheduler.runCurrent()

            assertNotNull(bitmapCache["a"])
            assertNotNull(bitmapCache["b"])
            assertNotNull(bitmapCache["c"])
        }
    }

    @Test
    fun updateRequests_removedActiveItemFinishesButIsNotCached() {
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val bitmapCache = MediaPreviewBitmapCache()
            val prefetcher = MediaPreviewBitmapPrefetcher(workerDispatcher = testDispatcher)
            val firstRelease = CompletableDeferred<Unit>()
            val secondRelease = CompletableDeferred<Unit>()
            val startedContentUris = mutableListOf<String>()
            backgroundScope.launch(testDispatcher) {
                prefetcher.runWorkers(
                    bitmapCache = bitmapCache,
                    bitmapLoader = { item ->
                        startedContentUris += item.contentUri
                        when (item.contentUri) {
                            "first" -> firstRelease.await()
                            else -> secondRelease.await()
                        }
                        solidBitmap()
                    },
                )
            }

            prefetcher.updateRequests(
                items = mediaPreviewItems("first"),
                candidates = mediaPreviewItems("first"),
                bitmapCache = bitmapCache,
            )
            testScheduler.runCurrent()
            prefetcher.updateRequests(
                items = mediaPreviewItems("second"),
                candidates = mediaPreviewItems("second"),
                bitmapCache = bitmapCache,
            )
            testScheduler.runCurrent()

            assertEquals(listOf("first", "second"), startedContentUris)

            firstRelease.complete(Unit)
            secondRelease.complete(Unit)
            testScheduler.runCurrent()

            assertNull(bitmapCache["first"])
            assertNotNull(bitmapCache["second"])
        }
    }

    @Test
    fun failedCandidatesDoNotSpinAndCanRetryAfterLeavingCandidateSet() {
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val bitmapCache = MediaPreviewBitmapCache()
            val prefetcher = MediaPreviewBitmapPrefetcher(workerDispatcher = testDispatcher)
            val attemptCounts = mutableMapOf<String, Int>()
            backgroundScope.launch(testDispatcher) {
                prefetcher.runWorkers(
                    bitmapCache = bitmapCache,
                    bitmapLoader = { item ->
                        val attemptCount = attemptCounts.getOrDefault(item.contentUri, 0) + 1
                        attemptCounts[item.contentUri] = attemptCount
                        when (item.contentUri) {
                            "null" -> when (attemptCount) {
                                1 -> null
                                else -> solidBitmap()
                            }
                            "failure" -> throw IllegalStateException("expected test failure")
                            else -> solidBitmap()
                        }
                    },
                )
            }
            val items = mediaPreviewItems("null", "failure", "healthy")

            prefetcher.updateRequests(
                items = items,
                candidates = items,
                bitmapCache = bitmapCache,
            )
            testScheduler.runCurrent()
            prefetcher.updateRequests(
                items = items,
                candidates = items,
                bitmapCache = bitmapCache,
            )
            testScheduler.runCurrent()

            assertEquals(1, attemptCounts["null"])
            assertEquals(1, attemptCounts["failure"])
            assertEquals(1, attemptCounts["healthy"])
            assertNotNull(bitmapCache["healthy"])

            prefetcher.updateRequests(
                items = items,
                candidates = persistentListOf(),
                bitmapCache = bitmapCache,
            )
            prefetcher.updateRequests(
                items = items,
                candidates = mediaPreviewItems("null"),
                bitmapCache = bitmapCache,
            )
            testScheduler.runCurrent()

            assertEquals(2, attemptCounts["null"])
            assertNotNull(bitmapCache["null"])
            assertTrue(attemptCounts.getValue("healthy") <= 1)
        }
    }

    private fun mediaPreviewItems(vararg contentUris: String): ImmutableList<MediaPreviewItem> {
        return contentUris
            .map { contentUri ->
                MediaPreviewItem(
                    contentUri = contentUri,
                    contentType = "image/jpeg",
                    isVideo = false,
                )
            }
            .let { items -> persistentListOf(*items.toTypedArray()) }
    }

    private fun solidBitmap(): Bitmap {
        return Bitmap.createBitmap(
            intArrayOf(Color.RED),
            1,
            1,
            Bitmap.Config.ARGB_8888,
        )
    }
}
