package com.android.messaging.ui.host

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavKey
import com.android.messaging.testutil.TestLifecycleOwner
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.navigation.NavigationReducerImpl
import com.android.messaging.ui.navigation.NavigatorImpl
import com.android.messaging.ui.onboarding.navigation.OnboardingNavKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppResumeEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val backStack = mutableStateListOf<NavKey>(ConversationListNavKey)

    private lateinit var lifecycleOwner: TestLifecycleOwner
    private var shouldShowOnboarding = false
    private var resumeCount = 0

    @Test
    fun resumesAppWhenOnboardingIsNotNeeded() {
        setContent()
        moveToResumed()

        assertEquals(1, resumeCount)
        assertEquals(listOf(ConversationListNavKey), backStack.toList())
    }

    @Test
    fun resetsToOnboardingWhenOnboardingIsNeeded() {
        shouldShowOnboarding = true

        setContent()
        moveToResumed()

        assertEquals(0, resumeCount)
        assertEquals(listOf(OnboardingNavKey), backStack.toList())
    }

    @Test
    fun ignoresResumeWhileOnboardingIsTheCurrentDestination() {
        backStack[0] = OnboardingNavKey

        setContent()
        moveToResumed()

        assertEquals(0, resumeCount)
        assertEquals(listOf(OnboardingNavKey), backStack.toList())
    }

    @Test
    fun resumesAppWhenOnboardingLeavesTheBackStack() {
        backStack[0] = OnboardingNavKey

        setContent()
        moveToResumed()

        assertEquals(0, resumeCount)

        composeTestRule.runOnIdle {
            backStack[0] = ConversationListNavKey
        }
        composeTestRule.waitForIdle()

        assertEquals(1, resumeCount)
    }

    private fun setContent() {
        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(
                initialState = Lifecycle.State.CREATED,
            )
        }

        val navigator = NavigatorImpl(
            backStack = backStack,
            navigationReducer = NavigationReducerImpl(),
            onFinish = {},
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AppResumeEffect(
                    backStack = backStack,
                    navigator = navigator,
                    shouldShowOnboarding = { shouldShowOnboarding },
                    onAppResumed = { resumeCount += 1 },
                )
            }
        }
    }

    private fun moveToResumed() {
        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.RESUMED)
        }
        composeTestRule.waitForIdle()
    }
}
