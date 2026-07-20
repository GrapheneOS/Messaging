package com.android.messaging.ui.navigation

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.onboarding.navigation.OnboardingNavKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppBackStackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var startDestinations: List<NavKey> = listOf(ConversationListNavKey)
    private lateinit var backStack: NavBackStack<NavKey>

    @Test
    fun restoredBackStackIsDroppedWhenStartDestinationChanges() {
        restoreWithStartDestinations(restoredStartDestinations = listOf(OnboardingNavKey))

        assertEquals(listOf(OnboardingNavKey), backStack.toList())
    }

    @Test
    fun restoredBackStackIsKeptWhenStartDestinationIsTheSame() {
        restoreWithStartDestinations(restoredStartDestinations = listOf(ConversationListNavKey))

        assertEquals(listOf(ConversationListNavKey, OnboardingNavKey), backStack.toList())
    }

    private fun restoreWithStartDestinations(restoredStartDestinations: List<NavKey>) {
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            backStack = rememberAppBackStack(startDestinations = startDestinations)
        }

        composeTestRule.runOnIdle {
            backStack.add(OnboardingNavKey)
        }

        startDestinations = restoredStartDestinations
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()
    }
}
