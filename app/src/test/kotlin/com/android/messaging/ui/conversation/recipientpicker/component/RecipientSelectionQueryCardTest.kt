package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
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
import kotlinx.collections.immutable.ImmutableList
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
                        recipients = recipients.removing(recipient)
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

    @Test
    fun lateQueryUpdateDoesNotOverwriteNewerTypedText() {
        var query by mutableStateOf("")
        val sentQueries = mutableListOf<String>()

        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionQueryCard(
                    uiState = queryCardUiState(query = query),
                    onQueryChanged = { sentQueries += it },
                    onQueryFocused = {},
                    onSelectedRecipientClick = {},
                    onSelectedRecipientBackspace = {},
                    focusRequester = FocusRequester(),
                    simSelectorSlot = null,
                )
            }
        }

        val field = composeTestRule.onNodeWithTag(
            testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG,
        )
        field.performTextReplacement(text = "a")
        field.performTextReplacement(text = "ab")
        composeTestRule.runOnIdle { query = "a" }

        composeTestRule.runOnIdle {
            assertEquals("ab", field.fieldText())
            assertEquals(listOf("a", "ab"), sentQueries)
        }
    }

    @Test
    fun addingRecipientClearsQueryEvenIfStateSkippedEarlierSends() {
        var query by mutableStateOf("")
        var recipients by mutableStateOf(persistentListOf<SelectedRecipient>())

        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionQueryCard(
                    uiState = queryCardUiState(query = query, recipients = recipients),
                    onQueryChanged = {},
                    onQueryFocused = {},
                    onSelectedRecipientClick = {},
                    onSelectedRecipientBackspace = {},
                    focusRequester = FocusRequester(),
                    simSelectorSlot = null,
                )
            }
        }

        val field = composeTestRule.onNodeWithTag(
            testTag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG,
        )
        field.performTextReplacement(text = "a")
        composeTestRule.runOnIdle { query = "a" }
        // The state never reports these two, as it ends up where it was
        field.performTextReplacement(text = "")
        field.performTextReplacement(text = "a")
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
        // The screen state combines the recipients and the query, so the clear can come a frame later
        composeTestRule.runOnIdle { query = "" }

        composeTestRule.runOnIdle {
            assertEquals("", recipientSelectionVisibleQueryText(fieldText = field.fieldText()))
        }
    }

    private fun queryCardUiState(
        query: String,
        recipients: ImmutableList<SelectedRecipient> = persistentListOf(),
    ): RecipientSelectionQueryCardUiState {
        return RecipientSelectionQueryCardUiState(
            text = RecipientSelectionQueryTextUiState(
                query = query,
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
    }

    private fun SemanticsNodeInteraction.fieldText(): String {
        return fetchSemanticsNode().config[SemanticsProperties.EditableText].text
    }

    private companion object {
        private const val PREFIX_TEXT = "To"
    }
}
