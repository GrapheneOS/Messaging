package com.android.messaging.ui.conversation.metadata.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.testutil.TEST_CALL_ACTION_PHONE_NUMBER
import com.android.messaging.ui.conversation.CONVERSATION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.TEST_ATT_SUBSCRIPTION_NAME
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.testAttSubscription
import com.android.messaging.ui.conversation.testVerizonSubscription
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.util.AccessibilityUtil
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationTopAppBarSimSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun simSelectorMenuItem_isHiddenWhenOnlyOneSubscriptionIsAvailable() {
        setContent(
            simSelector = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription),
                selectedSubscription = testVerizonSubscription,
            ),
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun simSelectorMenuItem_showsSelectedSubscriptionLabelWhenExpanded() {
        setContent(
            simSelector = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
                selectedSubscription = testAttSubscription,
            ),
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(TEST_ATT_SUBSCRIPTION_NAME)
            .assertIsDisplayed()
    }

    @Test
    fun simSelectorMenuItem_clickInvokesCallbackAndDismissesMenu() {
        var clicks = 0

        setContent(
            simSelector = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
                selectedSubscription = testVerizonSubscription,
            ),
            onSimSelectorClick = { clicks += 1 },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun oneOnOneConversation_showsDisplayDestinationSubtitleAndAccessibilityLabel() {
        setContent(simSelector = ConversationSimSelectorUiState())

        composeTestRule
            .onAllNodesWithText("(555) 123-4567")
            .assertCountEquals(expectedSize = 1)
        composeTestRule
            .onNodeWithText(TEST_CALL_ACTION_PHONE_NUMBER)
            .assertDoesNotExist()

        val expectedContentDescription = getVocalizedPhoneNumber(phoneNumber = "(555) 123-4567")

        composeTestRule
            .onNodeWithContentDescription(expectedContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun oneOnOneConversation_hidesSubtitleWhenItMatchesTitle() {
        setContent(
            metadata = presentMetadata.copy(
                title = "(555) 123-4567",
            ),
        )

        composeTestRule
            .onAllNodesWithText("(555) 123-4567")
            .assertCountEquals(expectedSize = 1)
    }

    @Test
    fun groupConversation_showsParticipantCountSubtitle() {
        val participantCountText = targetContext.resources.getQuantityString(
            R.plurals.wearable_participant_count,
            3,
            3,
        )

        setContent(
            metadata = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Group,
                participantCount = 3,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = null,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.Editable,
            ),
        )

        composeTestRule
            .onNodeWithText(participantCountText)
            .assertIsDisplayed()
    }

    private fun setContent(
        metadata: ConversationMetadataUiState = presentMetadata,
        simSelector: ConversationSimSelectorUiState = ConversationSimSelectorUiState(),
        onSimSelectorClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationTopAppBar(
                    metadata = metadata,
                    simSelector = simSelector,
                    onAddPeopleClick = {},
                    onSimSelectorClick = onSimSelectorClick,
                    onTitleClick = {},
                    onNavigateBack = {},
                )
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun getVocalizedPhoneNumber(phoneNumber: String): String {
        return AccessibilityUtil.getVocalizedPhoneNumber(
            targetContext.resources,
            phoneNumber,
        )
    }

    private companion object {
        private val presentMetadata = ConversationMetadataUiState.Present(
            title = "Carol",
            selfParticipantId = "self-1",
            avatar = ConversationMetadataUiState.Avatar.Single(
                photoUri = null,
            ),
            participantCount = 1,
            otherParticipantDisplayDestination = "(555) 123-4567",
            otherParticipantPhoneNumber = TEST_CALL_ACTION_PHONE_NUMBER,
            otherParticipantContactLookupKey = null,
            isArchived = false,
            composerAvailability = ConversationComposerAvailability.Editable,
        )
    }
}
