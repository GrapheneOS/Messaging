package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.ui.conversation.CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConversationInlineAudioAttachmentRowAnimationTest :
    BaseConversationInlineAudioAttachmentRowTest() {

    @Test
    fun progressIndicator_animatesInAndOutAsPlaybackStateChanges() {
        composeTestRule.mainClock.autoAdvance = false

        var isPlaying by mutableStateOf(value = false)

        setContent(
            isPlaying = { isPlaying },
            progress = { 0f },
        )

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(expectedSize = 0)

        composeTestRule.runOnIdle {
            isPlaying = true
        }
        composeTestRule.mainClock.advanceTimeBy(milliseconds = 500L)

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(expectedSize = 1)

        composeTestRule.runOnIdle {
            isPlaying = false
        }
        composeTestRule.mainClock.advanceTimeBy(milliseconds = 500L)

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
    }
}
