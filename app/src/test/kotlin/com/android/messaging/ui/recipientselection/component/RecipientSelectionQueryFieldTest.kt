package com.android.messaging.ui.recipientselection.component

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.android.messaging.ui.conversation.RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionQueryFieldUiState
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import kotlinx.collections.immutable.toPersistentList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecipientSelectionQueryFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun softBackspaceWithChipsAndEmptyQueryRemovesLastRecipient() {
        val uiState = queryFieldUiState(query = "", recipientCount = 1)
        val state = TextFieldState(initialText = recipientSelectionQueryFieldEditableText(uiState))
        val focusRequester = FocusRequester()
        var lastRecipientRemoveCount = 0

        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionQueryField(
                    uiState = uiState,
                    state = state,
                    onQueryFocusChanged = {},
                    onLastSelectedRecipientRemove = {
                        lastRecipientRemoveCount += 1
                    },
                    focusRequester = focusRequester,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .requestFocus()
            .performKeyInput { pressKey(key = Key.Backspace) }

        composeTestRule.runOnIdle {
            assertEquals(1, lastRecipientRemoveCount)
        }
    }

    @Test
    fun softBackspaceWithoutChipsAndEmptyQueryDoesNotRemoveRecipient() {
        val uiState = queryFieldUiState(query = "", recipientCount = 0)
        val state = TextFieldState(initialText = recipientSelectionQueryFieldEditableText(uiState))
        val focusRequester = FocusRequester()
        var lastRecipientRemoveCount = 0

        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionQueryField(
                    uiState = uiState,
                    state = state,
                    onQueryFocusChanged = {},
                    onLastSelectedRecipientRemove = {
                        lastRecipientRemoveCount += 1
                    },
                    focusRequester = focusRequester,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .requestFocus()
            .performKeyInput { pressKey(key = Key.Backspace) }

        composeTestRule.runOnIdle {
            assertEquals(0, lastRecipientRemoveCount)
        }
    }

    @Test
    fun typingFirstCharacterWithChipsEmitsQueryWithoutSentinel() {
        val uiState = queryFieldUiState(query = "", recipientCount = 1)
        val state = TextFieldState(initialText = recipientSelectionQueryFieldEditableText(uiState))
        val focusRequester = FocusRequester()

        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionQueryField(
                    uiState = uiState,
                    state = state,
                    onQueryFocusChanged = {},
                    onLastSelectedRecipientRemove = {},
                    focusRequester = focusRequester,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .performTextInput(text = "a")

        composeTestRule.runOnIdle {
            val currentText = state.text.toString()
            assertEquals("a", currentText)
        }
    }

    @Test
    fun hardwareBackspaceWithVisibleQueryDoesNotRemoveRecipient() {
        val uiState = queryFieldUiState(query = "hello", recipientCount = 1)
        val state = TextFieldState(initialText = recipientSelectionQueryFieldEditableText(uiState))
        val focusRequester = FocusRequester()
        var lastRecipientRemoveCount = 0

        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionQueryField(
                    uiState = uiState,
                    state = state,
                    onQueryFocusChanged = {},
                    onLastSelectedRecipientRemove = {
                        lastRecipientRemoveCount += 1
                    },
                    focusRequester = focusRequester,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .requestFocus()
            .performKeyInput { pressKey(key = Key.Backspace) }

        composeTestRule.runOnIdle {
            assertEquals(0, lastRecipientRemoveCount)
        }
    }

    private fun queryFieldUiState(
        query: String,
        recipientCount: Int,
    ): RecipientSelectionQueryFieldUiState {
        val recipients = (0 until recipientCount).map { index ->
            SelectedRecipient(
                destination = "+372540000$index",
                label = "Recipient $index",
                displayDestination = "+372 5400 000$index",
                photoUri = null,
            )
        }

        return RecipientSelectionQueryFieldUiState(
            query = query,
            enabled = true,
            placeholderText = RECIPIENT_SELECTION_PLACEHOLDER_TEXT,
            selectedRecipients = recipients.toPersistentList(),
        )
    }
}
