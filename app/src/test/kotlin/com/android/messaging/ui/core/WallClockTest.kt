package com.android.messaging.ui.core

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.cash.turbine.test
import com.android.messaging.testutil.TestLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WallClockTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var lifecycleOwner: TestLifecycleOwner

    @Test
    fun wallClockMinuteTicks_midMinute_emitsNowThenOnEachMinuteBoundary() = runTest {
        val ticks = wallClockMinuteTicks(
            currentTimeMillis = { MID_MINUTE_MILLIS + testScheduler.currentTime },
        )

        ticks.test {
            assertEquals(MID_MINUTE_MILLIS, awaitItem())
            assertEquals(NEXT_MINUTE_MILLIS, awaitItem())
            assertEquals(NEXT_MINUTE_MILLIS + MINUTE_MILLIS, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun currentWallClockMillis_withoutProvider_readsSystemTime() {
        val beforeMillis = System.currentTimeMillis()
        var readMillis = 0L

        composeTestRule.setContent {
            readMillis = currentWallClockMillis()
        }
        composeTestRule.waitForIdle()

        assertTrue(readMillis in beforeMillis..System.currentTimeMillis())
    }

    @Test
    fun provideWallClock_newTick_updatesCurrentWallClockMillis() {
        val ticks = MutableStateFlow(FIRST_TICK_MILLIS)
        setWallClockContent(ticks = ticks)

        composeTestRule.onNodeWithText(FIRST_TICK_MILLIS.toString()).assertExists()

        ticks.value = SECOND_TICK_MILLIS

        composeTestRule.onNodeWithText(SECOND_TICK_MILLIS.toString()).assertExists()
    }

    @Test
    fun provideWallClock_inBackground_stopsCollectingTicks() {
        val ticks = MutableStateFlow(FIRST_TICK_MILLIS)
        setWallClockContent(ticks = ticks)

        assertEquals(1, ticks.subscriptionCount.value)

        moveTo(state = Lifecycle.State.CREATED)

        assertEquals(0, ticks.subscriptionCount.value)
    }

    @Test
    fun provideWallClock_backInForeground_readsFreshTime() {
        var clockMillis = FIRST_TICK_MILLIS
        setWallClockContent(ticks = flow { emit(clockMillis) })

        composeTestRule.onNodeWithText(FIRST_TICK_MILLIS.toString()).assertExists()

        moveTo(state = Lifecycle.State.CREATED)
        clockMillis = SECOND_TICK_MILLIS
        moveTo(state = Lifecycle.State.RESUMED)

        composeTestRule.onNodeWithText(SECOND_TICK_MILLIS.toString()).assertExists()
    }

    private fun setWallClockContent(ticks: Flow<Long>) {
        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                ProvideWallClock(ticks = ticks) {
                    Text(text = currentWallClockMillis().toString())
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun moveTo(state: Lifecycle.State) {
        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = state)
        }
        composeTestRule.waitForIdle()
    }

    private companion object {
        private const val MINUTE_MILLIS = 60_000L
        private const val FIRST_TICK_MILLIS = 1_800_000_000_000L
        private const val SECOND_TICK_MILLIS = FIRST_TICK_MILLIS + MINUTE_MILLIS
        private const val MID_MINUTE_MILLIS = FIRST_TICK_MILLIS + 15_000L
        private const val NEXT_MINUTE_MILLIS = FIRST_TICK_MILLIS + MINUTE_MILLIS
    }
}
