package com.android.messaging.ui.conversationpicker.host.share

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers when a share intent may be redirected to LaunchConversationActivity. That activity only
 * understands a destination plus a text body, so an intent carrying an EXTRA_STREAM attachment
 * must stay in the conversation picker or the attachment is silently dropped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShareIntentActivityRedirectTest {

    private val address = "address"
    private val stream: Uri = Uri.parse("content://media/external/images/media/1")

    private fun sendIntent(): Intent = Intent(Intent.ACTION_SEND).setType("text/plain")

    @Test
    fun redirectsWhenEmailArrayIsTheDestination() {
        val intent = sendIntent().putExtra(Intent.EXTRA_EMAIL, arrayOf("a@example.com"))
        assertTrue(ShareIntentActivity.shouldRedirectToSendTo(intent))
    }

    @Test
    fun redirectsWhenEmailStringIsTheDestination() {
        val intent = sendIntent().putExtra(Intent.EXTRA_EMAIL, "a@example.com")
        assertTrue(ShareIntentActivity.shouldRedirectToSendTo(intent))
    }

    @Test
    fun redirectsWhenAddressIsTheDestination() {
        val intent = sendIntent().putExtra(address, "+15555550100")
        assertTrue(ShareIntentActivity.shouldRedirectToSendTo(intent))
    }

    @Test
    fun doesNotRedirectWithoutADestination() {
        assertFalse(ShareIntentActivity.shouldRedirectToSendTo(sendIntent()))
    }

    @Test
    fun doesNotRedirectForOtherActions() {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            .putExtra(Intent.EXTRA_EMAIL, arrayOf("a@example.com"))
        assertFalse(ShareIntentActivity.shouldRedirectToSendTo(intent))
    }

    @Test
    fun doesNotRedirectWhenAnAttachmentWouldBeLost() {
        // Regression: EXTRA_STREAM + EXTRA_EMAIL used to redirect and drop the attachment.
        val intent = sendIntent()
            .putExtra(Intent.EXTRA_EMAIL, arrayOf("a@example.com"))
            .putExtra(Intent.EXTRA_STREAM, stream)
        assertFalse(ShareIntentActivity.shouldRedirectToSendTo(intent))
    }

    @Test
    fun doesNotRedirectWhenAttachmentAccompaniesAnAddress() {
        val intent = sendIntent()
            .putExtra(address, "+15555550100")
            .putExtra(Intent.EXTRA_STREAM, stream)
        assertFalse(ShareIntentActivity.shouldRedirectToSendTo(intent))
    }

    @Test
    fun emailArrayOfOnlyNullsIsNotADestination() {
        val intent = sendIntent().putExtra(Intent.EXTRA_EMAIL, arrayOf<String?>(null, ""))
        assertFalse(ShareIntentActivity.shouldRedirectToSendTo(intent))
    }
}
