package com.android.messaging.ui.conversation.screen.effects

import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.view.View
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.MessageDetailsDialog
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.util.ContactUtil
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenImmediateEffectsTest : BaseConversationScreenEffectsActionTest() {

    @Test
    fun closeConversation_invokesNavigationCallback() {
        var navigationCount = 0
        setEffectsContent(
            onNavigateBack = {
                navigationCount += 1
            },
        )

        emitEffect(ConversationScreenEffect.CloseConversation)

        composeTestRule.runOnIdle {
            assertEquals(1, navigationCount)
        }
    }

    @Test
    fun openExternalUri_forwardsToUiIntents() {
        val uiIntents = mockk<UIIntents>(relaxed = true)
        mockkStatic(UIIntents::class)
        every { UIIntents.get() } returns uiIntents
        setEffectsContent()

        emitEffect(ConversationScreenEffect.OpenExternalUri(uri = EXTERNAL_URL))

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            uiIntents.launchBrowserForUrl(any(), EXTERNAL_URL)
        }
    }

    @Test
    fun placePhoneCall_forwardsPhoneNumberAndZeroOrigin() {
        val uiIntents = mockk<UIIntents>(relaxed = true)
        val pointSlot = slot<Point>()
        mockkStatic(UIIntents::class)
        every { UIIntents.get() } returns uiIntents
        setEffectsContent()

        emitEffect(ConversationScreenEffect.PlacePhoneCall(phoneNumber = PHONE_NUMBER))

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            uiIntents.launchPhoneCallActivity(any(), PHONE_NUMBER, capture(pointSlot))
        }
        assertEquals(0, pointSlot.captured.x)
        assertEquals(0, pointSlot.captured.y)
    }

    @Test
    fun launchAddContactFlow_forwardsDestinationToUiIntents() {
        val uiIntents = mockk<UIIntents>(relaxed = true)
        mockkStatic(UIIntents::class)
        every { UIIntents.get() } returns uiIntents
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.LaunchAddContactFlow(
                destination = CONTACT_DESTINATION,
            ),
        )

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            uiIntents.launchAddContactActivity(any(), CONTACT_DESTINATION)
        }
    }

    @Test
    fun launchForwardMessage_forwardsMessageToUiIntents() {
        val uiIntents = mockk<UIIntents>(relaxed = true)
        val message = mockk<MessageData>()
        mockkStatic(UIIntents::class)
        every { UIIntents.get() } returns uiIntents
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.LaunchForwardMessage(
                message = message,
            ),
        )

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            uiIntents.launchForwardMessageActivity(any(), message)
        }
    }

    @Test
    fun openVCardAttachmentPreview_forwardsUriToUiIntents() {
        val uiIntents = mockk<UIIntents>(relaxed = true)
        val contentUri = "content://attachments/contact-card"
        mockkStatic(UIIntents::class)
        every { UIIntents.get() } returns uiIntents
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.OpenAttachmentPreview(
                contentType = ContentType.TEXT_VCARD,
                contentUri = contentUri,
                imageCollectionUri = null,
            ),
        )

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            uiIntents.launchVCardDetailActivity(any(), Uri.parse(contentUri))
        }
    }

    @Test
    fun openVideoAttachmentPreview_forwardsUriToUiIntents() {
        val uiIntents = mockk<UIIntents>(relaxed = true)
        val contentUri = "content://attachments/video"
        mockkStatic(UIIntents::class)
        every { UIIntents.get() } returns uiIntents
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.OpenAttachmentPreview(
                contentType = ContentType.VIDEO_MP4,
                contentUri = contentUri,
                imageCollectionUri = null,
            ),
        )

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            uiIntents.launchFullScreenVideoViewer(any(), Uri.parse(contentUri))
        }
    }

    @Test
    fun showOrAddParticipantContact_forwardsContactDetails() {
        val avatarUri = Uri.parse("content://avatar/1")
        mockkStatic(ContactUtil::class)
        every {
            ContactUtil.showOrAddContact(
                any<View>(),
                42L,
                "lookup-key",
                avatarUri,
                CONTACT_DESTINATION,
            )
        } just runs
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.ShowOrAddParticipantContact(
                contactId = 42L,
                contactLookupKey = "lookup-key",
                avatarUri = avatarUri,
                normalizedDestination = CONTACT_DESTINATION,
            ),
        )

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            ContactUtil.showOrAddContact(
                any<View>(),
                42L,
                "lookup-key",
                avatarUri,
                CONTACT_DESTINATION,
            )
        }
    }

    @Test
    fun showMessageDetails_forwardsMessageAndParticipants() {
        val message = mockk<ConversationMessageData>()
        val participants = mockk<ConversationParticipantsData>()
        val selfParticipant = mockk<ParticipantData>()
        mockkStatic(MessageDetailsDialog::class)
        every {
            MessageDetailsDialog.show(
                any<Context>(),
                message,
                participants,
                selfParticipant,
            )
        } just runs
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.ShowMessageDetails(
                message = message,
                participants = participants,
                selfParticipant = selfParticipant,
            ),
        )

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            MessageDetailsDialog.show(
                any<Context>(),
                message,
                participants,
                selfParticipant,
            )
        }
    }

    private companion object {
        private const val EXTERNAL_URL = "https://example.com/message"
        private const val PHONE_NUMBER = "+15551234567"
        private const val CONTACT_DESTINATION = "+15557654321"
    }
}
