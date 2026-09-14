package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_INLINE_AUDIO_ATTACHMENT_PLAY_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationInlineAudioAttachmentRowTest :
    BaseConversationInlineAudioAttachmentRowTest() {

    @Test
    fun idleState_hidesProgressIndicator() {
        setContent(
            isPlaying = false,
            progress = 0f,
        )

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun playingState_showsProgressIndicatorWithoutProgress() {
        setContent(
            isPlaying = true,
            progress = 0f,
        )

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
    }

    @Test
    fun progressState_showsProgressIndicatorWhenNotPlaying() {
        setContent(
            isPlaying = false,
            progress = 0.45f,
        )

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
    }

    @Test
    fun playButton_clickTogglesContentDescription() {
        var isPlaying by mutableStateOf(value = false)
        var clicks = 0
        val playLabel = targetContext.getString(R.string.audio_play_content_description)
        val pauseLabel = targetContext.getString(R.string.audio_pause_content_description)

        setContent(
            isPlaying = { isPlaying },
            progress = { 0f },
            onClick = {
                clicks += 1
                isPlaying = !isPlaying
            },
        )

        composeTestRule
            .onAllNodesWithContentDescription(playLabel)
            .assertCountEquals(expectedSize = 1)
        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_INLINE_AUDIO_ATTACHMENT_PLAY_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }

        composeTestRule
            .onAllNodesWithContentDescription(pauseLabel)
            .assertCountEquals(expectedSize = 1)
    }
}
