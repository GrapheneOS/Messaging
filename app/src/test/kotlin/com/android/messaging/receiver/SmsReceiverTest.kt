package com.android.messaging.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.Telephony.Sms
import android.telephony.SubscriptionManager
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.secondaryuser.SecondaryUserMessageResolver
import com.android.messaging.data.secondaryuser.SecondaryUserNotificationTargetResolver
import com.android.messaging.data.secondaryuser.SecondaryUserNotifier
import com.android.messaging.data.secondaryuser.model.SecondaryUserMessageInfo
import com.android.messaging.data.secondaryuser.model.SecondaryUserNotificationTarget
import com.android.messaging.data.sms.IncomingSmsParser
import com.android.messaging.di.receiver.IncomingSmsEntryPoint
import com.android.messaging.sms.MmsUtils
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.OsUtil
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.EntryPointAccessors
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmsReceiverTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val phoneUtils = mockk<PhoneUtils>(relaxed = true)
    private val incomingSmsParser = mockk<IncomingSmsParser>()
    private val messageResolver = mockk<SecondaryUserMessageResolver>()
    private val targetResolver = mockk<SecondaryUserNotificationTargetResolver>()
    private val notifier = mockk<SecondaryUserNotifier>(relaxed = true)
    private val entryPoint = mockk<IncomingSmsEntryPoint>()

    @Before
    fun setUp() {
        installTestFactory(context = context, phoneUtils = phoneUtils)
        every { phoneUtils.isSmsEnabled } returns true
        every {
            phoneUtils.getEffectiveIncomingSubIdFromSystem(
                any<Intent>(),
                SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            )
        } returns SUBSCRIPTION_ID

        mockkStatic(OsUtil::class)
        every { OsUtil.isSecondaryUser() } returns true

        mockkStatic(MmsUtils::class)
        every { MmsUtils.parseReceivedSmsMessage(any(), any(), any()) } returns contentValuesOf(
            Sms.ADDRESS to SENDER,
            Sms.BODY to BODY,
        )

        mockkStatic(EntryPointAccessors::class)
        every {
            EntryPointAccessors.fromApplication(any(), IncomingSmsEntryPoint::class.java)
        } returns entryPoint
        every { entryPoint.applicationScope() } returns CoroutineScope(Dispatchers.Unconfined)
        every { entryPoint.ioDispatcher() } returns Dispatchers.Unconfined
        every { entryPoint.incomingSmsParser() } returns incomingSmsParser
        every { entryPoint.secondaryUserMessageResolver() } returns messageResolver
        every { entryPoint.secondaryUserNotificationTargetResolver() } returns targetResolver
        every { entryPoint.secondaryUserNotifier() } returns notifier

        every { incomingSmsParser.parse(any()) } returns emptyArray()
        every { messageResolver.resolve(address = SENDER, body = BODY) } returns MESSAGE_INFO
        every {
            targetResolver.resolve(address = SENDER, subscriptionId = SUBSCRIPTION_ID)
        } returns TARGET
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun smsReceived_inSecondaryUser_notifiesOnTheSendersTarget() {
        val isBroadcastFinished = sendSmsReceived()

        verify(exactly = 1) {
            notifier.notifyIncomingMessage(info = MESSAGE_INFO, target = TARGET)
        }
        assertTrue("the broadcast is never finished", isBroadcastFinished)
    }

    @Test
    fun smsReceived_inPrimaryUser_doesNotNotify() {
        every { OsUtil.isSecondaryUser() } returns false

        sendSmsReceived()

        verify(exactly = 0) { notifier.notifyIncomingMessage(info = any(), target = any()) }
    }

    @Test
    fun smsReceived_withSmsDisabled_doesNotNotify() {
        every { phoneUtils.isSmsEnabled } returns false

        sendSmsReceived()

        verify(exactly = 0) { notifier.notifyIncomingMessage(info = any(), target = any()) }
    }

    // Telephony sends SMS_RECEIVED ordered; the result receiver runs once every receiver finishes.
    private fun sendSmsReceived(): Boolean {
        var isFinished = false
        context.sendOrderedBroadcast(
            Intent(Sms.Intents.SMS_RECEIVED_ACTION)
                .putExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, SUBSCRIPTION_ID),
            null,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    isFinished = true
                }
            },
            null,
            Activity.RESULT_OK,
            null,
            null,
        )
        shadowOf(Looper.getMainLooper()).idle()
        return isFinished
    }

    private companion object {
        const val SUBSCRIPTION_ID = 7
        const val SENDER = "+15551234567"
        const val BODY = "Hello"
        val MESSAGE_INFO = SecondaryUserMessageInfo(sender = "Alice", body = BODY)
        val TARGET = SecondaryUserNotificationTarget.Conversation(
            conversationId = "42",
            conversationTitle = "Alice",
        )
    }
}
