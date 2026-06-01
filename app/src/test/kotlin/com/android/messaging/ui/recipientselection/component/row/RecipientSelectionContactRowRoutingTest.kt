package com.android.messaging.ui.recipientselection.component.row

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecipientSelectionContactRowRoutingTest : BaseRecipientSelectionContactRowTest() {

    @Test
    fun singleDestinationContact_routesClickToNormalizedDestination() {
        val item = singleDestinationContactItem()

        setContactRowContent(item = item)

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .performClick()

        verify(exactly = 1) {
            onRowDestinationClick.invoke(MOBILE_NORMALIZED_DESTINATION)
        }
    }

    @Test
    fun singleDestinationContact_routesLongClickToNormalizedDestination() {
        val item = singleDestinationContactItem()

        setContactRowContent(item = item)

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .performSemanticsAction(SemanticsActions.OnLongClick)

        verify(exactly = 1) {
            onRowDestinationLongClick.invoke(MOBILE_NORMALIZED_DESTINATION)
        }
        verify(exactly = 0) {
            onRowDestinationClick.invoke(any())
        }
    }

    @Test
    fun singleDestinationContact_withoutLongClickHandlerStillRoutesClick() {
        val item = singleDestinationContactItem()

        setContactRowContent(item = item, onDestinationLongClick = null)

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .performClick()

        verify(exactly = 1) {
            onRowDestinationClick.invoke(MOBILE_NORMALIZED_DESTINATION)
        }
        verify(exactly = 0) {
            onRowDestinationLongClick.invoke(any())
        }
    }

    @Test
    fun selectedSingleDestinationContact_setsRowSelectedSemantics() {
        val item = singleDestinationContactItem()

        setContactRowContent(
            item = item,
            selectedDestinations = persistentSetOf(MOBILE_NORMALIZED_DESTINATION),
        )

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .assertIsSelected()
    }

    @Test
    fun multiDestinationContact_routesSpecificDestinationClick() {
        val item = multiDestinationContactItem()

        setContactRowContent(item = item)

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = WORK_EMAIL_DESTINATION))
            .performClick()

        verify(exactly = 1) {
            onRowDestinationClick.invoke(WORK_EMAIL_NORMALIZED_DESTINATION)
        }
    }

    @Test
    fun syntheticPhone_routesClickToNormalizedDestination() {
        val item = syntheticPhoneItem()

        setContactRowContent(item = item)

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .performClick()

        verify(exactly = 1) {
            onRowDestinationClick.invoke(SYNTHETIC_PHONE_NORMALIZED_DESTINATION)
        }
    }

    @Test
    fun syntheticPhone_withoutLongClickHandlerStillRoutesClick() {
        val item = syntheticPhoneItem()

        setContactRowContent(item = item, onDestinationLongClick = null)

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .performClick()

        verify(exactly = 1) {
            onRowDestinationClick.invoke(SYNTHETIC_PHONE_NORMALIZED_DESTINATION)
        }
        verify(exactly = 0) {
            onRowDestinationLongClick.invoke(any())
        }
    }

    @Test
    fun syntheticPhone_routesLongClickToNormalizedDestination() {
        val item = syntheticPhoneItem()

        setContactRowContent(item = item)

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .performSemanticsAction(SemanticsActions.OnLongClick)

        verify(exactly = 1) {
            onRowDestinationLongClick.invoke(SYNTHETIC_PHONE_NORMALIZED_DESTINATION)
        }
    }

    @Test
    fun selectedSyntheticPhone_setsRowSelectedSemantics() {
        val item = syntheticPhoneItem()

        setContactRowContent(
            item = item,
            selectedDestinations = persistentSetOf(SYNTHETIC_PHONE_NORMALIZED_DESTINATION),
        )

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .assertIsDisplayed()
            .assertIsSelected()
    }

    @Test
    fun contactWithoutDestinations_rendersHeaderWithoutDestinationRows() {
        val item = contactWithoutDestinations()

        setContactRowContent(item = item)

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(item = item))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(EMPTY_CONTACT_DISPLAY_NAME)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = MOBILE_DESTINATION))
            .assertDoesNotExist()
    }
}
