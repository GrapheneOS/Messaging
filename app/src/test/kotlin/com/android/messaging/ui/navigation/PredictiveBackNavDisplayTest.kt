package com.android.messaging.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.common.components.PredictiveBackSwipes
import com.android.messaging.ui.common.components.PredictiveBackSwipes.Companion.PIXEL_TOLERANCE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class PredictiveBackNavDisplayTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val swipes = PredictiveBackSwipes(
        composeTestRule = composeTestRule,
        closingPageTag = SECOND,
    )
    private val backStack = mutableStateListOf<NavKey>(TestKey(FIRST), TestKey(SECOND))
    private val transitions = mutableMapOf<String, Transition<EnterExitState>>()

    @Before
    fun setUp() {
        composeTestRule.setContent {
            AppNavDisplay(
                backStack = backStack,
                entryProvider = { key ->
                    NavEntry(key) {
                        val name = (key as TestKey).name
                        transitions[name] = LocalNavAnimatedContentScope.current.transition
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = colors.getValue(name))
                                .testTag(name),
                        )
                    }
                },
                onBack = { backStack.removeLastOrNull() },
                sceneStrategies = rememberAppSceneStrategies(additionalStrategies = emptyList()),
                showsTwoPanes = false,
            )
        }
    }

    // Holding Back with three-button navigation plays the swipe to its end and holds it there.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun heldSwipe_keepsBothPagesInPlace() {
        swipes.start(progress = 1f)

        assertEquals(TARGET_SCALE, checkNotNull(swipes.page()).scale, SCALE_TOLERANCE)
        assertEquals(1f, swipes.closingPageAlpha(), TOLERANCE)
        assertEquals(-revealOffsetPx(), revealedPageLeft(), PIXEL_TOLERANCE)
        assertEquals(1f - LIGHT_SCRIM_ALPHA, revealedPageBrightness(), TOLERANCE)
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun heldSwipe_growsBackWhenCancelled() {
        swipes.start(progress = 1f)

        composeTestRule.mainClock.autoAdvance = false
        swipes.dispatch { onBackPressedDispatcher.dispatchOnBackCancelled() }
        val alphas = mutableListOf<Float>()
        val frames = swipes.advanceFrames {
            if (swipes.page() != null) {
                alphas += swipes.closingPageAlpha()
            }
        }

        val scales = frames.map { it.scale }
        assertTrue("scales $scales", scales.count { it in GROWING_SCALES } >= MIN_EASING_FRAMES)
        for ((previous, next) in scales.zipWithNext()) {
            assertTrue("scales $scales", next >= previous - SCALE_TOLERANCE)
        }
        assertEquals(1f, scales.last(), SCALE_TOLERANCE)
        for (alpha in alphas) {
            assertEquals("alphas $alphas", 1f, alpha, TOLERANCE)
        }
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun heldSwipe_settlesTheRevealedPageWhenReleased() {
        swipes.start(progress = 1f)

        composeTestRule.mainClock.autoAdvance = false
        swipes.dispatch { onBackPressedDispatcher.onBackPressed() }
        val lefts = mutableListOf<Float>()
        val brightnesses = mutableListOf<Float>()
        swipes.advanceFrames {
            lefts += revealedPageLeft()
            brightnesses += revealedPageBrightness()
        }

        assertThat(backStack.toList()).isEqualTo(listOf(TestKey(FIRST)))
        val revealOffsetPx = revealOffsetPx()
        val settlingLefts = lefts.count { it in -revealOffsetPx + 1f..-1f }
        assertTrue("lefts $lefts", settlingLefts >= MIN_EASING_FRAMES)
        for ((previous, next) in lefts.zipWithNext()) {
            assertTrue("lefts $lefts", next >= previous - PIXEL_TOLERANCE)
        }
        for ((previous, next) in brightnesses.zipWithNext()) {
            assertTrue("brightnesses $brightnesses", next >= previous - TOLERANCE)
        }
        assertEquals(0f, lefts.last(), PIXEL_TOLERANCE)
        assertEquals(1f, brightnesses.last(), TOLERANCE)
    }

    // The next swipe mustn't hold the fade of the page this one is still closing.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun committedPage_keepsFading_whenAnotherSwipeStarts() {
        swipes.start()
        composeTestRule.mainClock.autoAdvance = false
        swipes.dispatch { onBackPressedDispatcher.onBackPressed() }

        val alphas = mutableListOf<Float>()
        val recordAlpha = {
            if (swipes.page() != null) {
                alphas += swipes.closingPageAlpha()
            }
        }
        swipes.advanceFrames(count = FRAMES_BEFORE_NEXT_SWIPE, onFrame = recordAlpha)
        swipes.start()
        swipes.advanceFrames(onFrame = recordAlpha)

        assertTrue("alphas $alphas", alphas.size > FRAMES_BEFORE_NEXT_SWIPE)
        assertTrue("alphas $alphas", alphas.any { it in MIN_FADING_ALPHA..MAX_FADING_ALPHA })
        for ((previous, next) in alphas.zipWithNext()) {
            assertTrue("alphas $alphas", next <= previous + TOLERANCE)
        }
        assertThat(swipes.page()).isNull()
    }

    @Test
    fun plainPop_keepsTheAppTransitionLength() {
        composeTestRule.mainClock.autoAdvance = false
        swipes.dispatch { onBackPressedDispatcher.onBackPressed() }
        repeat(MAX_FRAMES_TO_START) {
            if (!transitions.getValue(SECOND).isRunning) {
                composeTestRule.mainClock.advanceTimeByFrame()
            }
        }

        assertThat(transitions.getValue(SECOND).totalDurationNanos)
            .isEqualTo(APP_TRANSITION_MILLIS * NANOS_PER_MILLI)
    }

    private fun revealedPageLeft(): Float {
        return checkNotNull(swipes.bounds(FIRST)).left
    }

    // Left of the closing page, where only the revealed page and its scrim are drawn.
    private fun revealedPageBrightness(): Float {
        return swipes.pixel(x = 1f, y = checkNotNull(swipes.bounds(FIRST)).center.y).blue
    }

    private fun revealOffsetPx(): Float {
        return with(composeTestRule.density) { REVEAL_OFFSET.toPx() }
    }

    private data class TestKey(
        val name: String,
    ) : NavKey

    private companion object {
        const val FIRST = "first"
        const val SECOND = "second"
        const val FRAMES_BEFORE_NEXT_SWIPE = 5
        const val SCALE_TOLERANCE = 0.001f
        const val TARGET_SCALE = 0.85f
        const val MIN_EASING_FRAMES = 3
        const val MIN_FADING_ALPHA = 0.1f
        const val MAX_FADING_ALPHA = 0.9f
        const val TOLERANCE = 0.01f
        const val MAX_FRAMES_TO_START = 5
        const val APP_TRANSITION_MILLIS = 350L
        const val NANOS_PER_MILLI = 1_000_000L
        const val LIGHT_SCRIM_ALPHA = 0.2f
        val REVEAL_OFFSET = 96.dp
        val GROWING_SCALES = TARGET_SCALE + SCALE_TOLERANCE..1f - SCALE_TOLERANCE
        val colors = mapOf(FIRST to Color.Blue, SECOND to Color.Red)
    }
}
