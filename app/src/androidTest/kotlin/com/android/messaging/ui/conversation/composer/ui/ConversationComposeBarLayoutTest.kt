package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationComposeBarLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun singleLineInputAtDefaultFontScale_keepsTextFieldAndSendButtonHeightsEqual() {
        composeTestRule.setContent {
            val density = LocalDensity.current

            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = DEFAULT_FONT_SCALE,
                ),
            ) {
                AppTheme {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        ConversationComposeBar(
                            audioRecording = ConversationAudioRecordingUiState(),
                            messageText = "Hello",
                            subjectText = "",
                            sendProtocol = ConversationDraftSendProtocol.SMS,
                            segmentCounter = null,
                            isMessageFieldEnabled = true,
                            isAttachmentActionEnabled = false,
                            isRecordActionEnabled = true,
                            isSendActionEnabled = true,
                            shouldShowRecordAction = false,
                            onContactAttachClick = {},
                            onMediaPickerClick = {},
                            onLockedAudioRecordingStartRequest = {},
                            onMessageTextChange = {},
                            onAudioRecordingStartRequest = {},
                            onAudioRecordingFinish = {},
                            onAudioRecordingLock = { false },
                            onAudioRecordingCancel = {},
                            onSendClick = {},
                            onSendActionLongClick = {},
                            onSubjectChipClick = {},
                            onSubjectChipClear = {},
                        )
                    }
                }
            }
        }

        val textFieldBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .getUnclippedBoundsInRoot()

        val sendButtonBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .getUnclippedBoundsInRoot()

        val textFieldHeight = textFieldBounds.bottom - textFieldBounds.top
        val sendButtonHeight = sendButtonBounds.bottom - sendButtonBounds.top

        assertEquals(
            textFieldHeight.value,
            sendButtonHeight.value,
            HEIGHT_ASSERTION_DELTA_DP,
        )
    }

    private companion object {
        private const val DEFAULT_FONT_SCALE = 1f
        private const val HEIGHT_ASSERTION_DELTA_DP = 0.5f
    }
}
