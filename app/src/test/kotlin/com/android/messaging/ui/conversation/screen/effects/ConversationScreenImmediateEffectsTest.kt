package com.android.messaging.ui.conversation.screen.effects

import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Intents
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.contact.model.AddContactRequest
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.screen.model.ConversationScreenNavEvent
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenImmediateEffectsTest : BaseConversationScreenEffectsActionTest() {

    @Test
    fun closeConversation_invokesNavigationCallback() {
        var navigationCount = 0
        setEffectsContent(
            onCloseConversation = {
                navigationCount += 1
            },
        )

        emitNavigationEvent(ConversationScreenNavEvent.CloseConversation)

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
    fun launchAddContactFlow_startsContactEditorIntentWithDestination() {
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.LaunchAddContactFlow(
                destination = CONTACT_DESTINATION,
            ),
        )

        val activity = composeTestRule.activity
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            shadowOf(activity).peekNextStartedActivity() != null
        }
        val intent = requireNotNull(shadowOf(activity).nextStartedActivity)
        assertEquals(Intent.ACTION_INSERT_OR_EDIT, intent.action)
        assertEquals(Contacts.CONTENT_ITEM_TYPE, intent.type)
        assertEquals(
            CONTACT_DESTINATION,
            intent.getStringExtra(Intents.Insert.PHONE),
        )
    }

    @Test
    fun openVCardAttachmentPreview_navigatesToVCardDetail() {
        val contentUri = "content://attachments/contact-card"
        var navigatedUri: String? = null
        setEffectsContent(
            onNavigateToVCardDetail = { uri -> navigatedUri = uri },
        )

        emitEffect(
            ConversationScreenEffect.NavigateToVCardDetail(uri = contentUri),
        )

        assertEquals(contentUri, navigatedUri)
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
    fun addParticipantContact_forwardsRequestToNavigation() {
        val addContactRequests = mutableListOf<AddContactRequest>()
        setEffectsContent(
            onNavigateToAddContact = { request -> addContactRequests += request },
        )

        emitEffect(
            ConversationScreenEffect.AddParticipantContact(
                request = AddContactRequest(
                    destination = CONTACT_DESTINATION,
                    avatarUri = "content://avatar/1",
                ),
            ),
        )

        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            addContactRequests.isNotEmpty()
        }
        assertEquals(
            listOf(
                AddContactRequest(
                    destination = CONTACT_DESTINATION,
                    avatarUri = "content://avatar/1",
                ),
            ),
            addContactRequests,
        )
    }

    @Test
    fun navigateToMessageDetails_forwardsMessageId() {
        var navigatedMessageId: MessageId? = null
        setEffectsContent(
            onNavigateToMessageDetails = { messageId -> navigatedMessageId = messageId },
        )

        emitNavigationEvent(
            ConversationScreenNavEvent.NavigateToMessageDetails(
                messageId = MessageId("message-1"),
            ),
        )

        assertThat(navigatedMessageId).isEqualTo(MessageId("message-1"))
    }

    private companion object {
        private const val EXTERNAL_URL = "https://example.com/message"
        private const val PHONE_NUMBER = "+15551234567"
        private const val CONTACT_DESTINATION = "+15557654321"
    }
}
