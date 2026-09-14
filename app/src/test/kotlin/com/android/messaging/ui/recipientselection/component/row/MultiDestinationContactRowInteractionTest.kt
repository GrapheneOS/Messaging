package com.android.messaging.ui.recipientselection.component.row

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MultiDestinationContactRowInteractionTest : BaseRecipientSelectionContactRowTest() {

    @Test
    fun clickDestination_forwardsNormalizedValue() {
        val item = multiDestinationContactItem()

        setMultiDestinationRowContent(item = item)

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = WORK_EMAIL_DESTINATION))
            .performClick()

        verify(exactly = 1) {
            onRowDestinationClick.invoke(WORK_EMAIL_NORMALIZED_DESTINATION)
        }
        verify(exactly = 0) {
            onRowDestinationLongClick.invoke(any())
        }
    }

    @Test
    fun longClickDestination_forwardsNormalizedValue() {
        val item = multiDestinationContactItem()

        setMultiDestinationRowContent(item = item)

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = HOME_PHONE_DESTINATION))
            .performSemanticsAction(SemanticsActions.OnLongClick)

        verify(exactly = 1) {
            onRowDestinationLongClick.invoke(HOME_PHONE_NORMALIZED_DESTINATION)
        }
        verify(exactly = 0) {
            onRowDestinationClick.invoke(any())
        }
    }

    @Test
    fun destinationWithoutLongClickHandler_stillForwardsClick() {
        val item = multiDestinationContactItem()

        setMultiDestinationRowContent(item = item, onDestinationLongClick = null)

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = WORK_EMAIL_DESTINATION))
            .performClick()

        verify(exactly = 1) {
            onRowDestinationClick.invoke(WORK_EMAIL_NORMALIZED_DESTINATION)
        }
        verify(exactly = 0) {
            onRowDestinationLongClick.invoke(any())
        }
    }

    @Test
    fun disabledDestination_doesNotForwardClickOrLongClick() {
        val item = multiDestinationContactItem()

        setMultiDestinationRowContent(item = item, enabled = false)

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = MOBILE_DESTINATION))
            .performTouchInput {
                click(position = center)
                longClick(position = center)
            }
        composeTestRule.waitForIdle()

        verify(exactly = 0) {
            onRowDestinationClick.invoke(any())
        }
        verify(exactly = 0) {
            onRowDestinationLongClick.invoke(any())
        }
    }
}
