package com.android.messaging.ui.conversationpicker.host.share

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ShareIntentActivityTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun createForwardIntent_preservesAttachmentAndGrantsReadPermission() {
        val intent = ShareIntentActivity.createForwardIntent(
            context = context,
            uri = FORWARD_URI,
            contentType = IMAGE_JPEG,
        )

        assertEquals(ShareIntentActivity::class.java.name, intent.component?.className)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals(IMAGE_JPEG, intent.type)
        assertEquals(
            FORWARD_URI,
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java),
        )
        assertTrue(
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
        )
    }

    private companion object {
        private const val IMAGE_JPEG = "image/jpeg"
        private val FORWARD_URI = Uri.parse("content://example.test/photo.jpg")
    }
}
