package com.android.messaging.ui.conversation.recipientpicker.component.row

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionPrimaryActionUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecipientSelectionContentMultiDestinationTest :
    BaseRecipientSelectionContactRowTest() {

    @Test
    fun multiDestinationContent_forwardsSpecificItemAndNormalizedDestination() {
        val item = multiDestinationContactItem()

        setSelectionContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    items = persistentListOf(item),
                ),
            ),
        )

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = WORK_EMAIL_DESTINATION))
            .performClick()

        verify(exactly = 1) {
            onContentDestinationClick.invoke(item, WORK_EMAIL_NORMALIZED_DESTINATION)
        }
    }

    @Test
    fun selectedRecipients_markMatchingMultiDestinationOnly() {
        val item = multiDestinationContactItem()

        setSelectionContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    items = persistentListOf(item),
                ),
                selectedRecipients = persistentListOf(
                    selectedRecipient(
                        destination = WORK_EMAIL_NORMALIZED_DESTINATION,
                        displayDestination = WORK_EMAIL_DESTINATION,
                    ),
                ),
            ),
        )

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = MOBILE_DESTINATION))
            .assertIsNotSelected()
        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = WORK_EMAIL_DESTINATION))
            .assertIsSelected()
    }

    @Test
    fun loadingPrimaryAction_disablesMultiDestinationRows() {
        val item = multiDestinationContactItem()

        setSelectionContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    items = persistentListOf(item),
                ),
                primaryAction = RecipientSelectionPrimaryActionUiState(
                    text = "Next",
                    isEnabled = false,
                    isLoading = true,
                    testTag = null,
                ),
            ),
        )

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = MOBILE_DESTINATION))
            .performTouchInput {
                click(position = center)
            }
        composeTestRule.waitForIdle()

        verify(exactly = 0) {
            onContentDestinationClick.invoke(any(), any())
        }
    }
}
