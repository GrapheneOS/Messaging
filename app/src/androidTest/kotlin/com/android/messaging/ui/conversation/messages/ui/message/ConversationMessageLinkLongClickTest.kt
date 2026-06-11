package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.conversationMessageBubbleTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationMessageLinkLongClickTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun longClickOutgoingLinkOnlyMessageSelectsMessage() {
        var externalUriClickCount = 0
        var messageLongClickCount = 0

        composeTestRule.setContent {
            AppTheme {
                ConversationMessage(
                    message = outgoingMessage(text = LINK_ONLY_TEXT),
                    onExternalUriClick = {
                        externalUriClickCount += 1
                    },
                    onMessageLongClick = {
                        messageLongClickCount += 1
                    },
                )
            }
        }

        awaitLinkAnnotated(text = LINK_ONLY_TEXT)

        composeTestRule
            .onNodeWithText(text = LINK_ONLY_TEXT, useUnmergedTree = true)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, externalUriClickCount)
        }

        composeTestRule
            .onNodeWithText(text = LINK_ONLY_TEXT, useUnmergedTree = true)
            .performTouchInput {
                longClick(position = center)
            }

        composeTestRule.runOnIdle {
            assertEquals(1, externalUriClickCount)
            assertEquals(1, messageLongClickCount)
        }
    }

    @Test
    fun longClickOutgoingLinkOnlyMessageStaysSelectedAfterRelease() {
        var externalUriClickCount = 0
        var messageClickCount = 0
        var messageLongClickCount = 0
        var isSelected by mutableStateOf(false)
        var isSelectionMode by mutableStateOf(false)

        composeTestRule.setContent {
            AppTheme {
                ConversationMessage(
                    message = outgoingMessage(text = LINK_ONLY_TEXT),
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onExternalUriClick = {
                        externalUriClickCount += 1
                    },
                    onMessageClick = {
                        messageClickCount += 1
                        isSelected = !isSelected
                    },
                    onMessageLongClick = {
                        messageLongClickCount += 1
                        isSelectionMode = true
                        isSelected = true
                    },
                )
            }
        }

        awaitLinkAnnotated(text = LINK_ONLY_TEXT)

        composeTestRule
            .onNodeWithText(text = LINK_ONLY_TEXT, useUnmergedTree = true)
            .performTouchInput {
                longClick(position = center)
            }

        composeTestRule.runOnIdle {
            assertEquals(0, externalUriClickCount)
            assertEquals(0, messageClickCount)
            assertEquals(1, messageLongClickCount)
            assertEquals(true, isSelected)
            assertEquals(true, isSelectionMode)
        }
    }

    @Test
    fun longClickOutgoingPlainTextMessageSelectsMessageOnce() {
        var messageLongClickCount = 0

        composeTestRule.setContent {
            AppTheme {
                ConversationMessage(
                    message = outgoingMessage(text = PLAIN_TEXT),
                    onMessageLongClick = {
                        messageLongClickCount += 1
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageBubbleTestTag(
                    messageId = MESSAGE_ID,
                ),
            )
            .performTouchInput {
                longClick(position = center)
            }

        composeTestRule.runOnIdle {
            assertEquals(1, messageLongClickCount)
        }
    }

    @Test
    fun selectionModeDoesNotChangePlainTextMessageHeightAtSmallFontScale() {
        var isSelected by mutableStateOf(false)
        var isSelectionMode by mutableStateOf(false)

        composeTestRule.setContent {
            val density = LocalDensity.current

            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = MINIMAL_FONT_SCALE,
                ),
            ) {
                AppTheme {
                    ConversationMessage(
                        modifier = Modifier.testTag(tag = MESSAGE_TEST_TAG),
                        message = outgoingMessage(text = PLAIN_TEXT),
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        val unselectedHeight = composeTestRule
            .onNodeWithTag(testTag = MESSAGE_TEST_TAG)
            .getUnclippedBoundsInRoot()
            .let { bounds ->
                bounds.bottom - bounds.top
            }

        composeTestRule.runOnIdle {
            isSelected = true
            isSelectionMode = true
        }
        composeTestRule.waitForIdle()

        val selectedHeight = composeTestRule
            .onNodeWithTag(testTag = MESSAGE_TEST_TAG)
            .getUnclippedBoundsInRoot()
            .let { bounds ->
                bounds.bottom - bounds.top
            }

        composeTestRule.runOnIdle {
            assertEquals(
                unselectedHeight.value,
                selectedHeight.value,
                HEIGHT_ASSERTION_DELTA_DP,
            )
        }
    }

    private fun awaitLinkAnnotated(text: String) {
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            composeTestRule
                .onAllNodesWithText(text = text, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .any { node ->
                    node.config
                        .getOrNull(SemanticsProperties.Text)
                        ?.any { it.hasLinkAnnotations(start = 0, end = it.length) } == true
                }
        }
    }

    private fun outgoingMessage(text: String): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = MESSAGE_ID,
            conversationId = CONVERSATION_ID,
            text = text,
            parts = persistentListOf(
                ConversationMessagePartUiModel.Text(
                    text = text,
                ),
            ),
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
            selfParticipantId = null,
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
        private const val MESSAGE_ID = "message-id"
        private const val HEIGHT_ASSERTION_DELTA_DP = 0.5f
        private const val LINK_ONLY_TEXT = "https://example.com"
        private const val MESSAGE_TEST_TAG = "conversation-message"
        private const val MINIMAL_FONT_SCALE = 0.85f
        private const val PLAIN_TEXT = "plain outgoing message"
        private const val TIMESTAMP = 1_700_000_000_000L
    }
}
