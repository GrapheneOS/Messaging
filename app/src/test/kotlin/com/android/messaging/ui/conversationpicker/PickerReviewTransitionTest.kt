package com.android.messaging.ui.conversationpicker

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.android.messaging.ui.common.components.PredictiveBackSwipes
import com.android.messaging.ui.common.components.PredictiveBackSwipes.Companion.START_Y
import com.android.messaging.ui.common.components.PredictiveBackSwipes.Companion.assertEasesBack
import com.android.messaging.ui.common.components.PredictiveBackSwipes.Companion.assertFollows
import com.android.messaging.ui.common.components.predictiveBackPage
import com.android.messaging.ui.common.components.rememberPredictiveBackContentTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class PickerReviewTransitionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val swipes = PredictiveBackSwipes(
        composeTestRule = composeTestRule,
        closingPageTag = REVIEW,
    )
    private var isReviewing by mutableStateOf(true)

    @Before
    fun setUp() {
        composeTestRule.setContent {
            val reviewTransition = remember { PickerReviewTransition(isReviewing = isReviewing) }
            val transition = rememberTransition(transitionState = reviewTransition.transitionState)
            val predictiveBackContentTransform = rememberPredictiveBackContentTransform()

            LaunchedEffect(isReviewing) {
                reviewTransition.settleTo(isReviewing = isReviewing)
            }
            PickerReviewBackHandler(
                reviewTransition = reviewTransition,
                enabled = isReviewing,
                onDismissed = { isReviewing = false },
            )
            transition.AnimatedContent(
                transitionSpec = {
                    reviewTransition.stepContentTransform(
                        isForward = targetState,
                        predictiveBackContentTransform = predictiveBackContentTransform,
                    )
                },
            ) { reviewing ->
                val tag = if (reviewing) REVIEW else PICKER
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .predictiveBackPage(
                            transition = this.transition,
                            isOpen = reviewing == isReviewing,
                        )
                        .background(color = if (reviewing) Color.Red else Color.Blue)
                        .testTag(tag),
                )
            }
        }
    }

    @Test
    fun cancelledReview_easesBackWhileThePickerStaysFullSize() {
        val touchDeltaY = swipes.pageHeight / 4f
        swipes.start(touchDeltaY = touchDeltaY)

        composeTestRule.mainClock.autoAdvance = false
        swipes.dispatch { onBackPressedDispatcher.dispatchOnBackCancelled() }
        val pickerScales = mutableListOf<Float>()
        val frames = swipes.advanceFrames {
            swipes.page(tag = PICKER)?.let { pickerScales += it.scale }
        }

        assertEasesBack(touchDeltaY = touchDeltaY, frames = frames)
        // Reversing the transition instead would give the revealed picker the closing page's exit.
        assertTrue("picker scales $pickerScales", pickerScales.isNotEmpty())
        for (scale in pickerScales) {
            assertEquals("picker scales $pickerScales", 1f, scale, SCALE_TOLERANCE)
        }
        assertTrue(isReviewing)
    }

    @Test
    fun swipeDuringACancel_takesThePageOver() {
        val touchDeltaY = swipes.pageHeight / 4f
        swipes.start(touchDeltaY = touchDeltaY)
        val swipeScale = checkNotNull(swipes.page()).scale

        composeTestRule.mainClock.autoAdvance = false
        swipes.dispatch { onBackPressedDispatcher.dispatchOnBackCancelled() }
        swipes.advanceFrames(count = 2)
        swipes.start(touchDeltaY = -touchDeltaY, startY = START_Y + touchDeltaY)
        val page = swipes.advanceFrames(count = FRAMES_TO_SEEK).last()

        assertEquals(swipeScale, page.scale, SCALE_TOLERANCE)
        assertFollows(touchDeltaY = -touchDeltaY, page = page)

        swipes.dispatch { onBackPressedDispatcher.onBackPressed() }
        assertFalse(isReviewing)
    }

    // Holding Back with three-button navigation plays the swipe to its end and holds it there.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun heldReview_staysUntilCancelledAndGrowsBack() {
        swipes.start(touchDeltaY = 0f, progress = 1f)
        composeTestRule.mainClock.autoAdvance = false
        // Held past the transition's length.
        swipes.advanceFrames()

        assertEquals(TARGET_SCALE, checkNotNull(swipes.page()).scale, SCALE_TOLERANCE)
        assertEquals(1f, swipes.closingPageAlpha(), ALPHA_TOLERANCE)

        swipes.dispatch { onBackPressedDispatcher.dispatchOnBackCancelled() }
        val alphas = mutableListOf<Float>()
        val scales = swipes.advanceFrames { alphas += swipes.closingPageAlpha() }.map { it.scale }

        assertTrue("scales $scales", scales.count { it in GROWING_SCALES } >= MIN_EASING_FRAMES)
        for ((previous, next) in scales.zipWithNext()) {
            assertTrue("scales $scales", next >= previous - SCALE_TOLERANCE)
        }
        assertEquals(1f, scales.last(), SCALE_TOLERANCE)
        for (alpha in alphas) {
            assertEquals("alphas $alphas", 1f, alpha, ALPHA_TOLERANCE)
        }
        assertTrue(isReviewing)
    }

    private companion object {
        const val REVIEW = "review"
        const val PICKER = "picker"
        const val SCALE_TOLERANCE = 0.001f
        const val FRAMES_TO_SEEK = 2
        const val TARGET_SCALE = 0.85f
        const val ALPHA_TOLERANCE = 0.01f
        const val MIN_EASING_FRAMES = 3
        val GROWING_SCALES = TARGET_SCALE + SCALE_TOLERANCE..1f - SCALE_TOLERANCE
    }
}
