package com.android.messaging.datamodel.action

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.provider.BaseColumns
import android.provider.Telephony.Mms
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.ActionSyncTestDataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.SyncManager
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.mmslib.pdu.CharacterSets
import com.android.messaging.mmslib.pdu.PduHeaders
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.BugleGservices
import com.android.messaging.util.BugleGservicesKeys
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.FileNotFoundException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
internal class SyncMessagesActionLargeMmsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var dataModel: ActionSyncTestDataModel
    private lateinit var database: DatabaseWrapper
    private val prefs = FakeBuglePrefs()
    private val telephonyProvider = FakeTelephonyProvider()

    private val syncManager: SyncManager
        get() = dataModel.syncManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        dataModel = ActionSyncTestDataModel()
        installTestFactory(
            context = context,
            dataModel = dataModel,
            prefs = prefs,
            gServices = createSingleMessageBatchGservices(),
        )

        database = createInMemoryActionSyncTestDatabase(context)
        dataModel.setDatabase(database)
        seedSelfParticipant()

        val providerInfo = ProviderInfo().apply {
            authority = TELEPHONY_AUTHORITIES.joinToString(separator = ";")
        }
        telephonyProvider.attachInfo(context, providerInfo)
        TELEPHONY_AUTHORITIES.forEach { authority ->
            ShadowContentResolver.registerProviderInternal(authority, telephonyProvider)
        }
    }

    @After
    fun tearDown() {
        ShadowContentResolver.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun mmsWithLargeDocumentIsSyncedAndTheSyncContinues() {
        telephonyProvider.addIncomingMms(id = MMS_ID, timestampMillis = MMS_TIMESTAMP_MILLIS)
        telephonyProvider.addPart(
            id = TEXT_PART_ID,
            messageId = MMS_ID,
            contentType = ContentType.TEXT_PLAIN,
            text = MESSAGE_TEXT,
        )
        telephonyProvider.addPart(
            id = DOCUMENT_PART_ID,
            messageId = MMS_ID,
            contentType = DOCUMENT_CONTENT_TYPE,
            file = temporaryFolder.newFile().apply { writeBytes(ByteArray(DOCUMENT_SIZE_BYTES)) },
        )
        val action = createFullSyncAction()

        runOnWorker {
            action.executeAction()
            val response = requireNotNull(action.doBackgroundWork())

            assertEquals(setOf(LAST_TIMESTAMP_KEY), response.keySet())
            assertTrue(parcelledSize(action = action, response = response) < SMALL_RESPONSE_BYTES)

            action.processBackgroundResponse(response)
        }

        assertEquals(listOf(ContentType.TEXT_PLAIN to MESSAGE_TEXT), readLocalParts())
        assertTrue(action.hasBackgroundActions())
        assertTrue(syncManager.isSyncing(MMS_TIMESTAMP_MILLIS + 1))
    }

    @Test
    fun syncEndsWhenTheBatchChangesAreMissing() {
        val action = createFullSyncAction()

        runOnWorker {
            action.executeAction()
            action.processBackgroundResponse(
                Bundle().apply { putLong(LAST_TIMESTAMP_KEY, MMS_TIMESTAMP_MILLIS) },
            )
        }

        assertSyncEnded()
    }

    @Test
    fun syncEndsWhenTheBatchFails() {
        val action = createFullSyncAction()

        runOnWorker {
            action.executeAction()
            action.processBackgroundWorkFailure()
        }

        assertSyncEnded()
        assertTrue(syncManager.shouldSync(false, START_TIMESTAMP_MILLIS + 1))
    }

    @Test
    fun failureOfAnOrphanBatchLeavesTheSyncRunning() {
        val action = createFullSyncAction()
        val orphanAction = SyncMessagesAction(
            -1L,
            UPPER_BOUND_MILLIS - 1,
            1,
            START_TIMESTAMP_MILLIS - 1,
        )

        runOnWorker {
            action.executeAction()
            orphanAction.processBackgroundWorkFailure()
        }

        assertTrue(syncManager.isSyncing())
        assertTrue(syncManager.isSyncing(UPPER_BOUND_MILLIS))
        assertEquals(
            BuglePrefsKeys.LAST_SYNC_TIME_DEFAULT,
            prefs.getLong(BuglePrefsKeys.LAST_SYNC_TIME, BuglePrefsKeys.LAST_SYNC_TIME_DEFAULT),
        )
    }

    private fun createFullSyncAction(): SyncMessagesAction {
        return SyncMessagesAction(-1L, UPPER_BOUND_MILLIS, 1, START_TIMESTAMP_MILLIS)
    }

    private fun assertSyncEnded() {
        assertFalse(syncManager.isSyncing())
        assertFalse(syncManager.isSyncing(UPPER_BOUND_MILLIS))
        assertEquals(START_TIMESTAMP_MILLIS, prefs.getLong(BuglePrefsKeys.LAST_SYNC_TIME, -1L))
        assertEquals(
            START_TIMESTAMP_MILLIS,
            prefs.getLong(BuglePrefsKeys.LAST_FULL_SYNC_TIME, -1L),
        )
    }

    private fun runOnWorker(block: () -> Unit) {
        var failure: Throwable? = null
        val thread = Thread(
            {
                try {
                    block()
                } catch (throwable: Throwable) {
                    failure = throwable
                }
            },
            "sync-test-worker",
        )
        thread.start()
        thread.join()
        failure?.let { throwable -> throw throwable }
    }

    private fun parcelledSize(action: Action, response: Bundle): Int {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeParcelable(action, 0)
            parcel.writeBundle(response)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    private fun readLocalParts(): List<Pair<String, String?>> {
        val messageId = database.query(
            DatabaseHelper.MESSAGES_TABLE,
            arrayOf(BaseColumns._ID),
            "${MessageColumns.SMS_MESSAGE_URI}=?",
            arrayOf("content://mms/$MMS_ID"),
            null,
            null,
            null,
            null,
        ).use { cursor ->
            assertTrue("MMS was not synced", cursor.moveToFirst())
            cursor.getString(0)
        }
        return database.query(
            DatabaseHelper.PARTS_TABLE,
            arrayOf(PartColumns.CONTENT_TYPE, PartColumns.TEXT),
            "${PartColumns.MESSAGE_ID}=?",
            arrayOf(messageId),
            null,
            null,
            null,
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0) to cursor.getString(1))
                }
            }
        }
    }

    private fun seedSelfParticipant() {
        val values = ParticipantData
            .getSelfParticipant(ParticipantData.DEFAULT_SELF_SUB_ID)
            .toContentValues()
        values.put(BaseColumns._ID, SELF_PARTICIPANT_DB_ID)
        database.insert(DatabaseHelper.PARTICIPANTS_TABLE, null, values)
    }

    private fun createSingleMessageBatchGservices(): BugleGservices {
        val gServices = mockk<BugleGservices>(relaxed = true)
        every { gServices.getLong(any(), any()) } answers { secondArg() }
        every { gServices.getInt(any(), any()) } answers { secondArg() }
        every { gServices.getBoolean(any(), any()) } answers { secondArg() }
        every { gServices.getString(any(), any()) } answers { secondArg() }
        every { gServices.getFloat(any(), any()) } answers { secondArg() }
        every { gServices.getInt(BugleGservicesKeys.SMS_SYNC_BATCH_SIZE_MIN, any()) } returns 1
        every { gServices.getInt(BugleGservicesKeys.SMS_SYNC_BATCH_SIZE_MAX, any()) } returns 1
        return gServices
    }

    private class FakeTelephonyProvider : ContentProvider() {
        private val mmsRows = mutableListOf<Map<String, Any?>>()
        private val partRows = mutableListOf<Map<String, Any?>>()
        private val partFiles = mutableMapOf<Long, File>()

        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI("sms", null, SMS)
            addURI("mms", null, MMS)
            addURI("mms", "part", PARTS)
            addURI("mms", "part/#", PART)
            addURI("mms-sms", "conversations", THREADS)
            addURI("mms-sms", "canonical-address/#", CANONICAL_ADDRESS)
        }

        fun addIncomingMms(id: Long, timestampMillis: Long) {
            mmsRows += mapOf(
                Mms._ID to id,
                Mms.MESSAGE_BOX to Mms.MESSAGE_BOX_INBOX,
                Mms.SUBJECT to null,
                Mms.SUBJECT_CHARSET to CharacterSets.UTF_8,
                Mms.MESSAGE_SIZE to 0,
                Mms.DATE to timestampMillis / 1_000L,
                Mms.DATE_SENT to 0L,
                Mms.THREAD_ID to id,
                Mms.PRIORITY to PduHeaders.PRIORITY_NORMAL,
                Mms.STATUS to 0,
                Mms.READ to 0,
                Mms.SEEN to 0,
                Mms.CONTENT_LOCATION to null,
                Mms.TRANSACTION_ID to "issue528-tx-$id",
                Mms.MESSAGE_TYPE to PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF,
                Mms.EXPIRY to 0L,
                Mms.RESPONSE_STATUS to 0,
                Mms.RETRIEVE_STATUS to 0,
                Mms.SUBSCRIPTION_ID to ParticipantData.DEFAULT_SELF_SUB_ID,
            )
        }

        fun addPart(
            id: Long,
            messageId: Long,
            contentType: String,
            text: String? = null,
            file: File? = null,
        ) {
            partRows += mapOf(
                Mms.Part._ID to id,
                Mms.Part.MSG_ID to messageId,
                Mms.Part.CHARSET to CharacterSets.UTF_8,
                Mms.Part.CONTENT_TYPE to contentType,
                Mms.Part.TEXT to text,
            )
            file?.let { partFile -> partFiles[id] = partFile }
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
        ): Cursor {
            val columns = requireNotNull(projection) { "Unexpected query without projection" }
            val rows = when (matcher.match(uri)) {
                MMS -> mmsRows
                PARTS -> partRows.filter { row ->
                    row[Mms.Part.MSG_ID].toString() in selectionArgs.orEmpty()
                }
                THREADS -> threadRows(selectionArgs = selectionArgs)
                CANONICAL_ADDRESS -> listOf(mapOf(ADDRESS_COLUMN to "+1555${uri.lastPathSegment}"))
                else -> emptyList()
            }
            return when {
                columns.isCountProjection() -> MatrixCursor(columns).apply {
                    addRow(arrayOf(rows.size))
                }
                else -> rowsCursor(columns = columns, rows = rows)
            }
        }

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            val file = partFiles[uri.lastPathSegment?.toLongOrNull()]
                ?: throw FileNotFoundException("No file for $uri")
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
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

        private fun threadRows(selectionArgs: Array<out String>?): List<Map<String, Any?>> {
            val threadId = selectionArgs?.firstOrNull()?.toLongOrNull()
            return when (threadId) {
                null -> emptyList()
                else -> listOf(mapOf(BaseColumns._ID to threadId, RECIPIENT_IDS_COLUMN to threadId))
            }
        }

        private fun rowsCursor(
            columns: Array<out String>,
            rows: List<Map<String, Any?>>,
        ): MatrixCursor {
            return MatrixCursor(columns).apply {
                rows.forEach { row ->
                    addRow(columns.map { column -> row[column] }.toTypedArray())
                }
            }
        }

        private fun Array<out String>.isCountProjection(): Boolean {
            return size == 1 && first() == "count()"
        }

        private companion object {
            private const val SMS = 1
            private const val MMS = 2
            private const val PARTS = 3
            private const val PART = 4
            private const val THREADS = 5
            private const val CANONICAL_ADDRESS = 6
            private const val RECIPIENT_IDS_COLUMN = "recipient_ids"
            private const val ADDRESS_COLUMN = "address"
        }
    }

    private companion object {
        private val TELEPHONY_AUTHORITIES = listOf("sms", "mms", "mms-sms")
        private const val LAST_TIMESTAMP_KEY = "last_timestamp"
        private const val SELF_PARTICIPANT_DB_ID = 1L
        private const val MMS_ID = 528L
        private const val TEXT_PART_ID = 5_281L
        private const val DOCUMENT_PART_ID = 5_282L
        private const val MESSAGE_TEXT = "Hello"
        private const val DOCUMENT_CONTENT_TYPE = "application/pdf"
        private const val DOCUMENT_SIZE_BYTES = 2 * 1024 * 1024
        private const val SMALL_RESPONSE_BYTES = 4 * 1024
        private const val MMS_TIMESTAMP_MILLIS = 1_780_920_000_000L
        private const val UPPER_BOUND_MILLIS = MMS_TIMESTAMP_MILLIS + 1_000L
        private const val START_TIMESTAMP_MILLIS = MMS_TIMESTAMP_MILLIS + 10_000L
    }
}
