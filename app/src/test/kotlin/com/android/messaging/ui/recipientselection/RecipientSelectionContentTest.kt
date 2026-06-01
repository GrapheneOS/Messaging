package com.android.messaging.ui.recipientselection

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.recipientselection.component.RecipientSelectionContent
import com.android.messaging.ui.recipientselection.component.row.contactItem
import com.android.messaging.ui.recipientselection.component.row.recipientRowTestTag
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.selection.OnRecipientDestinationAction
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionPrimaryActionUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionRowDecorators
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionStrings
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecipientSelectionContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingState_showsProgressIndicatorAndHidesEmptyMessage() {
        setContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    isLoading = true,
                ),
            ),
        )

        composeTestRule
            .onAllNodes(
                matcher = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate),
            )
            .assertCountEquals(expectedSize = 1)
        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.contact_list_empty_text))
            .assertDoesNotExist()
    }

    @Test
    fun emptyItems_showsEmptyMessage() {
        setContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(),
            ),
        )

        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.contact_list_empty_text))
            .assertIsDisplayed()
    }

    @Test
    fun primaryAction_isShownAndForwardsClicks() {
        var primaryActionClicks = 0

        setContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    items = persistentListOf(contactItem()),
                ),
                primaryAction = RecipientSelectionPrimaryActionUiState(
                    text = "Continue",
                    isEnabled = true,
                    testTag = RECIPIENT_SELECTION_PRIMARY_ACTION_TEST_TAG,
                ),
            ),
            onPrimaryActionClick = {
                primaryActionClicks += 1
            },
        )

        composeTestRule
            .onNodeWithTag(RECIPIENT_SELECTION_PRIMARY_ACTION_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, primaryActionClicks)
        }
    }

    @Test
    fun longPressAndTrailingIndicator_areSupportedByRowDecorators() {
        val item = contactItem()
        var clickCount = 0
        var longClickedDestination: String? = null

        setContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    items = persistentListOf(item),
                ),
            ),
            rowDecorators = RecipientSelectionRowDecorators(
                recipientRowTestTag = { item ->
                    recipientRowTestTag(item = item)
                },
                showRecipientTrailingIndicator = { _, _ -> true },
                trailingIndicatorTestTag = RECIPIENT_SELECTION_TRAILING_INDICATOR_TEST_TAG,
            ),
            onRecipientDestinationClick = { _, _ ->
                clickCount += 1
            },
            onRecipientDestinationLongClick = { _, destination ->
                longClickedDestination = destination
            },
        )

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .performSemanticsAction(SemanticsActions.OnLongClick)

        composeTestRule
            .onNodeWithTag(RECIPIENT_SELECTION_TRAILING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(
                item.contact.destinations.first().normalizedValue,
                longClickedDestination,
            )
            assertEquals(0, clickCount)
        }
    }

    @Test
    fun scrollingNearEnd_requestsLoadMoreWhenAllowed() {
        var loadMoreCount = 0

        setContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    items = persistentListOf(
                        *Array(size = 30) { index ->
                            contactItem(
                                id = index.toLong(),
                                displayName = "Contact $index",
                                destination = "+1 555 ${
                                    index.toString().padStart(length = 4, padChar = '0')
                                }",
                                normalizedDestination = "+1555${
                                    index.toString().padStart(length = 4, padChar = '0')
                                }",
                            )
                        },
                    ),
                    canLoadMore = true,
                ),
            ),
            onLoadMore = {
                loadMoreCount += 1
            },
        )

        composeTestRule
            .onNode(matcher = hasScrollToIndexAction())
            .performScrollToIndex(index = 29)
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(1, loadMoreCount)
        }
    }

    private fun setContent(
        uiState: RecipientSelectionContentUiState,
        rowDecorators: RecipientSelectionRowDecorators = RecipientSelectionRowDecorators(
            recipientRowTestTag = { item ->
                recipientRowTestTag(item = item)
            },
        ),
        onLoadMore: () -> Unit = {},
        onPrimaryActionClick: () -> Unit = {},
        onRecipientDestinationClick: OnRecipientDestinationAction = { _, _ -> },
        onRecipientDestinationLongClick: OnRecipientDestinationAction? = null,
    ) {
        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionContent(
                    uiState = uiState,
                    strings = RecipientSelectionStrings(
                        queryPrefixText = targetContext.getString(R.string.to_address_label),
                        queryPlaceholderText = targetContext.getString(
                            R.string.new_chat_query_hint
                        ),
                    ),
                    rowDecorators = rowDecorators,
                    onRecipientDestinationClick = onRecipientDestinationClick,
                    onLoadMore = onLoadMore,
                    onPrimaryActionClick = onPrimaryActionClick,
                    onQueryChanged = {},
                    onRecipientDestinationLongClick = onRecipientDestinationLongClick,
                )
            }
        }
    }
}
