package com.android.messaging.ui.conversation.entry

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.android.messaging.ui.conversation.NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.entry.model.NewChatUiState
import com.android.messaging.ui.conversation.newChatContactRowTestTag
import com.android.messaging.ui.conversation.recipientpicker.component.row.CONTACT_ID
import com.android.messaging.ui.conversation.recipientpicker.component.row.MOBILE_NORMALIZED_DESTINATION
import com.android.messaging.ui.conversation.recipientpicker.component.row.contactItem
import com.android.messaging.ui.conversation.recipientpicker.component.row.selectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NewChatScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val defaultContactRowTestTag = newChatContactRowTestTag(
        contactId = "contact:$CONTACT_ID",
    )

    @Test
    fun contactClick_forwardsNormalizedDestinationToScreenModel() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(contactItem()),
                ),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(defaultContactRowTestTag)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onContactClicked(destination = MOBILE_NORMALIZED_DESTINATION)
            }
        }
    }

    @Test
    fun queryAndLoadMore_forwardToScreenModel() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
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
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNode(matcher = hasSetTextAction())
            .performTextInput("Ada")
        composeTestRule
            .onNode(matcher = hasScrollToIndexAction())
            .performScrollToIndex(index = 30)
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onQueryChanged(query = "Ada")
                screenModel.onLoadMore()
            }
        }
    }

    @Test
    fun createGroupButton_forwardsRequestToScreenModel() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(contactItem()),
                ),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(testTag = NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onCreateGroupRequested()
            }
        }
    }

    @Test
    fun createGroupMode_togglesRecipientsAndConfirmsSelection() {
        val selectedRecipient = selectedRecipient()
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                isCreatingGroup = true,
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(contactItem()),
                ),
                selectedGroupRecipients = persistentListOf(selectedRecipient),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(defaultContactRowTestTag)
            .assertIsSelected()
            .performClick()
        composeTestRule
            .onNodeWithTag(NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onCreateGroupRecipientClicked(recipient = selectedRecipient)
                screenModel.onCreateGroupConfirmed()
            }
        }
    }

    @Test
    fun longPress_forwardsSelectedRecipientToScreenModel() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(contactItem()),
                ),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(defaultContactRowTestTag)
            .performSemanticsAction(SemanticsActions.OnLongClick)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onContactLongClicked(recipient = selectedRecipient())
            }
        }
    }

    @Test
    fun resolvingState_showsRowProgressIndicatorOnlyForMatchingRecipient() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                isResolvingConversation = true,
                isResolvingConversationIndicatorVisible = true,
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(
                        contactItem(
                            id = 1,
                            normalizedDestination = MOBILE_NORMALIZED_DESTINATION,
                        ),
                        contactItem(
                            id = 2,
                            displayName = "Grace Hopper",
                            destination = "+1 555 0200",
                            normalizedDestination = "+15550200",
                        ),
                    ),
                ),
                resolvingRecipientDestination = MOBILE_NORMALIZED_DESTINATION,
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onAllNodesWithTag(NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
    }

    private fun setContent(screenModel: NewChatScreenModel) {
        composeTestRule.setContent {
            AppTheme {
                NewChatScreen(screenModel = screenModel)
            }
        }
    }

    private fun createScreenModel(initialUiState: NewChatUiState): NewChatScreenModel {
        return mockk<NewChatScreenModel>(relaxed = true) {
            every { effects } returns MutableSharedFlow()
            every { uiState } returns MutableStateFlow(value = initialUiState)
        }
    }
}
