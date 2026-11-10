package com.android.messaging.sms

import android.net.Uri
import android.os.Parcel
import com.android.messaging.util.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MmsUtilsPdfPartTest {

    @Test
    fun pdfPart_isFileButNotMediaNorText() {
        val part = mmsPart(contentType = ContentType.APPLICATION_PDF)

        assertTrue(part.isFile())
        assertFalse(part.isMedia())
        assertFalse(part.isText())
    }

    @Test
    fun createMmsMessagePart_pdf_createsAttachment() {
        val part = mmsPart(contentType = ContentType.APPLICATION_PDF)

        val messagePart = MmsUtils.createMmsMessagePart(part)

        assertNotNull(messagePart)
        assertEquals(ContentType.APPLICATION_PDF, messagePart!!.contentType)
        assertEquals(Uri.parse("content://mms/part/7"), messagePart.contentUri)
        assertTrue(messagePart.isAttachment())
        assertFalse(messagePart.isText())
    }

    @Test
    fun createMmsMessagePart_unknownFileType_isStillDropped() {
        // Only PDFs are imported today. Unknown types stay conservative so a crafted part can't
        // widen the set of files the app stores.
        val part = mmsPart(contentType = "application/zip")

        assertNull(MmsUtils.createMmsMessagePart(part))
    }

    private fun mmsPart(contentType: String): DatabaseMessages.MmsPart {
        val parcel = Parcel.obtain()
        parcel.writeString("content://mms/7")
        parcel.writeLong(7)
        parcel.writeLong(1)
        parcel.writeString(contentType)
        parcel.writeString(null)
        parcel.writeInt(0)
        parcel.writeInt(0)
        parcel.writeInt(0)
        parcel.writeLong(0)
        parcel.setDataPosition(0)
        return DatabaseMessages.MmsPart.CREATOR.createFromParcel(parcel).also {
            parcel.recycle()
        }
    }
}
