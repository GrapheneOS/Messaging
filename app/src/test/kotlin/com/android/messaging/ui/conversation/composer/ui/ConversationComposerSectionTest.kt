package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_SATELLITE_NOTICE_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSatelliteNotice
import com.android.messaging.ui.core.MessagingPreviewTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationComposerSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun composerSection_tracksSatelliteNoticeChangesInUiState() {
        val state = mutableStateOf(ConversationComposerUiState())
        composeTestRule.setContent {
            MessagingPreviewTheme {
                ConversationComposerSection(
                    uiState = state.value,
                    messageFieldFocusRequester = FocusRequester(),
                    onContactAttachClick = {},
                    onMediaPickerClick = {},
                    onMessageTextChange = { _, _ -> },
                    onPendingAttachmentRemove = {},
                    onResolvedAttachmentClick = {},
                    onResolvedAttachmentRemove = {},
                    onAudioRecordingStartRequest = {},
                    onLockedAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingLock = { true },
                    onAudioRecordingCancel = {},
                    onSendClick = {},
                    onSendActionLongClick = {},
                    onSubjectChipClick = {},
                    onSubjectChipClear = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(CONVERSATION_SATELLITE_NOTICE_TEST_TAG).assertDoesNotExist()

        val notices = listOf(
            ConversationSatelliteNotice.TextOnly to
                R.string.conversation_satellite_notice_text_only,
            ConversationSatelliteNotice.TextAndMultimedia to
                R.string.conversation_satellite_notice_text_and_multimedia,
            ConversationSatelliteNotice.MessagingUnavailable to
                R.string.conversation_satellite_notice_messaging_unavailable,
        )
        for ((notice, messageResId) in notices) {
            composeTestRule.runOnIdle {
                state.value = state.value.copy(satelliteNotice = notice)
            }
            composeTestRule
                .onNodeWithTag(CONVERSATION_SATELLITE_NOTICE_TEST_TAG)
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(targetContext.getString(messageResId))
                .assertIsDisplayed()
        }

        composeTestRule.runOnIdle {
            state.value = state.value.copy(satelliteNotice = null)
        }
        composeTestRule.onNodeWithTag(CONVERSATION_SATELLITE_NOTICE_TEST_TAG).assertDoesNotExist()
    }
}
