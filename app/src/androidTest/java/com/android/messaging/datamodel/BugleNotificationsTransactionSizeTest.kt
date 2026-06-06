package com.android.messaging.datamodel

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.TransactionTooLargeException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.util.NotificationChannelUtil
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented regression test for issue #102
 *
 * Builds a conversation notification the way [BugleNotifications.processAndSend] does: attaching
 * one bounded [Person] list for all message lines and actually posting it through the real
 * framework. Before the fix, a long-lived conversation's unbounded people list pushes the
 * notification's parcel past the ~1 MB Binder limit and
 * `NotificationManager.notify` throws [TransactionTooLargeException]
 *
 * Fails unless the attached people list is bounded.
 */
@RunWith(AndroidJUnit4::class)
class BugleNotificationsTransactionSizeTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelUtil.INCOMING_MESSAGES,
                "Conversations",
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )

        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    @Test
    fun notify_withManyAccumulatedMessages_doesNotExceedBinderLimit() {
        val self = Person.Builder().setName("Me").build()

        val newestSender = sender(index = 0)

        val style = MessagingStyle(self)
        style.addMessage(MessagingStyle.Message("latest", 1L, newestSender))

        val peopleInConversation = List(size = PERSON_COUNT, ::sender)
        assertEquals(
            PERSON_COUNT,
            peopleInConversation.map { person -> person.key }.toSet().size,
        )

        val builder = NotificationCompat.Builder(
            context,
            NotificationChannelUtil.INCOMING_MESSAGES,
        )
            .setSmallIcon(R.drawable.ic_sms_light)
            .setStyle(style)

        peopleInConversation
            .take(BugleNotifications.MAX_NOTIFICATION_PEOPLE)
            .forEach(builder::addPerson)

        val notification = builder.build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: RuntimeException) {
            if (e.cause is TransactionTooLargeException) {
                throw AssertionError(
                    "issue #102: posting a conversation notification with " +
                        "${BugleNotifications.MAX_NOTIFICATION_PEOPLE} attached people from " +
                        "${peopleInConversation.size} distinct senders threw " +
                        "TransactionTooLargeException",
                    e,
                )
            }
            throw e
        } finally {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    private fun sender(index: Int): Person {
        return Person.Builder()
            .setName("Sender $index ${"0123456789".repeat(25)}")
            .setKey("sender-$index")
            .build()
    }

    private companion object {
        const val NOTIFICATION_ID = 0x7102
        const val PERSON_COUNT = 6000
    }
}
