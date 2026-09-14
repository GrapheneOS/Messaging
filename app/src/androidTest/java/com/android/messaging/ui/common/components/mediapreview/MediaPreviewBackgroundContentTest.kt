package com.android.messaging.ui.common.components.mediapreview

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewTheme
import io.mockk.coEvery
import io.mockk.mockk
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class MediaPreviewBackgroundContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val bitmapLoader = mockk<suspend (MediaPreviewItem) -> Bitmap?>()
    private val deferredBitmapsByContentUri =
        ConcurrentHashMap<String, CompletableDeferred<Bitmap?>>()
    private val returnedContentUris = ConcurrentHashMap.newKeySet<String>()

    init {
        coEvery { bitmapLoader.invoke(any()) } coAnswers {
            val item = firstArg<MediaPreviewItem>()
            deferredBitmap(contentUri = item.contentUri)
                .await()
                .also { returnedContentUris += item.contentUri }
        }
    }

    @Test
    fun lateAndRapidBitmapChanges_holdThenFadeWithoutFallbackFlash() {
        val pagerScrollController = ControlledPagerScrollController()
        val items = persistentListOf(
            mediaPreviewItem(contentUri = "first"),
            mediaPreviewItem(contentUri = "second"),
            mediaPreviewItem(contentUri = "third"),
        )
        lateinit var pagerState: PagerState
        composeRule.mainClock.autoAdvance = false

        try {
            composeRule.setContent {
                MessagingPreviewTheme {
                    val currentPagerState = rememberPagerState(pageCount = { items.size })
                    SideEffect {
                        pagerState = currentPagerState
                    }
                    LaunchedEffect(currentPagerState, pagerScrollController) {
                        pagerScrollController.run(pagerState = currentPagerState)
                    }

                    Box(
                        modifier = Modifier
                            .size(size = 100.dp)
                            .testTag(tag = BACKGROUND_TEST_TAG),
                    ) {
                        MediaPreviewBackground(
                            modifier = Modifier.fillMaxSize(),
                            items = items,
                            pagerState = currentPagerState,
                            bitmapLoader = bitmapLoader,
                        )
                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            state = currentPagerState,
                        ) { _ ->
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            val fallbackColor = captureBackgroundColor()
            completeBitmapLoader(
                contentUri = "first",
                color = AndroidColor.RED,
            )
            waitForBitmapLoader(contentUri = "first")
            composeRule.waitForIdle()
            assertBackgroundColor(expectedColor = fallbackColor)

            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS / 2)
            val imageMidpointColor = Color.Red
                .copy(alpha = 0.5f)
                .compositeOver(background = fallbackColor)
            val expectedInitialMidpointColor = Color.Black
                .copy(alpha = 0.25f)
                .compositeOver(background = imageMidpointColor)
            assertBackgroundColor(expectedColor = expectedInitialMidpointColor)

            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS / 2)
            assertBackgroundColor(expectedColor = Color(red = 0.5f, green = 0f, blue = 0f))

            val firstScrollSession = pagerScrollController.beginScroll()
            waitForScrollSession(firstScrollSession)
            setPagerPosition(
                session = firstScrollSession,
                page = 0,
                pageOffsetFraction = 0.25f,
            )
            assertBackgroundColor(expectedColor = Color(red = 0.5f, green = 0f, blue = 0f))

            completeBitmapLoader(
                contentUri = "second",
                color = AndroidColor.BLUE,
            )
            waitForBitmapLoader(contentUri = "second")
            composeRule.waitForIdle()
            assertBackgroundColor(expectedColor = Color(red = 0.5f, green = 0f, blue = 0f))

            setPagerPosition(
                session = firstScrollSession,
                page = 1,
                pageOffsetFraction = 0f,
            )
            firstScrollSession.finish()
            waitForScrollToFinish(pagerState = pagerState)
            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS / 2)
            assertBackgroundColor(expectedColor = Color(red = 0.25f, green = 0f, blue = 0.25f))

            val secondScrollSession = pagerScrollController.beginScroll()
            waitForScrollSession(secondScrollSession)
            setPagerPosition(
                session = secondScrollSession,
                page = 2,
                pageOffsetFraction = 0f,
            )
            completeBitmapLoader(
                contentUri = "third",
                color = AndroidColor.GREEN,
            )
            waitForBitmapLoader(contentUri = "third")
            secondScrollSession.finish()
            waitForScrollToFinish(pagerState = pagerState)
            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS / 2)
            assertConstantLuminancePrimaryBlend()

            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS * 2)
            assertBackgroundColor(expectedColor = Color(red = 0f, green = 0.5f, blue = 0f))
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun interactiveBlend_tracksOffsetsAcrossCurrentPageFlip() {
        val pagerScrollController = ControlledPagerScrollController()
        val items = persistentListOf(
            mediaPreviewItem(contentUri = "first"),
            mediaPreviewItem(contentUri = "second"),
        )
        lateinit var pagerState: PagerState
        composeRule.mainClock.autoAdvance = false

        try {
            composeRule.setContent {
                MessagingPreviewTheme {
                    val currentPagerState = rememberPagerState(pageCount = { items.size })
                    SideEffect {
                        pagerState = currentPagerState
                    }
                    LaunchedEffect(currentPagerState, pagerScrollController) {
                        pagerScrollController.run(pagerState = currentPagerState)
                    }

                    Box(
                        modifier = Modifier
                            .size(size = 100.dp)
                            .testTag(tag = BACKGROUND_TEST_TAG),
                    ) {
                        MediaPreviewBackground(
                            modifier = Modifier.fillMaxSize(),
                            items = items,
                            pagerState = currentPagerState,
                            bitmapLoader = bitmapLoader,
                        )
                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            state = currentPagerState,
                        ) { _ ->
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            completeBitmapLoader(contentUri = "first", color = AndroidColor.RED)
            completeBitmapLoader(contentUri = "second", color = AndroidColor.BLUE)
            waitForBitmapLoader(contentUri = "first")
            waitForBitmapLoader(contentUri = "second")
            composeRule.waitForIdle()
            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS * 2)
            composeRule.waitForIdle()

            val scrollSession = pagerScrollController.beginScroll()
            waitForScrollSession(scrollSession)
            setPagerPosition(
                session = scrollSession,
                page = 0,
                pageOffsetFraction = 0.49f,
            )
            assertPagerPosition(
                pagerState = pagerState,
                expectedPage = 0,
                expectedPageOffsetFraction = 0.49f,
            )
            assertBackgroundColor(expectedColor = Color(red = 0.255f, green = 0f, blue = 0.245f))

            setPagerPosition(
                session = scrollSession,
                page = 1,
                pageOffsetFraction = -0.49f,
            )
            assertBackgroundColor(expectedColor = Color(red = 0.245f, green = 0f, blue = 0.255f))

            setPagerPosition(
                session = scrollSession,
                page = 1,
                pageOffsetFraction = -0.25f,
            )
            assertBackgroundColor(expectedColor = Color(red = 0.125f, green = 0f, blue = 0.375f))

            setPagerPosition(
                session = scrollSession,
                page = 0,
                pageOffsetFraction = 0.25f,
            )
            assertBackgroundColor(expectedColor = Color(red = 0.375f, green = 0f, blue = 0.125f))

            scrollSession.finish()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun readyScrollSettlesWithoutReturningToPreviousFrame() {
        val pagerScrollController = ControlledPagerScrollController()
        val items = persistentListOf(
            mediaPreviewItem(contentUri = "first"),
            mediaPreviewItem(contentUri = "second"),
        )
        lateinit var pagerState: PagerState
        composeRule.mainClock.autoAdvance = false

        try {
            composeRule.setContent {
                MessagingPreviewTheme {
                    val currentPagerState = rememberPagerState(pageCount = { items.size })
                    SideEffect {
                        pagerState = currentPagerState
                    }
                    LaunchedEffect(currentPagerState, pagerScrollController) {
                        pagerScrollController.run(pagerState = currentPagerState)
                    }

                    Box(
                        modifier = Modifier
                            .size(size = 100.dp)
                            .testTag(tag = BACKGROUND_TEST_TAG),
                    ) {
                        MediaPreviewBackground(
                            modifier = Modifier.fillMaxSize(),
                            items = items,
                            pagerState = currentPagerState,
                            bitmapLoader = bitmapLoader,
                        )
                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            state = currentPagerState,
                        ) { _ ->
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            completeBitmapLoader(contentUri = "first", color = AndroidColor.RED)
            completeBitmapLoader(contentUri = "second", color = AndroidColor.BLUE)
            waitForBitmapLoader(contentUri = "first")
            waitForBitmapLoader(contentUri = "second")
            composeRule.waitForIdle()
            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS * 2)

            val scrollSession = pagerScrollController.beginScroll()
            waitForScrollSession(scrollSession)
            setPagerPosition(
                session = scrollSession,
                page = 0,
                pageOffsetFraction = 0.49f,
            )
            assertBackgroundColor(expectedColor = Color(red = 0.255f, green = 0f, blue = 0.245f))

            setPagerPositionAndFinish(
                session = scrollSession,
                pagerState = pagerState,
                page = 1,
                pageOffsetFraction = 0f,
            )
            assertBackgroundColor(expectedColor = Color(red = 0f, green = 0f, blue = 0.5f))

            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS / 2)
            assertBackgroundColor(expectedColor = Color(red = 0f, green = 0f, blue = 0.5f))
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun middlePageBoundary_doesNotRequireInvisibleFollowingFrame() {
        val pagerScrollController = ControlledPagerScrollController()
        val items = persistentListOf(
            mediaPreviewItem(contentUri = "first"),
            mediaPreviewItem(contentUri = "second"),
            mediaPreviewItem(contentUri = "third"),
        )
        lateinit var pagerState: PagerState
        composeRule.mainClock.autoAdvance = false

        try {
            composeRule.setContent {
                MessagingPreviewTheme {
                    val currentPagerState = rememberPagerState(pageCount = { items.size })
                    SideEffect {
                        pagerState = currentPagerState
                    }
                    LaunchedEffect(currentPagerState, pagerScrollController) {
                        pagerScrollController.run(pagerState = currentPagerState)
                    }

                    Box(
                        modifier = Modifier
                            .size(size = 100.dp)
                            .testTag(tag = BACKGROUND_TEST_TAG),
                    ) {
                        MediaPreviewBackground(
                            modifier = Modifier.fillMaxSize(),
                            items = items,
                            pagerState = currentPagerState,
                            bitmapLoader = bitmapLoader,
                        )
                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            state = currentPagerState,
                        ) { _ ->
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            completeBitmapLoader(contentUri = "first", color = AndroidColor.RED)
            completeBitmapLoader(contentUri = "second", color = AndroidColor.BLUE)
            waitForBitmapLoader(contentUri = "first")
            waitForBitmapLoader(contentUri = "second")
            composeRule.waitForIdle()
            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS * 2)

            val scrollSession = pagerScrollController.beginScroll()
            waitForScrollSession(scrollSession)
            setPagerPosition(
                session = scrollSession,
                page = 0,
                pageOffsetFraction = 0.49f,
            )
            assertBackgroundColor(expectedColor = Color(red = 0.255f, green = 0f, blue = 0.245f))

            setPagerPosition(
                session = scrollSession,
                page = 1,
                pageOffsetFraction = 0f,
            )
            assertBackgroundColor(expectedColor = Color(red = 0f, green = 0f, blue = 0.5f))

            scrollSession.finish()
            waitForScrollToFinish(pagerState = pagerState)
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            assertBackgroundColor(expectedColor = Color(red = 0f, green = 0f, blue = 0.5f))

            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS / 2)
            assertBackgroundColor(expectedColor = Color(red = 0f, green = 0f, blue = 0.5f))
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun emptyDuringRecovery_refillRestartsInitialFade() {
        var items by mutableStateOf<ImmutableList<MediaPreviewItem>>(
            value = persistentListOf(mediaPreviewItem(contentUri = "first")),
        )
        composeRule.mainClock.autoAdvance = false

        try {
            composeRule.setContent {
                MessagingPreviewTheme {
                    val pagerState = rememberPagerState(pageCount = { items.size })

                    Box(
                        modifier = Modifier
                            .size(size = 100.dp)
                            .testTag(tag = BACKGROUND_TEST_TAG),
                    ) {
                        MediaPreviewBackground(
                            modifier = Modifier.fillMaxSize(),
                            items = items,
                            pagerState = pagerState,
                            bitmapLoader = bitmapLoader,
                        )
                    }
                }
            }

            val fallbackColor = captureBackgroundColor()
            completeBitmapLoader(contentUri = "first", color = AndroidColor.RED)
            waitForBitmapLoader(contentUri = "first")
            composeRule.waitForIdle()
            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS / 2)

            composeRule.runOnIdle {
                items = persistentListOf()
            }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            assertBackgroundColor(expectedColor = fallbackColor)

            composeRule.runOnIdle {
                items = persistentListOf(mediaPreviewItem(contentUri = "second"))
            }
            composeRule.mainClock.advanceTimeByFrame()
            completeBitmapLoader(contentUri = "second", color = AndroidColor.BLUE)
            waitForBitmapLoader(contentUri = "second")
            composeRule.waitForIdle()
            assertBackgroundColor(expectedColor = fallbackColor)

            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS / 2)
            val imageMidpointColor = Color.Blue
                .copy(alpha = 0.5f)
                .compositeOver(background = fallbackColor)
            val expectedInitialMidpointColor = Color.Black
                .copy(alpha = 0.25f)
                .compositeOver(background = imageMidpointColor)
            assertBackgroundColor(expectedColor = expectedInitialMidpointColor)

            composeRule.mainClock.advanceTimeBy(milliseconds = RECOVERY_ANIMATION_MILLIS / 2)
            assertBackgroundColor(expectedColor = Color(red = 0f, green = 0f, blue = 0.5f))
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun waitForScrollSession(session: ControlledPagerScrollSession) {
        composeRule.waitUntil {
            session.isStarted
        }
    }

    private fun waitForBitmapLoader(contentUri: String) {
        composeRule.waitUntil {
            contentUri in returnedContentUris
        }
    }

    private fun completeBitmapLoader(contentUri: String, color: Int) {
        deferredBitmap(contentUri = contentUri).complete(solidBitmap(color = color))
    }

    private fun deferredBitmap(contentUri: String): CompletableDeferred<Bitmap?> {
        return deferredBitmapsByContentUri.computeIfAbsent(contentUri) {
            CompletableDeferred()
        }
    }

    private fun solidBitmap(color: Int): Bitmap {
        return Bitmap.createBitmap(
            intArrayOf(color),
            1,
            1,
            Bitmap.Config.ARGB_8888,
        )
    }

    private fun setPagerPosition(
        session: ControlledPagerScrollSession,
        page: Int,
        pageOffsetFraction: Float,
    ) {
        val positionUpdate = session.setPosition(
            page = page,
            pageOffsetFraction = pageOffsetFraction,
        )
        composeRule.waitUntil {
            positionUpdate.isCompleted
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun setPagerPositionAndFinish(
        session: ControlledPagerScrollSession,
        pagerState: PagerState,
        page: Int,
        pageOffsetFraction: Float,
    ) {
        val positionUpdate = session.setPosition(
            page = page,
            pageOffsetFraction = pageOffsetFraction,
        )
        session.finish()
        composeRule.waitUntil {
            positionUpdate.isCompleted && !pagerState.isScrollInProgress
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun waitForScrollToFinish(pagerState: PagerState) {
        composeRule.waitUntil {
            !pagerState.isScrollInProgress
        }
    }

    private fun assertPagerPosition(
        pagerState: PagerState,
        expectedPage: Int,
        expectedPageOffsetFraction: Float,
    ) {
        composeRule.runOnIdle {
            assertTrue(pagerState.isScrollInProgress)
            assertTrue(pagerState.currentPage == expectedPage)
            assertTrue(
                abs(pagerState.currentPageOffsetFraction - expectedPageOffsetFraction) <=
                    FLOAT_POSITION_TOLERANCE,
            )
        }
    }

    private fun assertBackgroundColor(expectedColor: Color) {
        val centerColor = captureBackgroundColor()
        val redMatches = abs(centerColor.red - expectedColor.red) <=
            COLOR_COMPONENT_TOLERANCE
        val greenMatches = abs(centerColor.green - expectedColor.green) <=
            COLOR_COMPONENT_TOLERANCE
        val blueMatches = abs(centerColor.blue - expectedColor.blue) <=
            COLOR_COMPONENT_TOLERANCE

        assertTrue(
            "Expected $expectedColor but rendered $centerColor",
            redMatches && greenMatches && blueMatches,
        )
    }

    private fun captureBackgroundColor(): Color {
        val image = composeRule
            .onNodeWithTag(testTag = BACKGROUND_TEST_TAG)
            .captureToImage()
        return image.toPixelMap()[image.width / 2, image.height / 2]
    }

    private fun assertConstantLuminancePrimaryBlend() {
        val image = composeRule
            .onNodeWithTag(testTag = BACKGROUND_TEST_TAG)
            .captureToImage()
        val centerColor = image.toPixelMap()[image.width / 2, image.height / 2]
        val totalPrimaryIntensity = centerColor.red + centerColor.green + centerColor.blue

        assertTrue(
            "Expected a constant-luminance primary-color blend but rendered $centerColor",
            abs(totalPrimaryIntensity - 0.5f) <= COLOR_COMPONENT_TOLERANCE,
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
        private const val BACKGROUND_TEST_TAG = "media-preview-background"
        private const val COLOR_COMPONENT_TOLERANCE = 0.08f
        private const val FLOAT_POSITION_TOLERANCE = 0.001f
        private const val RECOVERY_ANIMATION_MILLIS = 180L
    }
}

private class ControlledPagerScrollController {
    private val sessions = Channel<ControlledPagerScrollSession>(capacity = Channel.UNLIMITED)

    suspend fun run(pagerState: PagerState) {
        for (session in sessions) {
            pagerState.scroll {
                session.markStarted()
                for (positionUpdate in session.positionUpdates) {
                    with(pagerState) {
                        updateCurrentPage(
                            page = positionUpdate.page,
                            pageOffsetFraction = positionUpdate.pageOffsetFraction,
                        )
                    }
                    positionUpdate.markCompleted()
                }
            }
        }
    }

    fun beginScroll(): ControlledPagerScrollSession {
        return ControlledPagerScrollSession().also { session ->
            check(sessions.trySend(session).isSuccess)
        }
    }
}

private class ControlledPagerScrollSession {
    private val started = CompletableDeferred<Unit>()
    val positionUpdates = Channel<ControlledPagerPositionUpdate>(capacity = Channel.UNLIMITED)
    val isStarted: Boolean
        get() = started.isCompleted

    fun markStarted() {
        started.complete(Unit)
    }

    fun setPosition(
        page: Int,
        pageOffsetFraction: Float,
    ): ControlledPagerPositionUpdate {
        return ControlledPagerPositionUpdate(
            page = page,
            pageOffsetFraction = pageOffsetFraction,
        ).also { positionUpdate ->
            check(positionUpdates.trySend(positionUpdate).isSuccess)
        }
    }

    fun finish() {
        positionUpdates.close()
    }
}

private class ControlledPagerPositionUpdate(
    val page: Int,
    val pageOffsetFraction: Float,
) {
    private val completed = CompletableDeferred<Unit>()
    val isCompleted: Boolean
        get() = completed.isCompleted

    fun markCompleted() {
        completed.complete(Unit)
    }
}
