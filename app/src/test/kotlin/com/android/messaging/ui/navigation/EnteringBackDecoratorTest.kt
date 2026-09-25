package com.android.messaging.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.android.messaging.testutil.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EnteringBackDecoratorTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun predictiveBack_afterACancelledSwipe_stillSeeksToThePreviousScreen() {
        val backStack = mutableStateListOf<NavKey>(TestKey(FIRST), TestKey(SECOND))
        var hasLateAnimation by mutableStateOf(false)
        composeTestRule.setContent {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberEnteringBackNavEntryDecorator(
                        canPop = backStack.size > 1,
                        topContentKey = NavEntry(backStack.last()) {}.contentKey,
                        onBack = { backStack.removeLastOrNull() },
                    ),
                ),
                entryProvider = { key ->
                    NavEntry(key) {
                        if (hasLateAnimation) {
                            LocalNavAnimatedContentScope.current.transition.animateFloat(
                                transitionSpec = { tween(durationMillis = LATE_ANIMATION_MILLIS) },
                                label = "late",
                            ) { if (it == EnterExitState.Visible) 1f else 0f }
                        }
                        Text(text = (key as TestKey).name)
                    }
                },
            )
        }

        swipeBack()
        composeTestRule.onNodeWithText(FIRST).assertExists()
        dispatch { onBackPressedDispatcher.dispatchOnBackCancelled() }
        composeTestRule.onNodeWithText(FIRST).assertDoesNotExist()

        // Growing the settled transition's duration makes Compose seek it again, which marks the
        // cancelled entry's `PostExit -> Visible` transition running, as happens on devices.
        hasLateAnimation = true
        composeTestRule.waitForIdle()

        swipeBack()
        composeTestRule.onNodeWithText(FIRST).assertExists()

        dispatch { onBackPressedDispatcher.onBackPressed() }
        assertThat(backStack.toList()).isEqualTo(listOf(TestKey(FIRST)))
    }

    @Test
    fun backSwipe_whileACancelPlaysBack_seeksToThePreviousScreen() {
        val backStack = mutableStateListOf<NavKey>(TestKey(FIRST), TestKey(SECOND))
        composeTestRule.setContent {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberEnteringBackNavEntryDecorator(
                        canPop = backStack.size > 1,
                        topContentKey = NavEntry(backStack.last()) {}.contentKey,
                        onBack = { backStack.removeLastOrNull() },
                    ),
                ),
                entryProvider = { key ->
                    NavEntry(key) { Text(text = (key as TestKey).name) }
                },
            )
        }

        swipeBack()
        composeTestRule.mainClock.autoAdvance = false
        dispatch { onBackPressedDispatcher.dispatchOnBackCancelled() }
        repeat(FRAMES_TO_SEEK) { composeTestRule.mainClock.advanceTimeByFrame() }
        // The revealed entry is entering until the cancel settles, and then goes away.
        swipeBack()
        repeat(FRAMES_TO_SETTLE) { composeTestRule.mainClock.advanceTimeByFrame() }

        composeTestRule.onNodeWithText(FIRST).assertExists()
        composeTestRule.mainClock.autoAdvance = true
        dispatch { onBackPressedDispatcher.onBackPressed() }
        assertThat(backStack.toList()).isEqualTo(listOf(TestKey(FIRST)))
    }

    @Test
    fun backSwipe_whileAnEntryOpens_popsItWithoutSeeking() {
        val backStack = mutableStateListOf<NavKey>(TestKey(FIRST))
        val transitions = mutableMapOf<String, Transition<EnterExitState>>()
        composeTestRule.setContent {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberEnteringBackNavEntryDecorator(
                        canPop = backStack.size > 1,
                        topContentKey = NavEntry(backStack.last()) {}.contentKey,
                        onBack = { backStack.removeLastOrNull() },
                    ),
                ),
                entryProvider = { key ->
                    NavEntry(key) {
                        val name = (key as TestKey).name
                        transitions[name] = LocalNavAnimatedContentScope.current.transition
                        Text(text = name)
                    }
                },
            )
        }

        composeTestRule.mainClock.autoAdvance = false
        backStack.add(TestKey(SECOND))
        composeTestRule.waitForIdle()
        repeat(MAX_FRAMES_TO_OPEN) {
            if (transitions[SECOND]?.isRunning != true) {
                composeTestRule.mainClock.advanceTimeByFrame()
            }
        }
        swipeBack()
        repeat(FRAMES_TO_SEEK) { composeTestRule.mainClock.advanceTimeByFrame() }

        // A seek would turn the opening entry around mid-swipe.
        assertThat(transitions.getValue(SECOND).targetState).isEqualTo(EnterExitState.Visible)
        composeTestRule.mainClock.autoAdvance = true
        dispatch { onBackPressedDispatcher.onBackPressed() }

        assertThat(backStack.toList()).isEqualTo(listOf(TestKey(FIRST)))
        composeTestRule.onNodeWithText(SECOND).assertDoesNotExist()
    }

    private fun swipeBack() {
        dispatch {
            onBackPressedDispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
            onBackPressedDispatcher.dispatchOnBackProgressed(backEvent(progress = 0.5f))
        }
    }

    private fun dispatch(block: ComponentActivity.() -> Unit) {
        composeTestRule.runOnUiThread { composeTestRule.activity.block() }
        composeTestRule.waitForIdle()
    }

    private fun backEvent(progress: Float): BackEventCompat {
        return BackEventCompat(
            touchX = 0f,
            touchY = 0f,
            progress = progress,
            swipeEdge = BackEventCompat.EDGE_LEFT,
        )
    }

    private data class TestKey(
        val name: String,
    ) : NavKey

    private companion object {
        const val FIRST = "first"
        const val SECOND = "second"
        const val LATE_ANIMATION_MILLIS = 5_000
        const val MAX_FRAMES_TO_OPEN = 5
        const val FRAMES_TO_SEEK = 3
        const val FRAMES_TO_SETTLE = 60
    }
}
