package com.android.messaging.datamodel

import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.action.InsertNewMessageAction
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.UIIntents
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NoConfirmationSmsSendServiceRespondViaMessageTest {

    @Before
    fun setUp() {
        installTestFactory(
            context = RuntimeEnvironment.getApplication().applicationContext,
        )
        mockkStatic(InsertNewMessageAction::class)
        every {
            InsertNewMessageAction.insertNewMessage(any(), any(), any(), any())
        } just runs
        every { InsertNewMessageAction.insertNewMessage(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun respondViaMessageWithoutConversationSendsReplyWithoutNotificationUpdate() {
        val intent = respondViaMessageIntent()

        TestNoConfirmationSmsSendService().handle(intent)

        verify(exactly = 1) {
            InsertNewMessageAction.insertNewMessage(
                ParticipantData.DEFAULT_SELF_SUB_ID,
                RECIPIENT,
                REPLY_TEXT,
                null,
            )
        }
    }

    @Test
    fun respondViaMessageWithConversationStillUpdatesNotificationInlineReply() {
        mockkStatic(BugleNotifications::class)
        every { BugleNotifications.updateWithInlineReply(any(), any()) } just runs
        val intent = respondViaMessageIntent().putExtra(
            UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID,
            CONVERSATION_ID,
        )

        TestNoConfirmationSmsSendService().handle(intent)

        val message = slot<MessageData>()
        verify(exactly = 1) { InsertNewMessageAction.insertNewMessage(capture(message)) }
        assertEquals(CONVERSATION_ID, message.captured.conversationId)
        verify(exactly = 1) {
            BugleNotifications.updateWithInlineReply(CONVERSATION_ID, REPLY_TEXT)
        }
    }

    private fun respondViaMessageIntent(): Intent {
        return Intent(
            TelephonyManager.ACTION_RESPOND_VIA_MESSAGE,
            Uri.parse("smsto:$RECIPIENT"),
        ).putExtra(Intent.EXTRA_TEXT, REPLY_TEXT)
    }

    private class TestNoConfirmationSmsSendService : NoConfirmationSmsSendService() {
        fun handle(intent: Intent) {
            onHandleIntent(intent)
        }
    }

    private companion object {
        private const val RECIPIENT = "+15551234567"
        private const val REPLY_TEXT = "Can't talk right now, what's up?"
        private const val CONVERSATION_ID = "194"
    }
}
