package com.android.messaging.datamodel.action

import android.app.PendingIntent
import android.content.Context
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.testutil.installTestFactory
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RedownloadMmsPendingIntentTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        installTestFactory(context = context)
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun downloadIntentsForTwoConversationsKeepTheirOwnMessage() {
        val first = redownloadPendingIntent("message-1")
        val second = redownloadPendingIntent("message-2")

        assertNotEquals(first, second)
        assertEquals("message-1", messageIdOf(first))
        assertEquals("message-2", messageIdOf(second))
    }

    @Test
    fun downloadIntentForTheSameMessageIsReusedRatherThanDuplicated() {
        val first = redownloadPendingIntent("message-1")
        val reposted = redownloadPendingIntent("message-1")

        assertEquals(first, reposted)
    }

    private fun redownloadPendingIntent(messageId: String): PendingIntent =
        RedownloadMmsAction.getPendingIntentForRedownloadMms(
            context,
            messageId,
            BugleNotifications.REQUEST_CODE_REDOWNLOAD_MMS,
        )

    private fun messageIdOf(pendingIntent: PendingIntent): String? {
        val actionBundle = shadowOf(pendingIntent).savedIntent
            .getBundleExtra(ActionServiceImpl.EXTRA_ACTION_BUNDLE)!!
        val action = actionBundle.getParcelable(
            ActionServiceImpl.BUNDLE_ACTION,
            Action::class.java,
        )!!
        return action.actionParameters.getString("message_id")
    }
}
