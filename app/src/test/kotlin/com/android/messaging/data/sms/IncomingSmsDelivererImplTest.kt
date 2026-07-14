package com.android.messaging.data.sms

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.action.ReceiveSmsMessageAction
import com.android.messaging.sms.MmsUtils
import com.android.messaging.util.DebugUtils
import com.android.messaging.util.PhoneUtils
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IncomingSmsDelivererImplTest {

    private val context: Context = mockk(relaxed = true)
    private val intent: Intent = mockk(relaxed = true)
    private val parser: IncomingSmsParser = mockk()

    // DCS 0 → message class UNKNOWN (not class 0), so it routes to ReceiveSmsMessageAction.
    private val message: SmsMessage = SmsMessage.createFromPdu(SMS_DELIVER_PDU, SMS_FORMAT_3GPP)
    private val deliverer = IncomingSmsDelivererImpl(parser)

    @Before
    fun setUp() {
        mockkStatic(MmsUtils::class, DebugUtils::class, PhoneUtils::class, DataModel::class)
        every { MmsUtils.parseReceivedSmsMessage(any(), any(), any()) } returns ContentValues()
        every { MmsUtils.getMessageDate(any(), any()) } returns RECEIVED_AT
        every { MmsUtils.isDumpSmsEnabled() } returns false
        every { DebugUtils.debugClassZeroSmsEnabled() } returns false
        every { DataModel.executeActionImmediately(any()) } just Runs
        every { DataModel.startActionService(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun deliverFromIntent_executesReceiveActionImmediately() {
        every { parser.parse(intent) } returns arrayOf(message)
        every { PhoneUtils.getDefault() } returns mockk {
            every { getEffectiveIncomingSubIdFromSystem(any(), any()) } returns SUB_ID
        }

        deliverer.deliverFromIntent(context, intent)

        verify { DataModel.executeActionImmediately(any<ReceiveSmsMessageAction>()) }
        verify(exactly = 0) { DataModel.startActionService(any()) }
    }

    @Test
    fun deliver_startsReceiveActionAsynchronously() {
        deliverer.deliver(context, SUB_ID, NO_ERROR_CODE, arrayOf(message))

        verify { DataModel.startActionService(any<ReceiveSmsMessageAction>()) }
        verify(exactly = 0) { DataModel.executeActionImmediately(any()) }
    }

    private companion object {
        private const val SUB_ID = 1
        private const val NO_ERROR_CODE = -1
        private const val RECEIVED_AT = 1_000L
        private const val SMS_FORMAT_3GPP = "3gpp"

        // A valid GSM SMS-DELIVER PDU ("hellohello").
        private const val SMS_DELIVER_PDU_HEX =
            "07911326040000F0040B911346610089F60000208062917314080CC8F71D14969741F977FD07"

        private val SMS_DELIVER_PDU: ByteArray =
            ByteArray(SMS_DELIVER_PDU_HEX.length / 2) { index ->
                SMS_DELIVER_PDU_HEX.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
    }
}
