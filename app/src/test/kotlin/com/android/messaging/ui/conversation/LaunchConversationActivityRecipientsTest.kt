package com.android.messaging.ui.conversation

import android.content.Intent
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the EXTRA_EMAIL parsing and recipient filtering in LaunchConversationActivity. The
 * extras arrive from other apps through an exported activity, so null and empty entries have to
 * be tolerated rather than assumed away.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LaunchConversationActivityRecipientsTest {

    private fun intentWithEmailArray(vararg emails: String?): Intent =
        Intent(Intent.ACTION_SENDTO).putExtra(Intent.EXTRA_EMAIL, arrayOf(*emails))

    private fun intentWithEmailString(email: String?): Intent =
        Intent(Intent.ACTION_SENDTO).putExtra(Intent.EXTRA_EMAIL, email)

    @Test
    fun emailArrayIsRead() {
        assertArrayEquals(
            arrayOf("a@example.com", "b@example.com"),
            LaunchConversationActivity.getEmailRecipients(
                intentWithEmailArray("a@example.com", "b@example.com"),
            ),
        )
    }

    @Test
    fun singleEmailStringIsStillRead() {
        assertArrayEquals(
            arrayOf("a@example.com"),
            LaunchConversationActivity.getEmailRecipients(
                intentWithEmailString("a@example.com"),
            ),
        )
    }

    @Test
    fun nullAndEmptyArrayEntriesAreDropped() {
        assertArrayEquals(
            arrayOf("a@example.com"),
            LaunchConversationActivity.getEmailRecipients(
                intentWithEmailArray(null, "", "a@example.com"),
            ),
        )
    }

    @Test
    fun arrayOfOnlyNullsCountsAsNoRecipient() {
        assertNull(
            LaunchConversationActivity.getEmailRecipients(intentWithEmailArray(null, null)),
        )
    }

    @Test
    fun emptyArrayCountsAsNoRecipient() {
        assertNull(LaunchConversationActivity.getEmailRecipients(intentWithEmailArray()))
    }

    @Test
    fun missingExtraCountsAsNoRecipient() {
        assertNull(LaunchConversationActivity.getEmailRecipients(Intent(Intent.ACTION_SENDTO)))
    }

    @Test
    fun emptyEmailStringCountsAsNoRecipient() {
        assertNull(LaunchConversationActivity.getEmailRecipients(intentWithEmailString("")))
    }

    @Test
    fun trimInvalidRecipientsToleratesNullEntries() {
        // Regression: a null entry used to NPE on recipient.length().
        assertArrayEquals(
            arrayOf("a@example.com"),
            LaunchConversationActivity.trimInvalidRecipients(
                arrayOf(null, "a@example.com"),
            ),
        )
    }

    @Test
    fun trimInvalidRecipientsDropsEmptyEntries() {
        assertArrayEquals(
            arrayOf("a@example.com"),
            LaunchConversationActivity.trimInvalidRecipients(
                arrayOf("", "a@example.com"),
            ),
        )
    }

    @Test
    fun trimInvalidRecipientsDropsOverlongEntries() {
        val tooLong = "x".repeat(1000)
        assertArrayEquals(
            arrayOf("a@example.com"),
            LaunchConversationActivity.trimInvalidRecipients(
                arrayOf(tooLong, "a@example.com"),
            ),
        )
    }

    @Test
    fun trimInvalidRecipientsReturnsNullWhenNothingValid() {
        assertNull(LaunchConversationActivity.trimInvalidRecipients(arrayOf(null, "")))
    }
}
