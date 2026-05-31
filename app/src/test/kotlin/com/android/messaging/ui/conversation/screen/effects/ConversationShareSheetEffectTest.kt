package com.android.messaging.ui.conversation.screen.effects

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.messaging.R
import com.android.messaging.ui.conversation.screen.openShareSheet
import com.android.messaging.util.UriUtil
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationShareSheetEffectTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkStatic(UriUtil::class)
        every { context.getText(R.string.action_share) } returns "Share"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun openShareSheet_sharesAttachmentWithReadGrant() {
        runTest {
            val fileUri = Uri.parse("file:///tmp/image.png")
            val scratchUri = Uri.parse("content://scratch/image.png")
            val chooserIntentSlot = slot<Intent>()
            every { UriUtil.persistContentToScratchSpace(fileUri) } returns scratchUri
            every { context.startActivity(capture(chooserIntentSlot)) } just runs

            openShareSheet(
                context = context,
                attachmentContentType = "image/png",
                attachmentContentUri = fileUri.toString(),
                text = "ignored",
            )

            val sendIntent = chooserIntentSlot.captured
                .getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)

            assertEquals(Intent.ACTION_CHOOSER, chooserIntentSlot.captured.action)
            assertEquals(
                "Share",
                chooserIntentSlot.captured.getCharSequenceExtra(Intent.EXTRA_TITLE),
            )
            assertEquals(Intent.ACTION_SEND, sendIntent?.action)
            assertEquals("image/png", sendIntent?.type)
            assertEquals(
                scratchUri,
                sendIntent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java),
            )
            assertNull(sendIntent?.getStringExtra(Intent.EXTRA_TEXT))
            assertTrue(
                requireNotNull(sendIntent).flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
            )
        }
    }

    @Test
    fun openShareSheet_fallsBackToTextWhenAttachmentFieldsAreBlank() {
        runTest {
            val chooserIntentSlot = slot<Intent>()
            every { context.startActivity(capture(chooserIntentSlot)) } just runs

            openShareSheet(
                context = context,
                attachmentContentType = " ",
                attachmentContentUri = "content://media/image/1",
                text = null,
            )

            val sendIntent = chooserIntentSlot.captured
                .getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)

            assertEquals(Intent.ACTION_SEND, sendIntent?.action)
            assertEquals("text/plain", sendIntent?.type)
            assertEquals("", sendIntent?.getStringExtra(Intent.EXTRA_TEXT))
            assertNull(sendIntent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            verify(exactly = 0) {
                UriUtil.persistContentToScratchSpace(any<Uri>())
            }
        }
    }

    @Test
    fun openShareSheet_keepsFileUriWhenScratchPersistenceFails() {
        runTest {
            val fileUri = Uri.parse("file:///tmp/video.mp4")
            val chooserIntentSlot = slot<Intent>()
            every { UriUtil.persistContentToScratchSpace(fileUri) } returns null
            every { context.startActivity(capture(chooserIntentSlot)) } just runs

            openShareSheet(
                context = context,
                attachmentContentType = "video/mp4",
                attachmentContentUri = fileUri.toString(),
                text = null,
            )

            val sendIntent = chooserIntentSlot.captured
                .getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)

            assertEquals(
                fileUri,
                sendIntent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java),
            )
            assertEquals("video/mp4", sendIntent?.type)
        }
    }
}
