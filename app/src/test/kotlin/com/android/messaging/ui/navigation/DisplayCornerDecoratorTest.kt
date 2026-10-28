package com.android.messaging.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.common.components.ListDetailPaneSide
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayCornerDecoratorTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun listDetailEntry_whenTheWindowGainsAndLosesASecondPane_keepsItsState() {
        var showsTwoPanes by mutableStateOf(false)
        val entryStates = mutableSetOf<Any>()
        composeTestRule.setContent {
            NavDisplay(
                backStack = listOf(TestKey),
                onBack = {},
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberListDetailPaneNavEntryDecorator(showsTwoPanes = showsTwoPanes),
                    rememberDisplayCornerNavEntryDecorator(
                        topContentKey = NavEntry(TestKey) {}.contentKey,
                    ),
                ),
                entryProvider = { key ->
                    NavEntry(
                        key = key,
                        metadata = listDetailPaneMetadata(side = ListDetailPaneSide.End),
                    ) {
                        entryStates += remember { Any() }
                    }
                },
            )
        }

        showsTwoPanes = true
        composeTestRule.waitForIdle()
        showsTwoPanes = false
        composeTestRule.waitForIdle()

        assertThat(entryStates.size).isEqualTo(1)
    }

    private data object TestKey : NavKey
}
