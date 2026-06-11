package com.android.messaging.ui.conversation.messages.ui.message.rendering

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.ui.conversation.conversationMessageSelectionRowTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
                    messageId = DEFAULT_MESSAGE_ID,
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
                    messageId = DEFAULT_MESSAGE_ID,
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
}
