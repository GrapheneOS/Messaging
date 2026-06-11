package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import com.android.messaging.ui.conversation.RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryCardUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryChipsUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryTextUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecipientSelectionQueryCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun softBackspaceRemovesEachChipInSequenceWhenStartingWithMultipleChips() {
        var recipients by mutableStateOf(
            persistentListOf(
                SelectedRecipient(
                    destination = "+3725400001",
                    label = "Recipient 1",
                    displayDestination = "+372 5400 0001",
                    photoUri = null,
                ),
                SelectedRecipient(
                    destination = "+3725400002",
                    label = "Recipient 2",
                    displayDestination = "+372 5400 0002",
                    photoUri = null,
                ),
            ),
        )
        val focusRequester = FocusRequester()
        var removeCount = 0

        composeTestRule.setContent {
            AppTheme {
                val uiState = RecipientSelectionQueryCardUiState(
                    text = RecipientSelectionQueryTextUiState(
                        query = "",
                        enabled = true,
                        prefixText = PREFIX_TEXT,
                        placeholderText = RECIPIENT_SELECTION_PLACEHOLDER_TEXT,
                    ),
                    chips = RecipientSelectionQueryChipsUiState(
                        recipients = recipients,
                        armedRecipientDestination = null,
                        enabled = true,
                    ),
                )

                RecipientSelectionQueryCard(
                    uiState = uiState,
                    onQueryChanged = {},
                    onQueryFocused = {},
                    onSelectedRecipientClick = {},
                    onSelectedRecipientBackspace = { recipient ->
                        removeCount += 1
                        recipients = recipients.remove(recipient)
                    },
                    focusRequester = focusRequester,
                    simSelectorSlot = null,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .performTextReplacement(text = "")

        composeTestRule.runOnIdle {
            assertEquals(1, removeCount)
        }

        composeTestRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .performTextReplacement(text = "")

        composeTestRule.runOnIdle {
            assertEquals(2, removeCount)
        }
    }

    @Test
    fun firstChipInsertionRetainsQueryFieldFocus() {
        var recipients by mutableStateOf(persistentListOf<SelectedRecipient>())
        val focusRequester = FocusRequester()

        composeTestRule.setContent {
            AppTheme {
                val uiState = RecipientSelectionQueryCardUiState(
                    text = RecipientSelectionQueryTextUiState(
                        query = "",
                        enabled = true,
                        prefixText = PREFIX_TEXT,
                        placeholderText = RECIPIENT_SELECTION_PLACEHOLDER_TEXT,
                    ),
                    chips = RecipientSelectionQueryChipsUiState(
                        recipients = recipients,
                        armedRecipientDestination = null,
                        enabled = true,
                    ),
                )

                RecipientSelectionQueryCard(
                    uiState = uiState,
                    onQueryChanged = {},
                    onQueryFocused = {},
                    onSelectedRecipientClick = {},
                    onSelectedRecipientBackspace = {},
                    focusRequester = focusRequester,
                    simSelectorSlot = null,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .requestFocus()
            .assertIsFocused()

        composeTestRule.runOnIdle {
            recipients = persistentListOf(
                SelectedRecipient(
                    destination = "+3725400001",
                    label = "Recipient 1",
                    displayDestination = "+372 5400 0001",
                    photoUri = null,
                ),
            )
        }

        composeTestRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .assertIsFocused()
    }

    private companion object {
        private const val PREFIX_TEXT = "To"
    }
}
