package com.android.messaging.ui.conversation.messages.ui.attachment.rendering

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

private const val ROW_AUDIO_DURATION_MILLIS = 18_000

@RunWith(RobolectricTestRunner::class)
internal class ConversationInlineAttachmentRowAudioPlaybackTest :
    BaseConversationMessageAttachmentRenderingTest() {

    private lateinit var shadowMediaPlayer: ShadowMediaPlayer

    @Before
    fun setUpMediaPlayer() {
        ShadowMediaPlayer.resetStaticState()
        ShadowMediaPlayer.addMediaInfo(
            DataSource.toDataSource(AUDIO_CONTENT_URI),
            ShadowMediaPlayer.MediaInfo(ROW_AUDIO_DURATION_MILLIS, -1),
        )
        ShadowMediaPlayer.setCreateListener { _, shadow ->
            shadowMediaPlayer = shadow
            shadow.setInvalidStateBehavior(ShadowMediaPlayer.InvalidStateBehavior.ASSERT)
        }
    }

    @After
    fun tearDownMediaPlayer() {
        ShadowMediaPlayer.resetStaticState()
    }

    @Test
    fun audioAttachmentRow_clickStartsPlaybackAndShowsPauseAction() {
        val pauseLabel = targetContext.getString(R.string.audio_pause_content_description)

        setInlineAttachmentRowContent(
            attachment = audioInlineAttachment(),
            isSelectionMode = false,
        )

        clickInlineRow()
        shadowMediaPlayer.invokePreparedListener()
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithContentDescription(label = pauseLabel)
            .assertCountEquals(expectedSize = 1)
    }
}
