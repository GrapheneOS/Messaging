package com.android.messaging.ui.conversation.recipientpicker.component.simselector

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.NEW_CHAT_SIM_SELECTOR_CHIP_TEST_TAG
import com.android.messaging.ui.conversation.NEW_CHAT_SIM_SELECTOR_DROPDOWN_TEST_TAG
import com.android.messaging.ui.conversation.TEST_ATT_SUBSCRIPTION_NAME
import com.android.messaging.ui.conversation.TEST_VERIZON_SUBSCRIPTION_NAME
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.newChatSimSelectorItemTestTag
import com.android.messaging.ui.conversation.testAttSubscription
import com.android.messaging.ui.conversation.testVerizonSubscription
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NewChatSimSelectorRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onSimSelected = mockk<(String) -> Unit>(relaxed = true)

    @Before
    fun setUpNewChatSimSelectorRowTest() {
        clearAllMocks()
    }

    @Test
    fun unavailable_doesNotRender() {
        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
                selectedSubscription = testVerizonSubscription,
                isLoading = true,
            ),
        )

        composeTestRule
            .onNodeWithTag(NEW_CHAT_SIM_SELECTOR_CHIP_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun singleSubscription_doesNotRender() {
        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription),
                selectedSubscription = testVerizonSubscription,
            ),
        )

        composeTestRule
            .onNodeWithTag(NEW_CHAT_SIM_SELECTOR_CHIP_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun availableWithoutSelectedSubscription_doesNotRender() {
        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
                selectedSubscription = null,
            ),
        )

        composeTestRule
            .onNodeWithTag(NEW_CHAT_SIM_SELECTOR_CHIP_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun available_rendersChipWithSelectedLabel() {
        setContent()

        val prefix = targetContext.getString(R.string.new_chat_sim_selector_prefix)
        val chipDescription = targetContext.getString(
            R.string.new_chat_sim_selector_chip_content_description,
            TEST_VERIZON_SUBSCRIPTION_NAME,
        )

        composeTestRule.onNodeWithText(prefix).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(NEW_CHAT_SIM_SELECTOR_CHIP_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(TEST_VERIZON_SUBSCRIPTION_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(chipDescription).assertIsDisplayed()
    }

    @Test
    fun chipClick_opensDropdown() {
        setContent()

        openDropdown()

        composeTestRule
            .onNodeWithTag(NEW_CHAT_SIM_SELECTOR_DROPDOWN_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(
                newChatSimSelectorItemTestTag(
                    selfParticipantId = testVerizonSubscription.selfParticipantId,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(
                newChatSimSelectorItemTestTag(
                    selfParticipantId = testAttSubscription.selfParticipantId,
                ),
            )
            .assertIsDisplayed()
    }

    @Test
    fun dropdown_rendersDestinationWhenPresent() {
        setContent()

        openDropdown()

        composeTestRule
            .onNodeWithText(testVerizonSubscription.displayDestination.orEmpty())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testAttSubscription.displayDestination.orEmpty())
            .assertIsDisplayed()
    }

    @Test
    fun dropdown_omitsDestinationWhenNull() {
        val subscriptionWithoutDestination = testAttSubscription.copy(displayDestination = null)

        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(
                    testVerizonSubscription,
                    subscriptionWithoutDestination,
                ),
                selectedSubscription = testVerizonSubscription,
            ),
        )

        openDropdown()

        composeTestRule.onNodeWithText(TEST_ATT_SUBSCRIPTION_NAME).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testAttSubscription.displayDestination.orEmpty())
            .assertDoesNotExist()
    }

    @Test
    fun selectedSubscription_showsSelectedIconOnlyOnce() {
        setContent()

        openDropdown()

        val selectedDescription = targetContext.getString(R.string.sim_selector_item_selected)

        composeTestRule
            .onAllNodesWithContentDescription(selectedDescription)
            .assertCountEquals(expectedSize = 1)
    }

    @Test
    fun selectingSubscription_callsCallbackAndDismisses() {
        setContent()

        openDropdown()
        composeTestRule
            .onNodeWithTag(
                newChatSimSelectorItemTestTag(
                    selfParticipantId = testAttSubscription.selfParticipantId,
                ),
            )
            .performClick()

        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            onSimSelected.invoke(testAttSubscription.selfParticipantId)
        }
        composeTestRule
            .onNodeWithTag(NEW_CHAT_SIM_SELECTOR_DROPDOWN_TEST_TAG)
            .assertDoesNotExist()
    }

    private fun setContent(
        uiState: ConversationSimSelectorUiState = ConversationSimSelectorUiState(
            subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
            selectedSubscription = testVerizonSubscription,
        ),
    ) {
        composeTestRule.setContent {
            AppTheme {
                NewChatSimSelectorRow(
                    uiState = uiState,
                    onSimSelected = onSimSelected,
                )
            }
        }
    }

    private fun openDropdown() {
        composeTestRule
            .onNodeWithTag(NEW_CHAT_SIM_SELECTOR_CHIP_TEST_TAG)
            .performClick()
    }
}
