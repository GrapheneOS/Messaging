package com.android.messaging.ui.recipientselection.component.row

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionContentUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecipientSelectionContentSectionHeadersTest :
    BaseRecipientSelectionContactRowTest() {

    @Test
    fun contactList_whileTypedQueryIsPending_keepsItsSectionHeaders() {
        setSelectionContent(
            uiState = contentUiState(query = "taxi", itemsQuery = ""),
        )

        composeTestRule.onNodeWithText(NON_LETTER_SECTION_HEADER).assertIsDisplayed()
    }

    @Test
    fun searchResults_whileClearedQueryIsPending_stayWithoutSectionHeaders() {
        setSelectionContent(
            uiState = contentUiState(query = "", itemsQuery = "taxi"),
        )

        composeTestRule.onNodeWithText(NON_LETTER_CONTACT_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(NON_LETTER_SECTION_HEADER).assertDoesNotExist()
    }

    private fun contentUiState(
        query: String,
        itemsQuery: String,
    ): RecipientSelectionContentUiState {
        return RecipientSelectionContentUiState(
            picker = RecipientPickerUiState(
                query = query,
                itemsQuery = itemsQuery,
                items = persistentListOf(contactItem(displayName = NON_LETTER_CONTACT_NAME)),
            ),
        )
    }

    private companion object {
        private const val NON_LETTER_CONTACT_NAME = "123 Taxi"
        private const val NON_LETTER_SECTION_HEADER = "#"
    }
}
