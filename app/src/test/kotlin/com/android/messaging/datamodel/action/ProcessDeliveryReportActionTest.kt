package com.android.messaging.datamodel.action

import android.content.ContentValues
import android.net.Uri
import android.provider.Telephony.Sms
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.sms.MmsUtils
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProcessDeliveryReportActionTest {

    private val dataModel = mockk<DataModel>(relaxed = true)
    private val telephonyTimeSent = slot<Long>()
    private val localValues = slot<ContentValues>()

    @Before
    fun setUp() {
        installTestFactory(
            context = RuntimeEnvironment.getApplication().applicationContext,
            dataModel = dataModel,
        )
        mockkStatic(MmsUtils::class)
        every {
            MmsUtils.updateSmsStatusAndDateSent(any(), any(), capture(telephonyTimeSent))
        } just runs
        mockkStatic(BugleDatabaseOperations::class)
        every { BugleDatabaseOperations.readMessageData(any(), any<Uri>()) } returns
            mockk<MessageData> {
                every { messageId } returns MESSAGE_ID
                every { conversationId } returns CONVERSATION_ID
                every { smsMessageUri } returns SMS_MESSAGE_URI
            }
        every {
            BugleDatabaseOperations.updateMessageRow(any(), any(), capture(localValues))
        } just runs
        mockkStatic(MessagingContentProvider::class)
        every { MessagingContentProvider.notifyMessagesChanged(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun deliveryReportStoresTheSameSentTimestampLocallyAsInTelephony() {
        val before = System.currentTimeMillis()
        ProcessDeliveryReportAction(SMS_MESSAGE_URI, Sms.STATUS_COMPLETE).executeAction()
        val after = System.currentTimeMillis()

        val sentTimestamp = localValues.captured.getAsLong(MessageColumns.SENT_TIMESTAMP)
        assertEquals(telephonyTimeSent.captured, sentTimestamp)
        assertTrue(sentTimestamp in before..after)
    }

    private companion object {
        private val SMS_MESSAGE_URI: Uri = Uri.parse("content://sms/193")
        private const val MESSAGE_ID = "193"
        private const val CONVERSATION_ID = "17"
    }
}
