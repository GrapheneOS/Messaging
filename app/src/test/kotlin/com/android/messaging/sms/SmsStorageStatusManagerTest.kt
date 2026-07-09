package com.android.messaging.sms

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.provider.Settings
import com.android.messaging.Factory
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.UIIntentsImpl
import com.android.messaging.util.PendingIntentConstants
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow.directlyOn
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.util.ReflectionHelpers.ClassParameter

private const val STORAGE_WARNING_TITLE = "Storage space running out"
private const val STORAGE_WARNING_TEXT =
    "Messaging might not send or receive messages until more space is available on your device."
private const val STORAGE_WARNING_TICKER = "Low SMS storage. You may need to delete messages."

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [StorageWarningResourcesShadow::class])
internal class SmsStorageStatusManagerTest {

    private lateinit var context: Context
    private lateinit var phoneUtils: PhoneUtils

    @Before
    fun setUp() {
        ShadowNotificationManager.reset()
        context = RuntimeEnvironment.getApplication().applicationContext
        phoneUtils = mockk()
        every { phoneUtils.isSmsEnabled() } returns true
        installTestFactory(
            context = context,
            phoneUtils = phoneUtils,
        )
        every { requireNotNull(Factory.get()).getUIIntents() } returns UIIntentsImpl()
        shadowOf(context.packageManager).setResolveInfosForIntent(
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
            listOf(
                ResolveInfo().apply {
                    activityInfo = ActivityInfo().apply {
                        applicationInfo = ApplicationInfo().apply {
                            packageName = "com.android.settings"
                        }
                        packageName = "com.android.settings"
                        name = "StorageSettingsActivity"
                    }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        ShadowNotificationManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun handleStorageLow_postsDismissibleStorageSettingsNotification() {
        SmsStorageStatusManager.handleStorageLow()

        assertStorageWarningNotification()
    }

    @Test
    fun handleStorageFull_postsDismissibleStorageSettingsNotification() {
        SmsStorageStatusManager.handleStorageFull()

        assertStorageWarningNotification()
    }

    @Test
    fun handleStorageOk_cancelsStorageWarningNotification() {
        SmsStorageStatusManager.handleStorageLow()

        SmsStorageStatusManager.handleStorageOk()

        assertNull(storageNotification())
    }

    @Test
    fun storageWarnings_doNotPostWhenMessagingIsNotTheDefaultSmsApp() {
        every { phoneUtils.isSmsEnabled() } returns false

        SmsStorageStatusManager.handleStorageLow()
        SmsStorageStatusManager.handleStorageFull()

        assertEquals(0, shadowOf(notificationManager()).size())
    }

    @Test
    fun lowStorageNotification_usesGenericSettingsWhenStorageSettingsCannotBeResolved() {
        shadowOf(context.packageManager).setResolveInfosForIntent(
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
            emptyList(),
        )

        val pendingIntent = UIIntentsImpl().getPendingIntentForLowStorageNotifications(context)

        assertEquals(Settings.ACTION_SETTINGS, shadowOf(pendingIntent).savedIntent.action)
    }

    private fun assertStorageWarningNotification() {
        val notification = requireNotNull(storageNotification())

        assertEquals(
            STORAGE_WARNING_TITLE,
            notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
        )
        assertEquals(
            STORAGE_WARNING_TICKER,
            notification.tickerText.toString(),
        )
        assertEquals(
            STORAGE_WARNING_TEXT,
            notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
        )
        assertEquals(
            STORAGE_WARNING_TEXT,
            notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString(),
        )
        assertEquals(R.drawable.ic_sms_light, notification.smallIcon.resId)
        assertFalse(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertFalse(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)

        val contentIntent = requireNotNull(notification.contentIntent)
        val shadowPendingIntent = shadowOf(contentIntent)
        assertTrue(shadowPendingIntent.isActivity)
        assertTrue(shadowPendingIntent.isImmutable)
        assertEquals(
            Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            shadowPendingIntent.savedIntent.action,
        )
    }

    private fun storageNotification(): Notification? {
        return shadowOf(notificationManager()).getNotification(
            "${context.packageName}:smsstoragelow",
            PendingIntentConstants.SMS_STORAGE_LOW_NOTIFICATION_ID,
        )
    }

    private fun notificationManager(): NotificationManager {
        return requireNotNull(context.getSystemService(NotificationManager::class.java))
    }
}

@Implements(Resources::class)
internal class StorageWarningResourcesShadow {

    @RealObject
    private lateinit var realResources: Resources

    @Implementation
    fun getString(resourceId: Int): String {
        return when (resourceId) {
            R.string.sms_storage_low_title -> STORAGE_WARNING_TITLE
            R.string.sms_storage_low_text -> STORAGE_WARNING_TEXT
            R.string.sms_storage_low_notification_ticker -> STORAGE_WARNING_TICKER
            else -> directlyOn(
                realResources,
                Resources::class.java,
                "getString",
                ClassParameter.from(Int::class.javaPrimitiveType!!, resourceId),
            )
        }
    }
}
