package com.android.messaging.ui.conversationsettings.screen

import android.app.Activity
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.provider.Settings
import android.view.View
import com.android.messaging.FactoryTestAccess
import com.android.messaging.testutil.createIncomingMessagesTestChannel
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsScreenEffect as Effect
import com.android.messaging.util.NotificationChannelUtil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConversationSettingsEffectHandlerTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        ShadowNotificationManager.reset()
        val context = RuntimeEnvironment.getApplication().applicationContext
        installTestFactory(context = context)
        createIncomingMessagesTestChannel()
        activity = Robolectric.buildActivity(Activity::class.java)
            .setup()
            .get()
    }

    @After
    fun tearDown() {
        ShadowNotificationManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun openNotificationChannelSettingsStartsConversationChannelSettingsIntent() {
        val conversationId = "9901"
        val handler = ConversationSettingsEffectHandlerImpl(
            activity = activity,
            hostView = View(activity),
            clipboardManager = activity.getSystemService(
                Context.CLIPBOARD_SERVICE,
            ) as ClipboardManager,
        )

        handler.handle(
            Effect.OpenNotificationChannelSettings(
                conversationId = conversationId,
                conversationTitle = "Settings conversation",
            ),
        )

        val intent = shadowOf(activity).nextStartedActivity
        val channel = requireNotNull(
            NotificationChannelUtil.getNotificationManager()
                .getNotificationChannel(NotificationChannelUtil.INCOMING_MESSAGES, conversationId),
        )
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(activity.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertEquals(
            NotificationChannelUtil.INCOMING_MESSAGES,
            intent.getStringExtra(Settings.EXTRA_CHANNEL_ID),
        )
        assertEquals(conversationId, intent.getStringExtra(Settings.EXTRA_CONVERSATION_ID))
        assertNotEquals(NotificationManager.IMPORTANCE_NONE, channel.importance)
    }
}
