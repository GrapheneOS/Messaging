package com.android.messaging.ui.appsettings.subscription.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhoneNumberDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val errorText = targetContext.getString(R.string.invalid_self_phone_number)
    private val okText = targetContext.getString(android.R.string.ok)

    private var confirmedNumber: String? = null
    private var wasErrorDismissed = false

    @Test
    fun dialogShowsTheRejectionItIsGivenWithoutLosingWhatWasTyped() {
        setContent(currentNumber = "not a number", isInvalid = true)

        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()
        composeTestRule.onNodeWithText("not a number").assertIsDisplayed()
    }

    @Test
    fun dialogShowsNoErrorUntilItIsGivenOne() {
        setContent(currentNumber = "+37255550001", isInvalid = false)

        composeTestRule.onNodeWithText(errorText).assertIsNotDisplayed()
    }

    @Test
    fun dialogHandsTheTypedNumberUpOnConfirm() {
        setContent(currentNumber = "+37255550001", isInvalid = false)

        composeTestRule.onNodeWithText(okText).performClick()

        composeTestRule.runOnIdle {
            assertEquals("+37255550001", confirmedNumber)
        }
    }

    @Test
    fun dialogReportsAnEditSoTheRejectionCanBeCleared() {
        setContent(currentNumber = "12", isInvalid = true)

        composeTestRule.onNodeWithText("12").performTextInput("3")

        composeTestRule.runOnIdle {
            assertTrue(
                "the dialog does not decide the error is gone, it reports the edit that clears it",
                wasErrorDismissed,
            )
        }
    }

    private fun setContent(
        currentNumber: String,
        isInvalid: Boolean,
    ) {
        composeTestRule.setContent {
            AppTheme {
                PhoneNumberDialog(
                    currentNumber = currentNumber,
                    isInvalid = isInvalid,
                    onErrorDismissed = { wasErrorDismissed = true },
                    onDismiss = {},
                    onConfirm = { confirmedNumber = it },
                )
            }
        }
    }
}
