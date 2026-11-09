package com.android.messaging.data.secondaryuser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import com.android.messaging.Factory
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.secondaryuser.model.SecondaryUserMessageInfo
import com.android.messaging.data.secondaryuser.model.SecondaryUserNotificationTarget
import com.android.messaging.testutil.createIncomingMessagesTestChannel
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.UIIntentsImpl
import com.android.messaging.util.NotificationChannelUtil
import io.mockk.every
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecondaryUserNotifierTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val notifier = SecondaryUserNotifierImpl(
        context = context,
        notificationManager = notificationManager,
        generateNotificationId = { NOTIFICATION_ID },
    )

    @Before
    fun setUp() {
        ShadowNotificationManager.reset()
        installTestFactory(context = context)
        every { Factory.get().getUIIntents() } returns UIIntentsImpl()
        createIncomingMessagesTestChannel()
    }

    @After
    fun tearDown() {
        ShadowNotificationManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun notifyIncomingMessage_conversation_postsOnConversationChannel() {
        notifier.notifyIncomingMessage(info = MESSAGE, target = CONVERSATION_TARGET)

        assertEquals(CONVERSATION_ID, postedNotification().notification.channelId)
        val channel = NotificationChannelUtil.getConversationChannel(CONVERSATION_ID)
        assertEquals(NotificationChannelUtil.INCOMING_MESSAGES, channel?.parentChannelId)
        assertEquals(CONVERSATION_TITLE, channel?.name)
    }

    @Test
    fun notifyIncomingMessage_conversationWithSilencedChannel_keepsChannelSilent() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CONVERSATION_ID,
                CONVERSATION_TITLE,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setConversationId(NotificationChannelUtil.INCOMING_MESSAGES, CONVERSATION_ID)
                setSound(null, null)
            },
        )

        notifier.notifyIncomingMessage(info = MESSAGE, target = CONVERSATION_TARGET)

        assertEquals(CONVERSATION_ID, postedNotification().notification.channelId)
        assertNull(NotificationChannelUtil.getConversationChannel(CONVERSATION_ID)?.sound)
    }

    @Test
    fun notifyIncomingMessage_noConversation_postsOnConversationsChannel() {
        notifier.notifyIncomingMessage(
            info = MESSAGE,
            target = SecondaryUserNotificationTarget.NoConversation,
        )

        assertEquals(
            NotificationChannelUtil.INCOMING_MESSAGES,
            postedNotification().notification.channelId,
        )
        assertNull(NotificationChannelUtil.getConversationChannel(CONVERSATION_ID))
    }

    @Test
    fun notifyIncomingMessage_silenced_postsNothing() {
        notifier.notifyIncomingMessage(
            info = MESSAGE,
            target = SecondaryUserNotificationTarget.Silenced,
        )

        assertTrue(notificationManager.activeNotifications.isEmpty())
        assertNull(NotificationChannelUtil.getConversationChannel(CONVERSATION_ID))
    }

    @Test
    fun cancel_removesPostedNotification() {
        notifier.notifyIncomingMessage(info = MESSAGE, target = CONVERSATION_TARGET)

        notifier.cancel()

        assertTrue(notificationManager.activeNotifications.isEmpty())
    }

    private fun postedNotification(): StatusBarNotification {
        return notificationManager.activeNotifications.single()
    }

    private companion object {
        const val NOTIFICATION_ID = 84
        const val CONVERSATION_ID = "42"
        const val CONVERSATION_TITLE = "Alice"
        val MESSAGE = SecondaryUserMessageInfo(sender = CONVERSATION_TITLE, body = "Hello")
        val CONVERSATION_TARGET = SecondaryUserNotificationTarget.Conversation(
            conversationId = CONVERSATION_ID,
            conversationTitle = CONVERSATION_TITLE,
        )
    }
}
