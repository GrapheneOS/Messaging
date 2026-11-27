package com.android.messaging.datamodel.action

import android.content.ContentValues
import android.provider.Telephony.Sms
import com.android.messaging.R
import com.android.messaging.datamodel.BugleNotifications
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = OutgoingPipelineTestApplication::class, sdk = [36])
abstract class IncomingPipelineTestBase {

    internal val world = OutgoingPipelineWorld()
    internal val telephony = world.telephony
    internal val incomingMms = world.incomingMms
    internal val driver: IncomingPipelineDriver = LegacyIncomingPipelineDriver(world = world)

    @Before
    fun setUpWorld() {
        world.setUp()
    }

    @After
    fun tearDownWorld() {
        world.tearDown()
    }

    internal fun onlyConversationId(): String {
        return world.conversationIds().single()
    }

    internal fun onlyMessage(): IncomingMessageRow {
        return world.allIncomingMessages().single()
    }

    internal fun telephonySmsInboxInserts(): List<ContentValues> {
        return shadowOf(world.context.contentResolver).insertStatements
            .filter { statement -> statement.uri == Sms.Inbox.CONTENT_URI }
            .map { statement -> statement.contentValues }
    }

    internal fun threadIdFor(vararg recipients: String): Long {
        return telephony.threadIdFor(recipients = recipients.toList())
    }

    internal fun messageNotificationFor(conversationId: String): ConversationNotificationUpdate {
        return ConversationNotificationUpdate(
            conversationId = conversationId,
            coverage = BugleNotifications.UPDATE_ALL,
        )
    }

    internal fun disableMmsAutoRetrieve(subId: Int) {
        world.prefsFor(subId = subId).putBoolean(
            world.context.getString(R.string.auto_retrieve_mms_pref_key),
            false,
        )
    }

    companion object {
        internal const val SENDER_A = "+15550100001"
        internal const val SENDER_B = "+15550100002"
        internal const val SENDER_C = "+15550100003"
        internal const val TEXT = "hello"
        internal const val SECOND_TEXT = "again"
        internal const val SERVICE_CENTER = "+15550109999"
    }
}
