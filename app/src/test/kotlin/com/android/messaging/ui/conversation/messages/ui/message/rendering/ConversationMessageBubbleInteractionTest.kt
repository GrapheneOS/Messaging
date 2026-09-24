package com.android.messaging.ui.conversation.messages.ui.message.rendering

import android.content.Context
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.conversationMessageBubbleTestTag
import com.android.messaging.ui.conversation.conversationMessageSelectionRowTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import com.android.messaging.ui.conversation.messages.ui.message.ConversationMessage
import com.android.messaging.ui.core.AppTheme
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageBubbleInteractionTest :
    BaseConversationMessageRenderingTest() {

    @Test
    fun downloadableMmsMessage_clickForwardsDownloadOnly() {
        setConversationMessageContent(
            message = message(
                status = ConversationMessageUiModel.Status.Incoming.YetToManualDownload,
                isIncoming = true,
                canDownloadMessage = true,
                mmsDownload = mmsDownload(
                    state = MmsDownloadUiModel.State.AwaitingManualDownload,
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
            ),
        )

        clickBubble()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onDownloadClick.invoke()
            }
            verify(exactly = 0) {
                onResendClick.invoke()
            }
            verify(exactly = 0) {
                onAttachmentClick.invoke(any(), any(), any())
            }
        }
    }

    @Test
    fun downloadBlockedMmsMessage_clickIsNoOp() {
        setConversationMessageContent(
            message = message(
                status = ConversationMessageUiModel.Status.Incoming.YetToManualDownload,
                isIncoming = true,
                canDownloadMessage = false,
                mmsDownload = mmsDownload(
                    state = MmsDownloadUiModel.State.AwaitingManualDownload,
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
            ),
        )

        clickBubble()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onDownloadClick.invoke()
            }
            verify(exactly = 0) {
                onResendClick.invoke()
            }
            verify(exactly = 0) {
                onMessageClick.invoke()
            }
        }
    }

    @Test
    fun resendableTextMessage_clickForwardsResendOnly() {
        setConversationMessageContent(
            message = message(
                status = ConversationMessageUiModel.Status.Outgoing.Failed,
                canResendMessage = true,
            ),
        )

        clickBubble()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onResendClick.invoke()
            }
            verify(exactly = 0) {
                onDownloadClick.invoke()
            }
            verify(exactly = 0) {
                onMessageClick.invoke()
            }
        }
    }

    @Test
    fun visualAttachmentClick_normalModeForwardsAttachmentOpen() {
        setConversationMessageContent(
            message = message(
                text = null,
                parts = persistentListOf(
                    imagePart(),
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS,
            ),
            showIncomingParticipantIdentity = false,
        )

        clickBubble()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(IMAGE_CONTENT_TYPE, IMAGE_CONTENT_URI, "")
            }
            verify(exactly = 0) {
                onMessageClick.invoke()
            }
        }
    }

    @Test
    fun visualAttachmentClick_selectionModeForwardsMessageClick() {
        setConversationMessageContent(
            message = message(
                text = null,
                parts = persistentListOf(
                    imagePart(),
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS,
            ),
            isSelected = true,
            isSelectionMode = true,
            showIncomingParticipantIdentity = false,
        )

        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageSelectionRowTestTag(
                    messageId = MessageId(DEFAULT_MESSAGE_ID),
                ),
            )
            .assertIsSelected()

        clickSelectionRow()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageClick.invoke()
            }
            verify(exactly = 0) {
                onAttachmentClick.invoke(any(), any(), any())
            }
        }
    }

    @Test
    fun selectedDownloadMessage_selectionModeClickForwardsMessageClickOnly() {
        setConversationMessageContent(
            message = message(
                status = ConversationMessageUiModel.Status.Incoming.DownloadFailed,
                isIncoming = true,
                canDownloadMessage = true,
                mmsDownload = mmsDownload(
                    state = MmsDownloadUiModel.State.DownloadFailed,
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
            ),
            isSelected = true,
            isSelectionMode = true,
        )

        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageSelectionRowTestTag(
                    messageId = MessageId(DEFAULT_MESSAGE_ID),
                ),
            )
            .assertIsSelected()

        clickSelectionRow()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageClick.invoke()
            }
            verify(exactly = 0) {
                onDownloadClick.invoke()
            }
            verify(exactly = 0) {
                onResendClick.invoke()
            }
        }
    }

    @Test
    fun bubbleLongClick_forwardsMessageLongClickOnly() {
        setConversationMessageContent(
            message = message(),
        )

        longClickBubble()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageLongClick.invoke()
            }
            verify(exactly = 0) {
                onMessageClick.invoke()
            }
            verify(exactly = 0) {
                onDownloadClick.invoke()
            }
            verify(exactly = 0) {
                onResendClick.invoke()
            }
        }
    }

    @Test
    fun incomingAvatarClick_forwardsAvatarClick() {
        setConversationMessageContent(
            message = message(
                status = ConversationMessageUiModel.Status.Incoming.Complete,
                isIncoming = true,
                senderDisplayName = "Nora",
            ),
        )

        composeTestRule
            .onNodeWithText(text = "N")
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAvatarClick.invoke()
            }
        }
    }

    @Test
    fun resendableTextMessage_tapBesideBubbleIsNoOp() {
        setConversationMessageContent(
            message = message(
                status = ConversationMessageUiModel.Status.Outgoing.Failed,
                canResendMessage = true,
            ),
        )

        touchMessageRow { click(position = centerLeft) }

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onResendClick.invoke()
            }
        }
    }

    @Test
    fun downloadableMmsMessage_tapBesideBubbleIsNoOp() {
        setConversationMessageContent(
            message = message(
                status = ConversationMessageUiModel.Status.Incoming.YetToManualDownload,
                isIncoming = true,
                canDownloadMessage = true,
                mmsDownload = mmsDownload(
                    state = MmsDownloadUiModel.State.AwaitingManualDownload,
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
            ),
        )

        touchMessageRow { click(position = centerRight) }

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onDownloadClick.invoke()
            }
        }
    }

    @Test
    fun longClickBesideBubbleIsNoOp() {
        setConversationMessageContent(
            message = message(),
        )

        touchMessageRow { longClick(position = centerLeft) }

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onMessageLongClick.invoke()
            }
        }
    }

    @Test
    fun selectionMode_tapBesideBubbleForwardsMessageClick() {
        setConversationMessageContent(
            message = message(),
            isSelectionMode = true,
        )

        touchMessageRow { click(position = centerLeft) }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageClick.invoke()
            }
        }
    }

    @Test
    fun resendableTextMessage_pressLeavingTheBubbleNeitherResendsNorSelects() {
        setConversationMessageContent(
            message = message(
                status = ConversationMessageUiModel.Status.Outgoing.Failed,
                canResendMessage = true,
            ),
        )

        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageBubbleTestTag(
                    messageId = MessageId(DEFAULT_MESSAGE_ID),
                ),
                useUnmergedTree = true,
            )
            .performTouchInput {
                down(position = center)
                moveTo(position = centerLeft - Offset(x = 24.dp.toPx(), y = 0f))
                advanceEventTime(durationMillis = viewConfiguration.longPressTimeoutMillis * 2)
                up()
            }

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onMessageLongClick.invoke()
            }
            verify(exactly = 0) {
                onResendClick.invoke()
            }
        }
    }

    @Test
    fun heldEnterKeyWithoutKeyRepeat_forwardsMessageLongClick() {
        setConversationMessageContent(
            message = message(),
        )
        val messageRow = composeTestRule.onNodeWithTag(
            testTag = conversationMessageSelectionRowTestTag(
                messageId = MessageId(DEFAULT_MESSAGE_ID),
            ),
        )

        messageRow.requestFocus()
        messageRow.performKeyInput { keyDown(key = Key.Enter) }
        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = ViewConfiguration.getLongPressTimeout() * 2L,
        )
        messageRow.performKeyInput { keyUp(key = Key.Enter) }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageLongClick.invoke()
            }
        }
    }

    @Test
    fun touchExploration_longClickBesideBubbleForwardsMessageLongClick() {
        enableTouchExploration()
        setConversationMessageContent(
            message = message(),
        )

        touchMessageRow { longClick(position = centerLeft) }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageLongClick.invoke()
            }
        }
    }

    @Test
    fun touchExploration_resendableTextMessage_longClickBesideBubbleForwardsMessageLongClickOnly() {
        enableTouchExploration()
        setConversationMessageContent(
            message = message(
                status = ConversationMessageUiModel.Status.Outgoing.Failed,
                canResendMessage = true,
            ),
        )

        touchMessageRow { longClick(position = centerLeft) }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageLongClick.invoke()
            }
            verify(exactly = 0) {
                onResendClick.invoke()
            }
        }
    }

    @Test
    fun touchExploration_visualAttachmentClickOfMessageWithoutTapIsNoOp() {
        enableTouchExploration()
        setConversationMessageContent(
            message = message(
                text = null,
                parts = persistentListOf(
                    imagePart(),
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS,
            ),
            showIncomingParticipantIdentity = false,
        )

        clickBubble()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onAttachmentClick.invoke(any(), any(), any())
            }
            verify(exactly = 0) {
                onMessageLongClick.invoke()
            }
        }
    }

    @Test
    fun heldEnterKeyWithKeyRepeat_selectsOnlyTheFocusedMessageWhichKeepsFocus() {
        var selectedMessageIds by mutableStateOf(emptySet<String>())
        val focusedMessageIds = mutableListOf<String>()
        val toggleSelection = { messageId: String ->
            selectedMessageIds = when (messageId) {
                in selectedMessageIds -> selectedMessageIds - messageId
                else -> selectedMessageIds + messageId
            }
        }
        composeTestRule.setContent {
            AppTheme {
                Column {
                    listOf(DEFAULT_MESSAGE_ID, SECOND_MESSAGE_ID).forEach { messageId ->
                        Box(
                            modifier = Modifier.onFocusChanged { focusState ->
                                if (focusState.hasFocus) {
                                    focusedMessageIds += messageId
                                }
                            },
                        ) {
                            ConversationMessage(
                                message = message(messageId = messageId),
                                isSelected = messageId in selectedMessageIds,
                                isSelectionMode = selectedMessageIds.isNotEmpty(),
                                onMessageClick = { toggleSelection(messageId) },
                                onMessageLongClick = { toggleSelection(messageId) },
                            )
                        }
                    }
                }
            }
        }
        val secondMessageRow = composeTestRule.onNodeWithTag(
            testTag = conversationMessageSelectionRowTestTag(
                messageId = MessageId(SECOND_MESSAGE_ID),
            ),
        )

        secondMessageRow.requestFocus()
        secondMessageRow.performKeyInput {
            keyDown(key = Key.Enter)
            advanceEventTime(durationMillis = viewConfiguration.longPressTimeoutMillis * 4)
            keyUp(key = Key.Enter)
        }

        composeTestRule.runOnIdle {
            assertEquals(setOf(SECOND_MESSAGE_ID), selectedMessageIds)
            assertEquals(listOf(SECOND_MESSAGE_ID), focusedMessageIds)
        }
        secondMessageRow.assertIsFocused()
    }

    private fun touchMessageRow(block: TouchInjectionScope.() -> Unit) {
        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageSelectionRowTestTag(
                    messageId = MessageId(DEFAULT_MESSAGE_ID),
                ),
            )
            .performTouchInput(block = block)
    }

    private companion object {
        const val SECOND_MESSAGE_ID = "message-2"
    }
}

internal fun enableTouchExploration() {
    val accessibilityManager = ApplicationProvider
        .getApplicationContext<Context>()
        .getSystemService(AccessibilityManager::class.java)
    shadowOf(accessibilityManager).setTouchExplorationEnabled(true)
}
