package com.android.messaging.ui.conversation.messages.ui.list

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageBubbleTestTag
import com.android.messaging.ui.conversation.conversationMessageItemTestTag
import com.android.messaging.ui.conversation.conversationMessageSelectionRowTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.ui.ConversationMessages
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessagesListRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onAttachmentClick = mockk<(String, String, String) -> Unit>(relaxed = true)
    private val onExternalUriClick = mockk<(String) -> Unit>(relaxed = true)
    private val onMessageAvatarClick = mockk<(String) -> Unit>(relaxed = true)
    private val onMessageClick = mockk<(String) -> Unit>(relaxed = true)
    private val onMessageDownloadClick = mockk<(String) -> Unit>(relaxed = true)
    private val onMessageLongClick = mockk<(String) -> Unit>(relaxed = true)
    private val onMessageResendClick = mockk<(String) -> Unit>(relaxed = true)
    private val onSimSelectorClick = mockk<() -> Unit>(relaxed = true)

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun emptyList_rendersScrollableMessageContainerWithoutRows() {
        setMessagesContent(messages = persistentListOf())

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(testTag = conversationMessageItemTestTag(messageId = FIRST_MESSAGE_ID))
            .assertDoesNotExist()
    }

    @Test
    fun presentMessages_renderRowsAndForwardLongClickWithMessageId() {
        setMessagesContent(
            messages = persistentListOf(
                message(
                    messageId = FIRST_MESSAGE_ID,
                    text = FIRST_MESSAGE_TEXT,
                ),
                message(
                    messageId = SECOND_MESSAGE_ID,
                    text = SECOND_MESSAGE_TEXT,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(text = FIRST_MESSAGE_TEXT)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = SECOND_MESSAGE_TEXT)
            .assertIsDisplayed()

        longClickBubble(messageId = SECOND_MESSAGE_ID)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageLongClick.invoke(SECOND_MESSAGE_ID)
            }
            verify(exactly = 0) {
                onMessageClick.invoke(any())
            }
        }
    }

    @Test
    fun selectionModeSelectedRow_clickAndLongClickForwardMessageId() {
        setMessagesContent(
            messages = persistentListOf(
                message(
                    messageId = FIRST_MESSAGE_ID,
                    text = FIRST_MESSAGE_TEXT,
                ),
            ),
            selectedMessageIds = persistentSetOf(FIRST_MESSAGE_ID),
        )

        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageSelectionRowTestTag(
                    messageId = FIRST_MESSAGE_ID,
                ),
            )
            .assertIsSelected()
            .performClick()

        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageSelectionRowTestTag(
                    messageId = FIRST_MESSAGE_ID,
                ),
            )
            .performSemanticsAction(SemanticsActions.OnLongClick)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageClick.invoke(FIRST_MESSAGE_ID)
            }
            verify(exactly = 1) {
                onMessageLongClick.invoke(FIRST_MESSAGE_ID)
            }
        }
    }

    @Test
    fun sendSimIndicator_enabledForwardsClick() {
        val annotationText = targetContext.getString(
            R.string.conversation_send_sim_annotation,
            SEND_SIM_DISPLAY_NAME,
        )

        setMessagesContent(
            messages = persistentListOf(),
            currentSendSimDisplayName = SEND_SIM_DISPLAY_NAME,
        )

        clickSendSimAnnotation(annotationText = annotationText)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onSimSelectorClick.invoke()
            }
        }
    }

    @Test
    fun sendSimIndicator_selectionModeDisablesClick() {
        val annotationText = targetContext.getString(
            R.string.conversation_send_sim_annotation,
            SEND_SIM_DISPLAY_NAME,
        )

        setMessagesContent(
            messages = persistentListOf(
                message(
                    messageId = FIRST_MESSAGE_ID,
                    text = FIRST_MESSAGE_TEXT,
                ),
            ),
            selectedMessageIds = persistentSetOf(FIRST_MESSAGE_ID),
            currentSendSimDisplayName = SEND_SIM_DISPLAY_NAME,
        )

        clickSendSimAnnotation(annotationText = annotationText)

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onSimSelectorClick.invoke()
            }
        }
    }

    private fun setMessagesContent(
        messages: ImmutableList<ConversationMessageUiModel>,
        selectedMessageIds: ImmutableSet<String> = persistentSetOf(),
        currentSendSimDisplayName: String? = null,
    ) {
        composeTestRule.setContent {
            AppTheme {
                val listState = rememberLazyListState()

                ConversationMessages(
                    messages = messages,
                    listState = listState,
                    selectedMessageIds = selectedMessageIds,
                    subscriptions = persistentListOf(sendSubscription()),
                    currentSendSimDisplayName = currentSendSimDisplayName,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onMessageClick = onMessageClick,
                    onMessageAvatarClick = onMessageAvatarClick,
                    onMessageDownloadClick = onMessageDownloadClick,
                    onMessageLongClick = onMessageLongClick,
                    onMessageResendClick = onMessageResendClick,
                    onSimSelectorClick = onSimSelectorClick,
                )
            }
        }
    }

    private fun clickSendSimAnnotation(annotationText: String) {
        composeTestRule
            .onNodeWithText(text = annotationText)
            .performTouchInput {
                click(
                    position = Offset(
                        x = width * SEND_SIM_LINK_X_FRACTION,
                        y = height * SEND_SIM_LINK_Y_FRACTION,
                    ),
                )
            }
    }

    @Suppress("SameParameterValue")
    private fun longClickBubble(messageId: String) {
        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageBubbleTestTag(messageId = messageId),
            )
            .performSemanticsAction(SemanticsActions.OnLongClick)
    }

    private fun sendSubscription(): Subscription {
        return Subscription(
            selfParticipantId = SELF_PARTICIPANT_ID,
            subId = SEND_SUBSCRIPTION_ID,
            label = ConversationSubscriptionLabel.Named(name = SEND_SIM_DISPLAY_NAME),
            displayDestination = null,
            displaySlotId = SEND_SUBSCRIPTION_SLOT,
            color = SEND_SUBSCRIPTION_COLOR,
        )
    }

    private fun message(
        messageId: String,
        text: String,
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = messageId,
            conversationId = CONVERSATION_ID,
            text = text,
            parts = persistentListOf(),
            sentTimestamp = TIMESTAMP,
            receivedTimestamp = TIMESTAMP,
            displayTimestamp = TIMESTAMP,
            status = ConversationMessageUiModel.Status.Outgoing.Complete,
            isIncoming = false,
            senderDisplayName = null,
            senderAvatarUri = null,
            senderContactId = ParticipantData.PARTICIPANT_CONTACT_ID_NOT_RESOLVED,
            senderContactLookupKey = null,
            senderNormalizedDestination = null,
            senderParticipantId = null,
            selfParticipantId = SELF_PARTICIPANT_ID,
            canClusterWithPrevious = false,
            canClusterWithNext = false,
            canCopyMessageToClipboard = true,
            canDownloadMessage = false,
            canForwardMessage = true,
            canResendMessage = false,
            canSaveAttachments = false,
            mmsDownload = null,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.SMS,
        )
    }

    private companion object {
        private const val FIRST_MESSAGE_ID = "message-1"
        private const val FIRST_MESSAGE_TEXT = "First visible message"
        private const val SECOND_MESSAGE_ID = "message-2"
        private const val SECOND_MESSAGE_TEXT = "Second visible message"
        private const val SELF_PARTICIPANT_ID = "self-1"
        private const val SEND_SIM_DISPLAY_NAME = "Work SIM"
        private const val SEND_SIM_LINK_X_FRACTION = 0.8f
        private const val SEND_SIM_LINK_Y_FRACTION = 0.5f
        private const val SEND_SUBSCRIPTION_COLOR = 0
        private const val SEND_SUBSCRIPTION_ID = 1
        private const val SEND_SUBSCRIPTION_SLOT = 0
        private const val TIMESTAMP = 1_700_000_000_000L
    }
}
