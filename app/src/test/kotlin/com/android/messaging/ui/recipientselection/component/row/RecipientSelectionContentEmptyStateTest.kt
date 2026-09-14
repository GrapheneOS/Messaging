package com.android.messaging.ui.recipientselection.component.row

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.recipientselection.component.RecipientSelectionContactsContent
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionContentUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecipientSelectionContentEmptyStateTest :
    BaseRecipientSelectionContactRowTest() {

    private val hostPrompt = targetContext.getString(R.string.forward_picker_empty_text)
    private val noResultsText = targetContext.getString(R.string.recipient_picker_no_results_text)

    @Test
    fun emptyContent_withoutQuery_showsTheHostSuppliedPrompt() {
        setSelectionContent(
            uiState = emptyContentUiState(query = ""),
            emptyStateText = R.string.forward_picker_empty_text,
        )

        composeTestRule.onNodeWithText(hostPrompt).assertIsDisplayed()
        composeTestRule.onNodeWithText(noResultsText).assertDoesNotExist()
    }

    @Test
    fun emptyContent_withBlankQuery_showsTheHostSuppliedPrompt() {
        setSelectionContent(
            uiState = emptyContentUiState(query = "   "),
            emptyStateText = R.string.forward_picker_empty_text,
        )

        composeTestRule.onNodeWithText(hostPrompt).assertIsDisplayed()
        composeTestRule.onNodeWithText(noResultsText).assertDoesNotExist()
    }

    @Test
    fun emptyContent_withQuery_showsNoResultsInsteadOfThePrompt() {
        setSelectionContent(
            uiState = emptyContentUiState(query = "zzqqxx"),
            emptyStateText = R.string.forward_picker_empty_text,
        )

        composeTestRule.onNodeWithText(noResultsText).assertIsDisplayed()
        composeTestRule.onNodeWithText(hostPrompt).assertDoesNotExist()
    }

    @Test
    fun loadingContent_withQuery_showsNeitherEmptyState() {
        setSelectionContent(
            uiState = emptyContentUiState(query = "zzqqxx", isLoading = true),
            emptyStateText = R.string.forward_picker_empty_text,
        )

        composeTestRule.onNodeWithText(noResultsText).assertDoesNotExist()
        composeTestRule.onNodeWithText(hostPrompt).assertDoesNotExist()
    }

    @Test
    fun matchedContent_withQuery_showsNeitherEmptyState() {
        setSelectionContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    query = "ada",
                    items = persistentListOf(singleDestinationContactItem()),
                ),
            ),
            emptyStateText = R.string.forward_picker_empty_text,
        )

        composeTestRule.onNodeWithText(noResultsText).assertDoesNotExist()
        composeTestRule.onNodeWithText(hostPrompt).assertDoesNotExist()
    }

    @Test
    fun emptyContent_whenQueryIsTyped_swapsThePromptForNoResults() {
        val uiState = mutableStateOf(emptyContentUiState(query = ""))

        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionContactsContent(
                    uiState = uiState.value,
                    rowDecorators = defaultRowDecorators(),
                    onRecipientDestinationClick = onContentDestinationClick,
                    onLoadMore = onLoadMore,
                    onPrimaryActionClick = onPrimaryActionClick,
                    onRecipientDestinationLongClick = onContentDestinationLongClick,
                    emptyStateText = R.string.forward_picker_empty_text,
                )
            }
        }

        composeTestRule.onNodeWithText(hostPrompt).assertIsDisplayed()

        uiState.value = emptyContentUiState(query = "zzqqxx")

        composeTestRule.onNodeWithText(noResultsText).assertIsDisplayed()
        composeTestRule.onNodeWithText(hostPrompt).assertDoesNotExist()
    }

    private fun emptyContentUiState(
        query: String,
        isLoading: Boolean = false,
    ): RecipientSelectionContentUiState {
        return RecipientSelectionContentUiState(
            picker = RecipientPickerUiState(
                query = query,
                isLoading = isLoading,
            ),
        )
    }
}
