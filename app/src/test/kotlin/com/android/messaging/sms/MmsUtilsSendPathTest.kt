package com.android.messaging.sms

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.testutil.TEST_CONVERSATION_ID_VALUE as CONVERSATION_ID
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The app holds no media read permissions, so an attachment uri carried by an old message can be
 * unreadable by the time that message is sent or resent. The send path has to fail the message
 * instead of letting the SecurityException escape the action service and kill the process.
 */
@RunWith(RobolectricTestRunner::class)
class MmsUtilsSendPathTest {

    private val contentResolver = mockk<ContentResolver>()
    private val context = mockk<Context>()

    @Before
    fun setUp() {
        every { context.contentResolver } returns contentResolver
        installTestFactory(context = context)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun insertSendingMmsMessage_returnsNullWhenAnAttachmentIsNoLongerReadable() {
        every { contentResolver.openInputStream(any()) } throws SecurityException(
            "Permission Denial: opening provider com.android.providers.media.MediaProvider",
        )

        val messageUri = MmsUtils.insertSendingMmsMessage(
            context,
            listOf(RECIPIENT),
            createMmsDraftWithImageAttachment(),
            SUB_ID,
            SELF_PHONE_NUMBER,
            TIMESTAMP,
        )

        assertNull(messageUri)
        verify { contentResolver.openInputStream(ATTACHMENT_URI) }
    }

    private fun createMmsDraftWithImageAttachment(): MessageData {
        val messageData = MessageData.createDraftMmsMessage(
            CONVERSATION_ID,
            "self-1",
            "Hello",
            "Subject",
        )
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "image/jpeg",
                ATTACHMENT_URI,
                320,
                240,
            ),
        )

        return messageData
    }

    private companion object {
        private const val RECIPIENT = "+15555550100"
        private const val SELF_PHONE_NUMBER = "+15555550101"
        private const val SUB_ID = 1
        private const val TIMESTAMP = 1_700_000_000_000L
        private val ATTACHMENT_URI: Uri = Uri.parse("content://media/external/images/media/1")
    }
}
