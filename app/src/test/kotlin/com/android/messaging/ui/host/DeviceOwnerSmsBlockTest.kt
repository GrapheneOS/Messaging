package com.android.messaging.ui.host

import android.os.Process
import android.os.UserManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.testutil.TestLifecycleOwner
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class DeviceOwnerSmsBlockTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val message = targetContext.getString(R.string.requires_sms_permissions_message)
    private val closeText = targetContext.getString(R.string.requires_sms_permissions_close_button)

    private lateinit var lifecycleOwner: TestLifecycleOwner
    private var closeCount = 0

    @Test
    fun blocksTheAppWhileSmsIsDisallowed() {
        setSmsDisallowed(isDisallowed = true)

        setContent()
        moveTo(state = Lifecycle.State.RESUMED)

        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun showsNothingWhileSmsIsAllowed() {
        setContent()
        moveTo(state = Lifecycle.State.RESUMED)

        composeTestRule.onNodeWithText(message).assertDoesNotExist()
    }

    @Test
    fun checksTheRestrictionAgainOnEachResume() {
        setContent()
        moveTo(state = Lifecycle.State.RESUMED)

        setSmsDisallowed(isDisallowed = true)
        moveTo(state = Lifecycle.State.STARTED)
        moveTo(state = Lifecycle.State.RESUMED)

        composeTestRule.onNodeWithText(message).assertIsDisplayed()

        setSmsDisallowed(isDisallowed = false)
        moveTo(state = Lifecycle.State.STARTED)
        moveTo(state = Lifecycle.State.RESUMED)

        composeTestRule.onNodeWithText(message).assertDoesNotExist()
    }

    @Test
    fun closesTheAppFromTheDialog() {
        setSmsDisallowed(isDisallowed = true)

        setContent()
        moveTo(state = Lifecycle.State.RESUMED)
        composeTestRule.onNodeWithText(closeText).performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, closeCount)
        }
    }

    private fun setSmsDisallowed(isDisallowed: Boolean) {
        val userManager = targetContext.getSystemService(UserManager::class.java)
        shadowOf(userManager).setUserRestriction(
            Process.myUserHandle(),
            UserManager.DISALLOW_SMS,
            isDisallowed,
        )
    }

    private fun setContent() {
        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.CREATED)
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AppTheme {
                    DeviceOwnerSmsBlock(onClose = { closeCount += 1 })
                }
            }
        }
    }

    private fun moveTo(state: Lifecycle.State) {
        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = state)
        }
        composeTestRule.waitForIdle()
    }
}
