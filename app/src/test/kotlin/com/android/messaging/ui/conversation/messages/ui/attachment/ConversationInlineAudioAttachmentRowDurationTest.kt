package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationInlineAudioAttachmentRowDurationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun idleRow_showsTheResolvedDurationWithoutPlayingItFirst() {
        setContent(durationMillis = ROW_AUDIO_DURATION_MILLIS)

        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            composeTestRule
                .onAllNodesWithText(text = "00:18")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun idleRow_withUnknownDuration_fallsBackToZero() {
        setContent(durationMillis = 0L)

        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            composeTestRule
                .onAllNodesWithText(text = "00:00")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun setContent(durationMillis: Long) {
        composeTestRule.setContent {
            AppTheme {
                ConversationInlineAudioAttachmentRow(
                    attachment = ConversationInlineAttachment.Audio(
                        key = "audio-key",
                        contentUri = ROW_AUDIO_CONTENT_URI,
                        openAction = null,
                        titleText = "Audio attachment",
                        titleTextResId = null,
                        durationMillis = durationMillis,
                    ),
                    isIncoming = true,
                    isSelectionMode = false,
                    useStandaloneAudioAttachmentBackground = false,
                    onLongClick = {},
                )
            }
        }
    }

    private companion object {
        private const val ROW_AUDIO_CONTENT_URI = "content://mms/part/row-audio"
        private const val ROW_AUDIO_DURATION_MILLIS = 18_000L
    }
}
