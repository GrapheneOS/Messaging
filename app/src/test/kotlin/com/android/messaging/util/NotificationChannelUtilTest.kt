package com.android.messaging.util

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import com.android.messaging.FactoryTestAccess
import com.android.messaging.testutil.createIncomingMessagesTestChannel
import com.android.messaging.testutil.installTestFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationChannelUtilTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        ShadowNotificationManager.reset()
        context = RuntimeEnvironment.getApplication().applicationContext
        installTestFactory(context = context)
        createIncomingMessagesTestChannel()
    }

    @After
    fun tearDown() {
        ShadowNotificationManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun createConversationChannelFromLegacyCreatesDisabledChannelForMutedConversation() {
        val conversationId = "9701"

        val channel = NotificationChannelUtil.createConversationChannelFromLegacy(
            conversationId = conversationId,
            conversationTitle = "Muted conversation",
            legacyNotificationsEnabled = false,
            legacyVibrationEnabled = true,
        )

        assertEquals(NotificationManager.IMPORTANCE_NONE, channel.importance)
        assertEquals(conversationId, channel.conversationId)
        assertNotNull(NotificationChannelUtil.getConversationChannel(conversationId))
    }

    @Test
    fun createConversationChannelFromLegacyCopiesLegacySoundAndVibrationForEnabledConversation() {
        val conversationId = "9702"
        val ringtoneString = "content://com.android.messaging.test/ringtone"

        val channel = NotificationChannelUtil.createConversationChannelFromLegacy(
            conversationId = conversationId,
            conversationTitle = "Custom conversation",
            legacyNotificationsEnabled = true,
            legacyRingtoneString = ringtoneString,
            legacyVibrationEnabled = false,
        )

        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals(Uri.parse(ringtoneString), channel.sound)
        assertFalse(channel.shouldVibrate())
    }

    @Test
    fun createConversationChannelFromLegacyDoesNotOverwriteExistingChannel() {
        val conversationId = "9703"
        val existingChannel = NotificationChannelUtil.createConversationChannelFromLegacy(
            conversationId = conversationId,
            conversationTitle = "Existing conversation",
            legacyNotificationsEnabled = false,
            legacyRingtoneString = "",
            legacyVibrationEnabled = false,
        )

        val returnedChannel = NotificationChannelUtil.createConversationChannelFromLegacy(
            conversationId = conversationId,
            conversationTitle = "Updated conversation",
            legacyNotificationsEnabled = true,
            legacyRingtoneString = "content://com.android.messaging.test/updated",
            legacyVibrationEnabled = true,
        )

        assertEquals(existingChannel.importance, returnedChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_NONE, returnedChannel.importance)
        assertNull(returnedChannel.sound)
        assertFalse(returnedChannel.shouldVibrate())
    }

    @Test
    fun createConversationChannelForRuntimeCreatesEnabledChannelWithoutLegacyState() {
        val conversationId = "9704"

        val channel = NotificationChannelUtil.createConversationChannelForRuntime(
            conversationId = conversationId,
            conversationTitle = "Runtime conversation",
        )

        assertNotEquals(NotificationManager.IMPORTANCE_NONE, channel.importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals(conversationId, channel.conversationId)
    }

    @Test
    fun createConversationChannelForRuntimeDoesNotOverwriteExistingDisabledChannel() {
        val conversationId = "9705"
        val existingChannel = NotificationChannelUtil.createConversationChannelFromLegacy(
            conversationId = conversationId,
            conversationTitle = "Existing muted conversation",
            legacyNotificationsEnabled = false,
            legacyVibrationEnabled = false,
        )

        val returnedChannel = NotificationChannelUtil.createConversationChannelForRuntime(
            conversationId = conversationId,
            conversationTitle = "Runtime conversation",
        )

        assertEquals(existingChannel.importance, returnedChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_NONE, returnedChannel.importance)
    }
}
