package com.android.messaging.datamodel.action

import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = OutgoingPipelineTestApplication::class, sdk = [36])
abstract class OutgoingPipelineTestBase {

    internal val world = OutgoingPipelineWorld()
    internal val telephony = world.telephony
    internal val driver: OutgoingPipelineDriver = LegacyOutgoingPipelineDriver(world = world)

    @Before
    fun setUpWorld() {
        world.setUp()
    }

    @After
    fun tearDownWorld() {
        world.tearDown()
    }

    internal fun now(): Long {
        return System.currentTimeMillis()
    }

    internal fun statusOf(messageId: String): Int {
        return world.message(messageId = messageId).status
    }

    companion object {
        internal const val RECIPIENT_A = "+15550100001"
        internal const val RECIPIENT_B = "+15550100002"
        internal const val RECIPIENT_C = "+15550100003"
        internal const val SHORT_TEXT = "hello"
        internal const val RETRY_REQUEST_DELAY_FIRST_MILLIS = 5_000L
        internal const val RETRY_REQUEST_DELAY_SECOND_MILLIS = 10_000L
        internal const val RESEND_WINDOW_MILLIS = 20 * 60 * 1_000L
    }
}
