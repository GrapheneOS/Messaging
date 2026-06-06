package com.android.messaging.datamodel

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression test for issue #102
 *
 * [BugleNotifications.processAndSend] calls `addPerson` once per message line, so a long-lived
 * conversation attaches the same sender hundreds/thousands of times to the notification's people
 * list. That list is unbounded and grows the notification's Binder parcel past ~1 MB, after which
 * `NotificationManager.notify` throws [android.os.TransactionTooLargeException] on every update and
 * crashes the app.
 *
 * Fails until [BugleNotifications.limitPeopleLineInfos] de-duplicates / bounds the attached people
 * before constructing [androidx.core.app.Person] objects.
 */
@RunWith(RobolectricTestRunner::class)
class BugleNotificationsMessageLimitTest {

    @Test
    fun limitPeopleLineInfos_deduplicatesRepeatedSender() {
        val sender = messageLineInfo(authorId = "sender-1")
        val incoming = BugleNotifications.MAX_NOTIFICATION_PEOPLE * 20 + 7
        val lineInfos = List(size = incoming) { sender }

        val attached = BugleNotifications.limitPeopleLineInfos(lineInfos)

        assertEquals(1, attached.size)
        assertEquals("sender-1", attached.single().mAuthorId)
    }

    @Test
    fun limitPeopleLineInfos_keepsNewestDistinctSenders() {
        val incoming = BugleNotifications.MAX_NOTIFICATION_PEOPLE + 7
        val lineInfos = List(size = incoming) { index ->
            messageLineInfo(
                authorId = "sender-$index",
                messageId = "message-$index",
            )
        }

        val attached = BugleNotifications.limitPeopleLineInfos(lineInfos)

        val expectedAuthorIds = (0 until BugleNotifications.MAX_NOTIFICATION_PEOPLE)
            .map { index -> "sender-$index" }
        assertEquals(expectedAuthorIds, attached.map { lineInfo -> lineInfo.mAuthorId })
    }

    @Test
    fun limitPeopleLineInfos_keepsNewestLineForDuplicateSender() {
        val lineInfos = listOf(
            messageLineInfo(
                authorId = "sender-1",
                messageId = "newer-sender-1",
            ),
            messageLineInfo(
                authorId = "sender-2",
                messageId = "sender-2",
            ),
            messageLineInfo(
                authorId = "sender-1",
                messageId = "older-sender-1",
            ),
        )

        val attached = BugleNotifications.limitPeopleLineInfos(lineInfos)

        assertEquals(
            listOf("newer-sender-1", "sender-2"),
            attached.map { lineInfo -> lineInfo.mMessageId },
        )
    }

    private fun messageLineInfo(
        authorId: String,
        messageId: String = authorId,
    ): MessageNotificationState.MessageLineInfo {
        return MessageNotificationState.MessageLineInfo(
            authorId,
            "Sender $authorId",
            "Sender $authorId",
            "Message $messageId",
            null,
            null,
            false,
            null,
            messageId,
            0L,
            null,
        )
    }
}
