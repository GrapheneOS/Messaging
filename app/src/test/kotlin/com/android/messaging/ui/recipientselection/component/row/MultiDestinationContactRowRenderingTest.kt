package com.android.messaging.ui.recipientselection.component.row

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MultiDestinationContactRowRenderingTest : BaseRecipientSelectionContactRowTest() {

    @Test
    fun multiDestinationContact_rendersHeaderAndAllDestinations() {
        setMultiDestinationRowContent()

        composeTestRule.onNodeWithText(CONTACT_DISPLAY_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(MOBILE_DESTINATION).assertIsDisplayed()
        composeTestRule.onNodeWithText(WORK_EMAIL_DESTINATION).assertIsDisplayed()
        composeTestRule.onNodeWithText(HOME_PHONE_DESTINATION).assertIsDisplayed()
    }

    @Test
    fun phoneAndEmailDestinations_usePlatformLabels() {
        setMultiDestinationRowContent()

        composeTestRule
            .onNodeWithText(
                destinationLabel(
                    kind = DestinationLabelKind.PHONE,
                    type = Phone.TYPE_MOBILE,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                destinationLabel(
                    kind = DestinationLabelKind.EMAIL,
                    type = Email.TYPE_WORK,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                destinationLabel(
                    kind = DestinationLabelKind.PHONE,
                    type = Phone.TYPE_HOME,
                ),
            )
            .assertIsDisplayed()
    }

    @Test
    fun selectedDestination_setsSelectedSemanticsOnlyForMatchingDestination() {
        val item = multiDestinationContactItem()

        setMultiDestinationRowContent(
            item = item,
            selectedDestinations = persistentSetOf(WORK_EMAIL_NORMALIZED_DESTINATION),
        )

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = MOBILE_DESTINATION))
            .assertIsNotSelected()
        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = WORK_EMAIL_DESTINATION))
            .assertIsSelected()
        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = HOME_PHONE_DESTINATION))
            .assertIsNotSelected()
    }

    @Test
    fun adjacentSelectedDestinations_keepEachSelectedDestinationSelected() {
        val item = multiDestinationContactItem()

        setMultiDestinationRowContent(
            item = item,
            selectedDestinations = persistentSetOf(
                MOBILE_NORMALIZED_DESTINATION,
                WORK_EMAIL_NORMALIZED_DESTINATION,
            ),
        )

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = MOBILE_DESTINATION))
            .assertIsSelected()
        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = WORK_EMAIL_DESTINATION))
            .assertIsSelected()
        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = HOME_PHONE_DESTINATION))
            .assertIsNotSelected()
    }

    @Test
    fun nonAdjacentSelectedDestinations_keepSeparatedDestinationsSelected() {
        val item = multiDestinationContactItem()

        setMultiDestinationRowContent(
            item = item,
            selectedDestinations = persistentSetOf(
                MOBILE_NORMALIZED_DESTINATION,
                HOME_PHONE_NORMALIZED_DESTINATION,
            ),
        )

        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = MOBILE_DESTINATION))
            .assertIsSelected()
        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = WORK_EMAIL_DESTINATION))
            .assertIsNotSelected()
        composeTestRule
            .onNodeWithTag(destinationRowTestTag(item = item, destination = HOME_PHONE_DESTINATION))
            .assertIsSelected()
    }

    @Test
    fun trailingIndicatorVisibleForSpecificDestination() {
        setMultiDestinationRowContent(
            rowDecorators = defaultRowDecorators(
                showTrailingIndicator = { _, destination ->
                    destination == WORK_EMAIL_NORMALIZED_DESTINATION
                },
            ),
        )

        composeTestRule
            .onAllNodesWithTag(TRAILING_INDICATOR_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
    }

    private fun destinationLabel(kind: DestinationLabelKind, type: Int): String {
        val label = when (kind) {
            DestinationLabelKind.PHONE -> {
                Phone.getTypeLabel(targetContext.resources, type, null)
            }

            DestinationLabelKind.EMAIL -> {
                Email.getTypeLabel(targetContext.resources, type, null)
            }
        }

        return label.toString()
    }

    private enum class DestinationLabelKind {
        EMAIL,
        PHONE,
    }
}
