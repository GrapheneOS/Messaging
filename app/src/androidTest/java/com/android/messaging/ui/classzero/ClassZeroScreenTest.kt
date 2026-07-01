package com.android.messaging.ui.classzero

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.ui.classzero.model.ClassZeroUiState
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

internal class ClassZeroScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun screenDisplaysClassZeroMessage() {
        renderScreen()

        composeTestRule
            .onNodeWithTag(CLASS_ZERO_TITLE_TEST_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(MESSAGE_TEXT)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(CLASS_ZERO_SAVE_BUTTON_TEST_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(CLASS_ZERO_CANCEL_BUTTON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun saveClickCallsSaveCallback() {
        var saveClickCount = 0

        renderScreen(
            onSaveClicked = {
                saveClickCount += 1
            },
        )

        composeTestRule
            .onNodeWithTag(CLASS_ZERO_SAVE_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, saveClickCount)
        }
    }

    @Test
    fun cancelClickCallsCancelCallback() {
        var cancelClickCount = 0

        renderScreen(
            onCancelClicked = {
                cancelClickCount += 1
            },
        )

        composeTestRule
            .onNodeWithTag(CLASS_ZERO_CANCEL_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, cancelClickCount)
        }
    }

    private fun renderScreen(
        onSaveClicked: () -> Unit = {},
        onCancelClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                ClassZeroScreen(
                    uiState = ClassZeroUiState(
                        messageText = MESSAGE_TEXT,
                    ),
                    onSaveClicked = onSaveClicked,
                    onCancelClicked = onCancelClicked,
                )
            }
        }
    }

    private companion object {
        private const val MESSAGE_TEXT = "Class zero message"
    }
}
