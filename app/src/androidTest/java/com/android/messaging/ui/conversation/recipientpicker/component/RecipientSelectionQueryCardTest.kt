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
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryCardUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryChipsUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryTextUiState
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val PREFIX_TEXT = "To"
private const val PLACEHOLDER_TEXT = "Name, phone or email"

internal class RecipientSelectionQueryCardTest {
    @get:Rule
    val composeRule = createComposeRule()

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

        composeRule.setContent {
            AppTheme {
                val uiState = RecipientSelectionQueryCardUiState(
                    text = RecipientSelectionQueryTextUiState(
                        query = "",
                        enabled = true,
                        prefixText = PREFIX_TEXT,
                        placeholderText = PLACEHOLDER_TEXT,
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

        composeRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .performTextReplacement(text = "")

        composeRule.runOnIdle {
            assertEquals(1, removeCount)
        }

        composeRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .performTextReplacement(text = "")

        composeRule.runOnIdle {
            assertEquals(2, removeCount)
        }
    }

    @Test
    fun firstChipInsertionRetainsQueryFieldFocus() {
        var recipients by mutableStateOf(persistentListOf<SelectedRecipient>())
        val focusRequester = FocusRequester()

        composeRule.setContent {
            AppTheme {
                val uiState = RecipientSelectionQueryCardUiState(
                    text = RecipientSelectionQueryTextUiState(
                        query = "",
                        enabled = true,
                        prefixText = PREFIX_TEXT,
                        placeholderText = PLACEHOLDER_TEXT,
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

        composeRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .requestFocus()
            .assertIsFocused()

        composeRule.runOnIdle {
            recipients = persistentListOf(
                SelectedRecipient(
                    destination = "+3725400001",
                    label = "Recipient 1",
                    displayDestination = "+372 5400 0001",
                    photoUri = null,
                ),
            )
        }

        composeRule
            .onNodeWithTag(testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .assertIsFocused()
    }
}
