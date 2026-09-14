package com.android.messaging.ui.conversationpicker.common

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test

internal class PickerTopAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchActionVisibleWhenNotSelecting() {
        composeTopAppBar(
            isSearchActive = false,
            inSelectionMode = false,
        )

        composeRule
            .onNodeWithContentDescription(string(R.string.share_search))
            .assertIsDisplayed()
    }

    @Test
    fun searchActionHiddenWhenSelecting() {
        composeTopAppBar(
            isSearchActive = false,
            inSelectionMode = true,
        )

        composeRule
            .onNodeWithContentDescription(string(R.string.share_search))
            .assertDoesNotExist()
    }

    @Test
    fun clearSearchActionVisibleWhenSelectingFromSearch() {
        composeTopAppBar(
            isSearchActive = true,
            inSelectionMode = true,
            searchText = "alex",
        )

        composeRule
            .onNodeWithContentDescription(string(R.string.share_search_clear))
            .assertIsDisplayed()
    }

    private fun composeTopAppBar(
        isSearchActive: Boolean,
        inSelectionMode: Boolean,
        searchText: String = "",
    ) {
        composeRule.setContent {
            AppTheme {
                PickerTopAppBar(
                    isSearchActive = isSearchActive,
                    inSelectionMode = inSelectionMode,
                    selectedCount = 1,
                    searchState = TextFieldState(initialText = searchText),
                    title = R.string.share_intent_activity_label,
                    searchHint = R.string.share_search_hint,
                    onNavigateBack = {},
                    onSearchOpen = {},
                    onSearchClose = {},
                    onSelectionClear = {},
                )
            }
        }
    }

    // TODO: remove then conversation-compose-tests merged
    private fun string(resId: Int): String {
        return InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(resId)
    }
}
