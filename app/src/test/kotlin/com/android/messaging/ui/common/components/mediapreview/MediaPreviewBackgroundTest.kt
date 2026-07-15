package com.android.messaging.ui.common.components.mediapreview

import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MediaPreviewBackgroundTest {

    @Test
    fun blendPages_forwardCurrentPageFlip_keepsSamePairAndContinuousAlpha() {
        val beforeFlip = resolveMediaPreviewBackgroundBlendPages(
            currentPage = 0,
            currentPageOffsetFraction = 0.49f,
            pageCount = 3,
        )
        val afterFlip = resolveMediaPreviewBackgroundBlendPages(
            currentPage = 1,
            currentPageOffsetFraction = -0.49f,
            pageCount = 3,
        )

        assertEquals(beforeFlip, afterFlip)
        assertEquals(
            0.49f,
            resolveMediaPreviewBackgroundUpperAlpha(
                currentPage = 0,
                currentPageOffsetFraction = 0.49f,
                lowerPage = beforeFlip.lowerPage,
                pageCount = 3,
            ),
            FLOAT_DELTA,
        )
        assertEquals(
            0.51f,
            resolveMediaPreviewBackgroundUpperAlpha(
                currentPage = 1,
                currentPageOffsetFraction = -0.49f,
                lowerPage = afterFlip.lowerPage,
                pageCount = 3,
            ),
            FLOAT_DELTA,
        )
    }

    @Test
    fun blendPages_reverseCurrentPageFlip_keepsSamePairAndContinuousAlpha() {
        val beforeFlip = resolveMediaPreviewBackgroundBlendPages(
            currentPage = 1,
            currentPageOffsetFraction = -0.49f,
            pageCount = 3,
        )
        val afterFlip = resolveMediaPreviewBackgroundBlendPages(
            currentPage = 0,
            currentPageOffsetFraction = 0.49f,
            pageCount = 3,
        )

        assertEquals(beforeFlip, afterFlip)
        assertEquals(
            0.51f,
            resolveMediaPreviewBackgroundUpperAlpha(
                currentPage = 1,
                currentPageOffsetFraction = -0.49f,
                lowerPage = beforeFlip.lowerPage,
                pageCount = 3,
            ),
            FLOAT_DELTA,
        )
        assertEquals(
            0.49f,
            resolveMediaPreviewBackgroundUpperAlpha(
                currentPage = 0,
                currentPageOffsetFraction = 0.49f,
                lowerPage = afterFlip.lowerPage,
                pageCount = 3,
            ),
            FLOAT_DELTA,
        )
    }

    @Test
    fun blendPages_integerBoundary_switchesPairOnFullyOpaqueSharedPage() {
        val approachingBoundary = resolveMediaPreviewBackgroundBlendPages(
            currentPage = 1,
            currentPageOffsetFraction = -0.001f,
            pageCount = 3,
        )
        val atBoundary = resolveMediaPreviewBackgroundBlendPages(
            currentPage = 1,
            currentPageOffsetFraction = 0f,
            pageCount = 3,
        )

        assertEquals(
            MediaPreviewBackgroundBlendPages(
                lowerPage = 0,
                upperPage = 1,
            ),
            approachingBoundary,
        )
        assertEquals(
            MediaPreviewBackgroundBlendPages(
                lowerPage = 1,
                upperPage = 2,
            ),
            atBoundary,
        )
        assertEquals(
            0.999f,
            resolveMediaPreviewBackgroundUpperAlpha(
                currentPage = 1,
                currentPageOffsetFraction = -0.001f,
                lowerPage = approachingBoundary.lowerPage,
                pageCount = 3,
            ),
            FLOAT_DELTA,
        )
        assertEquals(
            0f,
            resolveMediaPreviewBackgroundUpperAlpha(
                currentPage = 1,
                currentPageOffsetFraction = 0f,
                lowerPage = atBoundary.lowerPage,
                pageCount = 3,
            ),
            FLOAT_DELTA,
        )
    }

    @Test
    fun blendPages_singlePage_clampsPairAndAlpha() {
        val blendPages = resolveMediaPreviewBackgroundBlendPages(
            currentPage = 4,
            currentPageOffsetFraction = -0.4f,
            pageCount = 1,
        )

        assertEquals(MediaPreviewBackgroundBlendPages(lowerPage = 0, upperPage = 0), blendPages)
        assertEquals(
            0f,
            resolveMediaPreviewBackgroundUpperAlpha(
                currentPage = 4,
                currentPageOffsetFraction = -0.4f,
                lowerPage = blendPages.lowerPage,
                pageCount = 1,
            ),
            FLOAT_DELTA,
        )
    }

    @Test
    fun interactivePairReady_requiresOnlyFramesWithVisibleContribution() {
        val distinctBlendPages = MediaPreviewBackgroundBlendPages(
            lowerPage = 1,
            upperPage = 2,
        )

        assertTrue(
            resolveMediaPreviewBackgroundInteractivePairReady(
                currentPageOffsetFraction = 0f,
                blendPages = distinctBlendPages,
                hasLowerFrame = true,
                hasUpperFrame = false,
            ),
        )
        assertFalse(
            resolveMediaPreviewBackgroundInteractivePairReady(
                currentPageOffsetFraction = 0.001f,
                blendPages = distinctBlendPages,
                hasLowerFrame = true,
                hasUpperFrame = false,
            ),
        )
        assertFalse(
            resolveMediaPreviewBackgroundInteractivePairReady(
                currentPageOffsetFraction = -0.001f,
                blendPages = distinctBlendPages,
                hasLowerFrame = true,
                hasUpperFrame = false,
            ),
        )
        assertTrue(
            resolveMediaPreviewBackgroundInteractivePairReady(
                currentPageOffsetFraction = 0.25f,
                blendPages = distinctBlendPages,
                hasLowerFrame = true,
                hasUpperFrame = true,
            ),
        )
        assertFalse(
            resolveMediaPreviewBackgroundInteractivePairReady(
                currentPageOffsetFraction = 0f,
                blendPages = distinctBlendPages,
                hasLowerFrame = false,
                hasUpperFrame = true,
            ),
        )
        assertTrue(
            resolveMediaPreviewBackgroundInteractivePairReady(
                currentPageOffsetFraction = 0.25f,
                blendPages = MediaPreviewBackgroundBlendPages(
                    lowerPage = 1,
                    upperPage = 1,
                ),
                hasLowerFrame = true,
                hasUpperFrame = false,
            ),
        )
    }

    @Test
    fun prefetchItems_prioritizesSettledTargetAndCurrentAndDeduplicatesUris() {
        val items = persistentListOf(
            mediaPreviewItem(contentUri = "a"),
            mediaPreviewItem(contentUri = "b"),
            mediaPreviewItem(contentUri = "c"),
            mediaPreviewItem(contentUri = "d"),
            mediaPreviewItem(contentUri = "b"),
        )

        val result = getMediaPreviewBackgroundPrefetchItems(
            items = items,
            currentPage = 2,
            settledPage = 1,
            targetPage = 3,
            blendPages = MediaPreviewBackgroundBlendPages(
                lowerPage = 1,
                upperPage = 2,
            ),
        )

        assertEquals(listOf("b", "d", "c", "a"), result.map { item -> item.contentUri })
    }

    @Test
    fun transitionState_missingPair_keepsAnchorUntilReadyFrameRecovers() {
        runTest {
            val state = mediaPreviewBackgroundTransitionState()
            val firstFrame = mediaPreviewBackgroundFrame(contentUri = "first")
            val secondFrame = mediaPreviewBackgroundFrame(contentUri = "second")

            state.settle(frame = firstFrame)
            state.onScrollFrame(
                isInteractivePairReady = false,
                currentPageFrame = null,
            )
            state.onScrollFrame(
                isInteractivePairReady = true,
                currentPageFrame = secondFrame,
            )

            assertSame(firstFrame, state.displayedFrame)
            assertTrue(state.isScrollFallbackLatched)

            state.settle(frame = secondFrame)

            assertSame(secondFrame, state.displayedFrame)
            assertNull(state.incomingFrame)
            assertFalse(state.isScrollFallbackLatched)
        }
    }

    @Test
    fun transitionState_sameSettledFrame_clearsFallbackWithoutReplacingFrame() {
        runTest {
            val state = mediaPreviewBackgroundTransitionState()
            val frame = mediaPreviewBackgroundFrame(contentUri = "same")

            state.settle(frame = frame)
            state.onScrollFrame(
                isInteractivePairReady = false,
                currentPageFrame = null,
            )
            state.settle(frame = mediaPreviewBackgroundFrame(contentUri = "same"))

            assertSame(frame, state.displayedFrame)
            assertFalse(state.isScrollFallbackLatched)
        }
    }

    @Test
    fun transitionState_clear_removesDisplayedAndIncomingFrames() {
        runTest {
            val state = mediaPreviewBackgroundTransitionState()

            state.settle(frame = mediaPreviewBackgroundFrame(contentUri = "first"))
            state.clear()

            assertNull(state.displayedFrame)
            assertNull(state.incomingFrame)
            assertFalse(state.isScrollFallbackLatched)
        }
    }

    @Test
    fun transitionState_readyScrollSettlesImmediatelyWithoutRecovery() {
        runTest {
            var recoveryCount = 0
            val state = MediaPreviewBackgroundTransitionState(
                recoveryAnimator = { recoveryProgress ->
                    recoveryCount += 1
                    recoveryProgress.snapTo(targetValue = 1f)
                },
            )
            val firstFrame = mediaPreviewBackgroundFrame(contentUri = "first")
            val secondFrame = mediaPreviewBackgroundFrame(contentUri = "second")

            state.settle(frame = firstFrame)
            state.onScrollFrame(
                isInteractivePairReady = true,
                currentPageFrame = firstFrame,
            )
            state.settle(frame = secondFrame)

            assertSame(secondFrame, state.displayedFrame)
            assertNull(state.incomingFrame)
            assertFalse(state.isScrollFallbackLatched)
            assertEquals(1, recoveryCount)
        }
    }

    @Test
    fun transitionState_scrollDuringRecovery_finishesSafelyThenRecoversLatestFrame() {
        runTest {
            val recoveryStarted = CompletableDeferred<Unit>()
            val releaseRecovery = CompletableDeferred<Unit>()
            val testDispatcher = StandardTestDispatcher(testScheduler)
            var recoveryCount = 0
            val state = MediaPreviewBackgroundTransitionState(
                recoveryAnimator = { recoveryProgress ->
                    recoveryCount += 1
                    when (recoveryCount) {
                        1 -> recoveryProgress.snapTo(targetValue = 1f)
                        else -> {
                            recoveryStarted.complete(Unit)
                            releaseRecovery.await()
                            recoveryProgress.snapTo(targetValue = 1f)
                        }
                    }
                },
            )
            val firstFrame = mediaPreviewBackgroundFrame(contentUri = "first")
            val secondFrame = mediaPreviewBackgroundFrame(contentUri = "second")
            val latestFrame = mediaPreviewBackgroundFrame(contentUri = "latest")

            state.settle(frame = firstFrame)
            val recoveryJob = backgroundScope.launch(testDispatcher) {
                state.settle(frame = secondFrame)
            }
            testScheduler.runCurrent()
            recoveryStarted.await()

            state.onScrollFrame(
                isInteractivePairReady = true,
                currentPageFrame = firstFrame,
            )
            assertTrue(state.isScrollFallbackLatched)
            assertSame(secondFrame, state.incomingFrame)

            releaseRecovery.complete(Unit)
            recoveryJob.join()

            assertSame(secondFrame, state.displayedFrame)
            assertTrue(state.isScrollFallbackLatched)

            state.settle(frame = latestFrame)

            assertSame(latestFrame, state.displayedFrame)
            assertFalse(state.isScrollFallbackLatched)
        }
    }

    private fun mediaPreviewBackgroundTransitionState(): MediaPreviewBackgroundTransitionState {
        return MediaPreviewBackgroundTransitionState(
            recoveryAnimator = { recoveryProgress ->
                recoveryProgress.snapTo(targetValue = 1f)
            },
        )
    }

    private fun mediaPreviewBackgroundFrame(contentUri: String): MediaPreviewBackgroundFrame {
        return MediaPreviewBackgroundFrame(
            contentUri = contentUri,
            imageBitmap = mockk(),
        )
    }

    private fun mediaPreviewItem(contentUri: String): MediaPreviewItem {
        return MediaPreviewItem(
            contentUri = contentUri,
            contentType = "image/jpeg",
            isVideo = false,
        )
    }

    private companion object {
        private const val FLOAT_DELTA = 0.001f
    }
}
