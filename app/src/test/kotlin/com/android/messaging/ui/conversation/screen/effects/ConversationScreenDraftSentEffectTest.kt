package com.android.messaging.ui.conversation.screen.effects

import androidx.compose.ui.test.onNodeWithContentDescription
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.MediaUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenDraftSentEffectTest : BaseConversationScreenEffectsActionTest() {

    @Test
    fun notifyDraftSent_whenSendSoundEnabledPlaysSoundAndAnnouncesSending() {
        val prefs = mockk<BuglePrefs>()
        val mediaUtil = mockk<MediaUtil>(relaxed = true)
        mockkStatic(BuglePrefs::class)
        mockkStatic(MediaUtil::class)
        every { BuglePrefs.getApplicationPrefs() } returns prefs
        every { MediaUtil.get() } returns mediaUtil
        every { prefs.getBoolean(any(), any()) } returns true
        setEffectsContent()

        emitEffect(ConversationScreenEffect.NotifyDraftSent)

        composeTestRule
            .onNodeWithContentDescription(targetContext.getString(R.string.sending_message))
            .assertExists()
        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            mediaUtil.playSound(any(), R.raw.message_sent, null)
        }
    }

    @Test
    fun notifyDraftSent_whenSendSoundDisabledOnlyAnnouncesSending() {
        val prefs = mockk<BuglePrefs>()
        val mediaUtil = mockk<MediaUtil>(relaxed = true)
        mockkStatic(BuglePrefs::class)
        mockkStatic(MediaUtil::class)
        every { BuglePrefs.getApplicationPrefs() } returns prefs
        every { MediaUtil.get() } returns mediaUtil
        every { prefs.getBoolean(any(), any()) } returns false
        setEffectsContent()

        emitEffect(ConversationScreenEffect.NotifyDraftSent)

        composeTestRule
            .onNodeWithContentDescription(targetContext.getString(R.string.sending_message))
            .assertExists()
        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                mediaUtil.playSound(any(), any(), any())
            }
        }
    }
}
