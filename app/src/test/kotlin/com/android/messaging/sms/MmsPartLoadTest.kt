package com.android.messaging.sms

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Parcel
import android.os.ParcelFileDescriptor
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.action.SyncMessagesActionMmsReplacementRaceTestApplication
import com.android.messaging.mmslib.pdu.CharacterSets
import com.android.messaging.sms.DatabaseMessages.MmsPart
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.BugleGservicesKeys
import com.android.messaging.util.ContentType
import java.io.File
import java.io.FileNotFoundException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(application = SyncMessagesActionMmsReplacementRaceTestApplication::class)
internal class MmsPartLoadTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val partProvider = FakeMmsPartProvider()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        installTestFactory(context = context)
        partProvider.attachInfo(context, ProviderInfo().apply { authority = MMS_AUTHORITY })
        ShadowContentResolver.registerProviderInternal(MMS_AUTHORITY, partProvider)
    }

    @After
    fun tearDown() {
        ShadowContentResolver.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun documentPartKeepsOnlyItsSize() {
        partProvider.addFile(
            partId = PART_ID,
            file = createFile(text = "%PDF".padEnd(LARGE_PART_SIZE_BYTES, 'x')),
        )

        val part = loadPart(contentType = "application/pdf")

        assertNull(part.mText)
        assertEquals(LARGE_PART_SIZE_BYTES.toLong(), part.mSize)
        assertTrue(part.parcelledSize() < SMALL_PARCEL_SIZE_BYTES)
        assertNull(MmsUtils.createMmsMessagePart(part))
    }

    @Test
    fun embeddedSmilPartKeepsOnlyItsSize() {
        val smil = "<smil>".padEnd(LARGE_PART_SIZE_BYTES, ' ')

        val part = loadPart(contentType = ContentType.APP_SMIL, text = smil)

        assertNull(part.mText)
        assertEquals(LARGE_PART_SIZE_BYTES.toLong(), part.mSize)
        assertTrue(part.parcelledSize() < SMALL_PARCEL_SIZE_BYTES)
    }

    @Test
    fun embeddedTextPartKeepsOnlyTheImportedText() {
        val text = "a".repeat(LARGE_PART_SIZE_BYTES)

        val part = loadPart(contentType = ContentType.TEXT_PLAIN, text = text)

        assertEquals(text.take(BugleGservicesKeys.MMS_TEXT_LIMIT_DEFAULT), part.mText)
        assertEquals(LARGE_PART_SIZE_BYTES.toLong(), part.mSize)
        assertEquals(part.mText, MmsUtils.createMmsMessagePart(part).text)
    }

    @Test
    fun fileTextPartKeepsOnlyTheImportedText() {
        val text = "ä".repeat(LARGE_PART_SIZE_BYTES / 2)
        partProvider.addFile(partId = PART_ID, file = createFile(text = text))

        val part = loadPart(contentType = ContentType.APP_WAP_XHTML)

        assertEquals(text.take(BugleGservicesKeys.MMS_TEXT_LIMIT_DEFAULT), part.mText)
        assertEquals(LARGE_PART_SIZE_BYTES.toLong(), part.mSize)
        assertTrue(part.parcelledSize() < SMALL_PARCEL_SIZE_BYTES)
    }

    @Test
    fun shortFileTextPartIsDecodedWhole() {
        val text = "Hello"
        partProvider.addFile(partId = PART_ID, file = createFile(text = text))

        val part = loadPart(contentType = ContentType.APP_WAP_XHTML)

        assertEquals(text, part.mText)
        assertEquals(text.length.toLong(), part.mSize)
    }

    @Test
    fun fileTextPartWithoutFileHasNoText() {
        val part = loadPart(contentType = ContentType.APP_WAP_XHTML)

        assertNull(part.mText)
        assertEquals(0L, part.mSize)
    }

    @Test
    fun fileTextPartWhoseProviderReturnsNoFileHasNoText() {
        partProvider.returnNoFile = true

        val part = loadPart(contentType = ContentType.APP_WAP_XHTML)

        assertNull(part.mText)
        assertEquals(0L, part.mSize)
    }

    private fun loadPart(contentType: String, text: String? = null): MmsPart {
        val cursor = MatrixCursor(MmsPart.PROJECTION).apply {
            addRow(arrayOf<Any?>(PART_ID, MESSAGE_ID, CharacterSets.UTF_8, contentType, text))
            moveToFirst()
        }
        return cursor.use { MmsPart.get(cursor, false) }
    }

    private fun createFile(text: String): File {
        return temporaryFolder.newFile().apply { writeText(text) }
    }

    private fun MmsPart.parcelledSize(): Int {
        val parcel = Parcel.obtain()
        return try {
            writeToParcel(parcel, 0)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    private class FakeMmsPartProvider : ContentProvider() {
        private val files = mutableMapOf<Long, File>()

        var returnNoFile = false

        fun addFile(partId: Long, file: File) {
            files[partId] = file
        }

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            val partId = uri.lastPathSegment?.toLongOrNull()
            val file = files[partId]
            return when {
                returnNoFile -> null
                file == null -> throw FileNotFoundException("No file for $uri")
                else -> ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }

        override fun onCreate(): Boolean {
            return true
        }

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? {
            return null
        }

        override fun getType(uri: Uri): String? {
            return null
        }

        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            return null
        }

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            return 0
        }

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int {
            return 0
        }
    }

    private companion object {
        private const val MMS_AUTHORITY = "mms"
        private const val PART_ID = 528L
        private const val MESSAGE_ID = 5280L
        private const val LARGE_PART_SIZE_BYTES = 2 * 1024 * 1024
        private const val SMALL_PARCEL_SIZE_BYTES = 16 * 1024
    }
}
