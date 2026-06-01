package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.testutil.performDisabledTouchClick
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_MMS_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSegmentCounterUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationComposeBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun multiLineInput_growsTextFieldWithoutGrowingSendButton() {
        setContent(
            messageText = "Line 1\nLine 2\nLine 3\nLine 4",
            subjectText = "",
        )

        val textFieldBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .getUnclippedBoundsInRoot()
        val sendButtonBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .getUnclippedBoundsInRoot()

        assertTrue(textFieldBounds.height > sendButtonBounds.height)
    }

    @Test
    fun mmsSendProtocol_showsMmsIndicatorAndStateDescription() {
        setContent(
            messageText = "Hello",
            subjectText = "",
            sendProtocol = ConversationDraftSendProtocol.MMS,
            segmentCounter = null,
        )

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_MMS_INDICATOR_TEST_TAG,
                useUnmergedTree = true,
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    targetContext.getString(R.string.mms_text),
                ),
            )
    }

    @Test
    fun smsSendProtocol_hidesMmsIndicator() {
        setContent(
            messageText = "Hello",
            subjectText = "",
            sendProtocol = ConversationDraftSendProtocol.SMS,
            segmentCounter = null,
        )

        composeTestRule
            .onAllNodesWithTag(
                testTag = CONVERSATION_MMS_INDICATOR_TEST_TAG,
                useUnmergedTree = true,
            )
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun enabledState_andCallbacks_areWiredCorrectly() {
        var messageText = ""
        var currentMessageText by mutableStateOf(value = "")
        var sendClicks = 0

        setContent(
            messageText = { currentMessageText },
            isSendActionEnabled = true,
            onMessageTextChange = { updatedText ->
                currentMessageText = updatedText
                messageText = updatedText
            },
            onSendClick = {
                sendClicks += 1
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .performTextInput("Hello")

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .assertIsEnabled()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals("Hello", messageText)
            assertEquals(1, sendClicks)
        }
    }

    @Test
    fun sendButton_canBeDisabled() {
        var sendClicks = 0

        setContent(
            messageText = "Hello",
            isSendActionEnabled = false,
            onSendClick = {
                sendClicks += 1
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performDisabledTouchClick()

        composeTestRule.runOnIdle {
            assertEquals(0, sendClicks)
        }
    }

    @Test
    fun textField_canBeDisabled() {
        setContent(
            messageText = "",
            isMessageFieldEnabled = false,
            isSendActionEnabled = false,
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .assertIsNotEnabled()
    }

    @Test
    fun attachmentButton_performsHapticFeedbackAndOpensMenu() {
        val hapticFeedback = createHapticFeedbackMock()

        setContent(
            messageText = "",
            isSendActionEnabled = false,
            isAttachmentActionEnabled = true,
            hapticFeedback = hapticFeedback,
        )

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .assertCountEquals(expectedSize = 0)

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG)
            .assertCountEquals(expectedSize = 1)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            }
        }
    }

    @Test
    fun attachmentMenuMediaItem_forwardsCallback() {
        var mediaClicks = 0

        setContent(
            messageText = "",
            isSendActionEnabled = false,
            isAttachmentActionEnabled = true,
            onMediaPickerClick = {
                mediaClicks += 1
            },
        )

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, mediaClicks)
        }
    }

    @Test
    fun attachmentMenuAudioItem_forwardsLockedRecordingStartRequest() {
        var audioClicks = 0

        setContent(
            messageText = "Message with text",
            isSendActionEnabled = true,
            isAttachmentActionEnabled = true,
            onLockedAudioRecordingStartRequest = {
                audioClicks += 1
            },
        )

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, audioClicks)
        }
    }

    @Test
    fun attachmentMenuContactItem_forwardsCallback() {
        var contactClicks = 0

        setContent(
            messageText = "",
            isSendActionEnabled = false,
            isAttachmentActionEnabled = true,
            onContactAttachClick = {
                contactClicks += 1
            },
        )

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, contactClicks)
        }
    }

    @Test
    fun attachmentButton_canBeDisabled() {
        setContent(
            messageText = "",
            subjectText = "",
            isSendActionEnabled = false,
            isAttachmentActionEnabled = false,
        )

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun emptyMessage_withRecordActionVisible_showsRecordButton() {
        setContent(
            messageText = "",
            subjectText = "",
            shouldShowRecordAction = true,
        )

        composeTestRule
            .onNodeWithContentDescription(
                targetContext.getString(R.string.audio_record_view_content_description),
            )
            .assertIsDisplayed()
    }

    @Test
    fun recordingState_showsRecordingBarWithLockAffordanceAboveSendButton() {
        setContent(
            audioRecording = ConversationAudioRecordingUiState(
                phase = ConversationAudioRecordingPhase.Recording,
                durationMillis = 1_000L,
            ),
            messageText = "",
            subjectText = "",
            shouldShowRecordAction = true,
        )

        val sendButtonBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .getUnclippedBoundsInRoot()
        val lockAffordanceBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG)
            .getUnclippedBoundsInRoot()

        composeTestRule
            .onNodeWithTag(CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG)
            .assertIsDisplayed()

        assertTrue(lockAffordanceBounds.bottom.value <= sendButtonBounds.top.value + 8f)
        assertTrue(
            kotlin.math.abs(
                (
                    (lockAffordanceBounds.left.value + lockAffordanceBounds.right.value) / 2f
                    ) - (
                    (sendButtonBounds.left.value + sendButtonBounds.right.value) / 2f
                    ),
            ) <= 8f,
        )
    }

    @Test
    fun lockedRecordingState_showsStopButtonFromUiState() {
        setContent(
            audioRecording = recordingAudioState(isLocked = true),
            messageText = "",
            subjectText = "",
            shouldShowRecordAction = false,
        )

        composeTestRule
            .onNodeWithContentDescription(
                targetContext.getString(R.string.audio_record_stop_content_description),
            )
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun activeRecording_controlsStayEnabledWhenRecordStartActionIsDisabled() {
        var finishRequests = 0
        setContent(
            audioRecording = recordingAudioState(isLocked = true),
            messageText = "",
            subjectText = "",
            isRecordActionEnabled = false,
            shouldShowRecordAction = false,
            onAudioRecordingFinish = {
                finishRequests += 1
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .assertIsEnabled()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, finishRequests)
        }
    }

    @Test
    fun longPressRecordButton_startsAndFinishesRecording() {
        var audioRecording by mutableStateOf(ConversationAudioRecordingUiState())
        var startRequests = 0
        var finishRequests = 0
        var cancelRequests = 0

        setContent(
            audioRecording = { audioRecording },
            messageText = { "" },
            isSendActionEnabled = false,
            shouldShowRecordAction = { true },
            onAudioRecordingStartRequest = {
                startRequests += 1
                audioRecording = recordingAudioState()
            },
            onAudioRecordingFinish = {
                finishRequests += 1
                audioRecording = ConversationAudioRecordingUiState()
            },
            onAudioRecordingCancel = {
                cancelRequests += 1
                audioRecording = ConversationAudioRecordingUiState()
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                advanceEventTime(durationMillis = 700L)
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, startRequests)
            assertEquals(1, finishRequests)
            assertEquals(0, cancelRequests)
        }
    }

    @Test
    fun longPressAndDragLeft_cancelsRecording() {
        var audioRecording by mutableStateOf(ConversationAudioRecordingUiState())
        var startRequests = 0
        var finishRequests = 0
        var cancelRequests = 0
        val cancelDragDistancePx = with(composeTestRule.density) {
            (AUDIO_RECORD_CANCEL_THRESHOLD + 24.dp).toPx()
        }

        setContent(
            audioRecording = { audioRecording },
            messageText = { "" },
            isSendActionEnabled = false,
            shouldShowRecordAction = { true },
            onAudioRecordingStartRequest = {
                startRequests += 1
                audioRecording = recordingAudioState()
            },
            onAudioRecordingFinish = {
                finishRequests += 1
                audioRecording = ConversationAudioRecordingUiState()
            },
            onAudioRecordingCancel = {
                cancelRequests += 1
                audioRecording = ConversationAudioRecordingUiState()
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                advanceEventTime(durationMillis = 700L)
                moveBy(
                    Offset(
                        x = -cancelDragDistancePx,
                        y = 0f,
                    ),
                )
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, startRequests)
            assertEquals(0, finishRequests)
            assertEquals(1, cancelRequests)
        }
    }

    @Test
    fun lockGesture_emitsConfirmHapticAndKeepsRecordingActiveUntilStopTap() {
        var audioRecording by mutableStateOf(ConversationAudioRecordingUiState())
        var startRequests = 0
        var lockRequests = 0
        var finishRequests = 0
        var cancelRequests = 0
        var shouldShowRecordAction by mutableStateOf(true)
        val hapticFeedback = createHapticFeedbackMock()
        val lockDragDistancePx = with(composeTestRule.density) {
            (AUDIO_RECORD_LOCK_THRESHOLD + 24.dp).toPx()
        }

        setContent(
            audioRecording = { audioRecording },
            messageText = { "" },
            isSendActionEnabled = false,
            shouldShowRecordAction = { shouldShowRecordAction },
            hapticFeedback = hapticFeedback,
            onAudioRecordingStartRequest = {
                startRequests += 1
                shouldShowRecordAction = false
                audioRecording = recordingAudioState()
            },
            onAudioRecordingFinish = {
                finishRequests += 1
                audioRecording = finalizingAudioState()
            },
            onAudioRecordingLock = {
                lockRequests += 1
                audioRecording = recordingAudioState(isLocked = true)
                true
            },
            onAudioRecordingCancel = {
                cancelRequests += 1
                audioRecording = ConversationAudioRecordingUiState()
                shouldShowRecordAction = true
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                advanceEventTime(durationMillis = 700L)
                moveBy(
                    Offset(
                        x = 0f,
                        y = -lockDragDistancePx,
                    ),
                )
                up()
            }

        composeTestRule
            .onNodeWithContentDescription(
                targetContext.getString(R.string.audio_record_stop_content_description),
            )
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            assertEquals(1, startRequests)
            assertEquals(1, lockRequests)
            assertEquals(0, finishRequests)
            assertEquals(0, cancelRequests)
            assertEquals(ConversationAudioRecordingPhase.Recording, audioRecording.phase)
            assertTrue(audioRecording.isLocked)
            verify(exactly = 1) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG)
            .assertCountEquals(expectedSize = 0)

        composeTestRule.runOnIdle {
            assertEquals(1, finishRequests)
            assertEquals(0, cancelRequests)
            assertEquals(ConversationAudioRecordingPhase.Finalizing, audioRecording.phase)
        }
    }

    @Test
    fun lockedRecording_canStillSlideLeftToCancel() {
        var audioRecording by mutableStateOf(ConversationAudioRecordingUiState())
        var finishRequests = 0
        var cancelRequests = 0
        val lockDragDistancePx = with(composeTestRule.density) {
            (AUDIO_RECORD_LOCK_THRESHOLD + 24.dp).toPx()
        }
        val cancelDragDistancePx = with(composeTestRule.density) {
            (AUDIO_RECORD_CANCEL_THRESHOLD + 24.dp).toPx()
        }

        setContent(
            audioRecording = { audioRecording },
            messageText = { "" },
            isSendActionEnabled = false,
            shouldShowRecordAction = { true },
            onAudioRecordingStartRequest = {
                audioRecording = recordingAudioState()
            },
            onAudioRecordingFinish = {
                finishRequests += 1
                audioRecording = ConversationAudioRecordingUiState()
            },
            onAudioRecordingLock = {
                audioRecording = recordingAudioState(isLocked = true)
                true
            },
            onAudioRecordingCancel = {
                cancelRequests += 1
                audioRecording = ConversationAudioRecordingUiState()
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                advanceEventTime(durationMillis = 700L)
                moveBy(
                    Offset(
                        x = 0f,
                        y = -lockDragDistancePx,
                    ),
                )
                up()
            }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                moveBy(
                    Offset(
                        x = -cancelDragDistancePx,
                        y = 0f,
                    ),
                )
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, finishRequests)
            assertEquals(1, cancelRequests)
            assertEquals(ConversationAudioRecordingPhase.Idle, audioRecording.phase)
        }
    }

    private fun setContent(
        audioRecording: ConversationAudioRecordingUiState = ConversationAudioRecordingUiState(),
        messageText: String,
        subjectText: String = "",
        sendProtocol: ConversationDraftSendProtocol = ConversationDraftSendProtocol.SMS,
        segmentCounter: ConversationSegmentCounterUiState? = null,
        isMessageFieldEnabled: Boolean = true,
        isSendActionEnabled: Boolean = true,
        isAttachmentActionEnabled: Boolean = false,
        isRecordActionEnabled: Boolean = true,
        shouldShowRecordAction: Boolean = false,
        hapticFeedback: HapticFeedback? = null,
        onContactAttachClick: () -> Unit = {},
        onMediaPickerClick: () -> Unit = {},
        onLockedAudioRecordingStartRequest: () -> Unit = {},
        onMessageTextChange: (String) -> Unit = {},
        onAudioRecordingStartRequest: () -> Unit = {},
        onAudioRecordingFinish: () -> Unit = {},
        onAudioRecordingLock: () -> Boolean = { false },
        onAudioRecordingCancel: () -> Unit = {},
        onSendClick: () -> Unit = {},
        onSendActionLongClick: () -> Unit = {},
        onSubjectChipClick: () -> Unit = {},
        onSubjectChipClear: () -> Unit = {},
    ) {
        setContent(
            audioRecording = { audioRecording },
            messageText = { messageText },
            subjectText = subjectText,
            sendProtocol = sendProtocol,
            segmentCounter = segmentCounter,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isSendActionEnabled = isSendActionEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            shouldShowRecordAction = { shouldShowRecordAction },
            hapticFeedback = hapticFeedback,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            onMessageTextChange = onMessageTextChange,
            onAudioRecordingStartRequest = onAudioRecordingStartRequest,
            onAudioRecordingFinish = onAudioRecordingFinish,
            onAudioRecordingLock = onAudioRecordingLock,
            onAudioRecordingCancel = onAudioRecordingCancel,
            onSendClick = onSendClick,
            onSendActionLongClick = onSendActionLongClick,
            onSubjectChipClick = onSubjectChipClick,
            onSubjectChipClear = onSubjectChipClear,
        )
    }

    private fun setContent(
        audioRecording: () -> ConversationAudioRecordingUiState =
            { ConversationAudioRecordingUiState() },
        messageText: () -> String,
        subjectText: String = "",
        sendProtocol: ConversationDraftSendProtocol = ConversationDraftSendProtocol.SMS,
        segmentCounter: ConversationSegmentCounterUiState? = null,
        isMessageFieldEnabled: Boolean = true,
        isSendActionEnabled: Boolean = true,
        isAttachmentActionEnabled: Boolean = false,
        isRecordActionEnabled: Boolean = true,
        shouldShowRecordAction: () -> Boolean = { false },
        hapticFeedback: HapticFeedback? = null,
        onContactAttachClick: () -> Unit = {},
        onMediaPickerClick: () -> Unit = {},
        onLockedAudioRecordingStartRequest: () -> Unit = {},
        onMessageTextChange: (String) -> Unit = {},
        onAudioRecordingStartRequest: () -> Unit = {},
        onAudioRecordingFinish: () -> Unit = {},
        onAudioRecordingLock: () -> Boolean = { false },
        onAudioRecordingCancel: () -> Unit = {},
        onSendClick: () -> Unit = {},
        onSendActionLongClick: () -> Unit = {},
        onSubjectChipClick: () -> Unit = {},
        onSubjectChipClear: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            val content: @Composable () -> Unit = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    ConversationComposeBar(
                        audioRecording = audioRecording(),
                        messageText = messageText(),
                        subjectText = subjectText,
                        sendProtocol = sendProtocol,
                        segmentCounter = segmentCounter,
                        isMessageFieldEnabled = isMessageFieldEnabled,
                        isAttachmentActionEnabled = isAttachmentActionEnabled,
                        isRecordActionEnabled = isRecordActionEnabled,
                        isSendActionEnabled = isSendActionEnabled,
                        shouldShowRecordAction = shouldShowRecordAction(),
                        onContactAttachClick = onContactAttachClick,
                        onMediaPickerClick = onMediaPickerClick,
                        onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
                        onMessageTextChange = onMessageTextChange,
                        onAudioRecordingStartRequest = onAudioRecordingStartRequest,
                        onAudioRecordingFinish = onAudioRecordingFinish,
                        onAudioRecordingLock = onAudioRecordingLock,
                        onAudioRecordingCancel = onAudioRecordingCancel,
                        onSendClick = onSendClick,
                        onSendActionLongClick = onSendActionLongClick,
                        onSubjectChipClick = onSubjectChipClick,
                        onSubjectChipClear = onSubjectChipClear,
                    )
                }
            }

            hapticFeedback?.let { feedback ->
                CompositionLocalProvider(LocalHapticFeedback provides feedback) {
                    AppTheme(content = content)
                }
            } ?: AppTheme(content = content)
        }
    }

    private fun createHapticFeedbackMock(): HapticFeedback {
        val hapticFeedback = mockk<HapticFeedback>()
        every {
            hapticFeedback.performHapticFeedback(any())
        } just runs
        return hapticFeedback
    }

    private fun recordingAudioState(
        durationMillis: Long = 0L,
        isLocked: Boolean = false,
    ): ConversationAudioRecordingUiState {
        return ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Recording,
            durationMillis = durationMillis,
            isLocked = isLocked,
        )
    }

    private fun finalizingAudioState(): ConversationAudioRecordingUiState {
        return ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Finalizing,
        )
    }
}
