package com.android.messaging.datamodel.action

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import androidx.collection.LongSparseArray
import androidx.core.content.contentValuesOf
import com.android.messaging.BugleApplication
import com.android.messaging.Factory
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.ActionSyncTestDataModel
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.action.mms.IsMmsNotificationDownloadCountSynchronized
import com.android.messaging.datamodel.action.mms.mmsNotificationDownloadStatuses
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.mmslib.pdu.PduHeaders
import com.android.messaging.sms.DatabaseMessages
import com.android.messaging.util.BugleGservices
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

/**
 * Regression test for #144.
 *
 * MMS download replacement temporarily removes the telephony M-Notification.ind row and replaces it
 * with the final downloaded MMS. Sync must not mistake that transient missing remote row for a real
 * user deletion while the local message is still in a download/notification state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = SyncMessagesActionMmsReplacementRaceTestApplication::class)
class SyncMessagesActionMmsReplacementRaceTest {

    private lateinit var context: Context
    private lateinit var dataModel: ActionSyncTestDataModel
    private lateinit var database: DatabaseWrapper
    private lateinit var telephonyProvider: FakeTelephonyProvider

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        dataModel = ActionSyncTestDataModel()

        FactoryTestAccess.install(testFactory(context = context, dataModel = dataModel))

        database = createInMemoryActionSyncTestDatabase(context)
        dataModel.setDatabase(database)

        seedSelfParticipant()
        deleteSyntheticRows()

        telephonyProvider = FakeTelephonyProvider()
        ShadowContentResolver.registerProviderInternal("sms", telephonyProvider)
        ShadowContentResolver.registerProviderInternal("mms", telephonyProvider)
        ShadowContentResolver.registerProviderInternal("mms-sms", telephonyProvider)
    }

    @After
    fun tearDown() {
        ShadowContentResolver.reset()
        if (::database.isInitialized) {
            deleteSyntheticRows()
        }

        FactoryTestAccess.reset()
    }

    @Test
    fun syncDoesNotDeleteIncomingMmsDownloadReplacementStatesWhenRemoteRowIsTemporarilyMissing() {
        mmsNotificationDownloadStatuses.forEachIndexed { index, status ->
            seedIncomingMmsPushNotification(
                id = TEST_ID_BASE + index,
                status = status,
                timestampMillis = TEST_TIMESTAMP_MILLIS - index,
            )
        }

        runSyncBatch()

        mmsNotificationDownloadStatuses.forEachIndexed { index, status ->
            assertMessageCount(
                id = TEST_ID_BASE + index,
                expectedCount = 1,
                failureMessage = "MMS download/replacement status $status was deleted",
            )
        }
    }

    @Test
    fun syncReplacesIncomingMmsNotificationWithDownloadedMmsMatchingTransactionId() {
        val notificationId = TEST_ID_BASE
        val downloadedMmsId = TEST_ID_BASE + protectedStatusCount

        seedIncomingMmsPushNotification(
            id = notificationId,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        telephonyProvider.addMmsRow(
            id = downloadedMmsId,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            threadId = notificationId,
            transactionId = transactionIdFor(id = notificationId),
        )

        runSyncBatch()

        val message = requireNotNull(readMessage(id = notificationId))
        assertEquals(MessageData.PROTOCOL_MMS, message.getProtocol())
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_COMPLETE, message.getStatus())
        assertEquals("content://mms/$downloadedMmsId", message.getSmsMessageUri().toString())
        assertMessageCount(id = downloadedMmsId, expectedCount = 0)
    }

    @Test
    fun syncReplacesIncomingMmsNotificationWhenDownloadedMmsMovesConversationWithSameSender() {
        val notificationId = TEST_ID_BASE
        val downloadedMmsId = TEST_ID_BASE + protectedStatusCount
        val downloadedThreadId = TEST_ID_BASE + 100L
        val originalConversationId = (notificationId + CONVERSATION_ID_OFFSET).toString()

        seedIncomingMmsPushNotification(
            id = notificationId,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        telephonyProvider.setThreadRecipient(
            threadId = downloadedThreadId,
            recipientId = notificationId,
        )
        telephonyProvider.addMmsRow(
            id = downloadedMmsId,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            threadId = downloadedThreadId,
            transactionId = transactionIdFor(id = notificationId),
        )

        runSyncBatch()

        val message = requireNotNull(readMessage(id = notificationId))
        assertEquals(MessageData.PROTOCOL_MMS, message.getProtocol())
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_COMPLETE, message.getStatus())
        assertEquals("content://mms/$downloadedMmsId", message.getSmsMessageUri().toString())
        assertFalse(message.getConversationId() == originalConversationId)
        assertEquals(0, countConversations(id = originalConversationId))
        assertEquals(1, countConversations(id = message.getConversationId()))
    }

    @Test
    fun syncReplacesIncomingMmsNotificationWhenDownloadedMmsSenderDiffers() {
        val notificationId = TEST_ID_BASE
        val downloadedMmsId = TEST_ID_BASE + protectedStatusCount
        val downloadedThreadId = TEST_ID_BASE + 100L

        seedIncomingMmsPushNotification(
            id = notificationId,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        telephonyProvider.addMmsRow(
            id = downloadedMmsId,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            threadId = downloadedThreadId,
            transactionId = transactionIdFor(id = notificationId),
        )

        runSyncBatch()

        val message = requireNotNull(readMessage(id = notificationId))
        assertEquals(MessageData.PROTOCOL_MMS, message.getProtocol())
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_COMPLETE, message.getStatus())
        assertEquals("content://mms/$downloadedMmsId", message.getSmsMessageUri().toString())
        assertMessageCount(id = downloadedMmsId, expectedCount = 0)
        assertEquals(1, countMessagesByUri(messageUri = "content://mms/$downloadedMmsId"))
    }

    @Test
    fun syncKeepsDownloadedMmsWhenReplacingExpiredIncomingMmsNotification() {
        val notificationId = TEST_ID_BASE
        val downloadedMmsId = TEST_ID_BASE + protectedStatusCount

        seedIncomingMmsPushNotification(
            id = notificationId,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            mmsExpiryMillis = EXPIRED_MMS_EXPIRY_MILLIS,
        )
        telephonyProvider.addMmsRow(
            id = downloadedMmsId,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            threadId = notificationId,
            transactionId = transactionIdFor(id = notificationId),
        )

        runSyncBatch()

        val message = requireNotNull(readMessage(id = notificationId))
        assertEquals(MessageData.PROTOCOL_MMS, message.getProtocol())
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_COMPLETE, message.getStatus())
        assertEquals("content://mms/$downloadedMmsId", message.getSmsMessageUri().toString())
        assertEquals(1, countMessagesByUri(messageUri = "content://mms/$downloadedMmsId"))
    }

    @Test
    fun syncDoesNotReplaceIncomingMmsNotificationWhenDownloadedMmsTransactionIdIsMissing() {
        val emptyTransactionNotificationId = TEST_ID_BASE
        val nullTransactionNotificationId = TEST_ID_BASE + 1L
        val emptyTransactionDownloadedMmsId = TEST_ID_BASE + protectedStatusCount
        val nullTransactionDownloadedMmsId = emptyTransactionDownloadedMmsId + 1L

        seedIncomingMmsPushNotification(
            id = emptyTransactionNotificationId,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        seedIncomingMmsPushNotification(
            id = nullTransactionNotificationId,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS - 1L,
        )
        telephonyProvider.addMmsRow(
            id = emptyTransactionDownloadedMmsId,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            threadId = emptyTransactionNotificationId,
            transactionId = "",
        )
        telephonyProvider.addMmsRow(
            id = nullTransactionDownloadedMmsId,
            timestampMillis = TEST_TIMESTAMP_MILLIS - 1L,
            threadId = nullTransactionNotificationId,
            transactionId = null,
        )

        runSyncBatch()

        assertIncomingMmsNotificationStillDownloading(id = emptyTransactionNotificationId)
        assertIncomingMmsNotificationStillDownloading(id = nullTransactionNotificationId)
        assertEquals(
            1,
            countMessagesByUri(messageUri = "content://mms/$emptyTransactionDownloadedMmsId"),
        )
        assertEquals(
            1,
            countMessagesByUri(messageUri = "content://mms/$nullTransactionDownloadedMmsId"),
        )
    }

    @Test
    fun syncDoesNotReplaceIncomingMmsNotificationWithDifferentSelfParticipant() {
        val notificationId = TEST_ID_BASE
        val downloadedMmsId = TEST_ID_BASE + protectedStatusCount

        seedSelfParticipant(
            id = OTHER_SELF_PARTICIPANT_DB_ID,
            subId = OTHER_SELF_PARTICIPANT_SUB_ID,
        )
        seedIncomingMmsPushNotification(
            id = notificationId,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            selfParticipantId = OTHER_SELF_PARTICIPANT_DB_ID,
        )
        telephonyProvider.addMmsRow(
            id = downloadedMmsId,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            threadId = notificationId,
            transactionId = transactionIdFor(id = notificationId),
        )

        runSyncBatch()

        assertIncomingMmsNotificationStillDownloading(id = notificationId)
        assertEquals(1, countMessagesByUri(messageUri = "content://mms/$downloadedMmsId"))
    }

    @Test
    fun syncCountTreatsTemporarilyMissingDownloadingMmsNotificationAsSynchronized() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )

        assertTrue(isSynchronized())
    }

    @Test
    fun syncCountTreatsUnknownExpiryMissingDownloadingMmsNotificationAsSynchronized() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            mmsExpiryMillis = UNKNOWN_MMS_EXPIRY_MILLIS,
        )

        assertTrue(isSynchronized())
    }

    @Test
    fun syncCountDoesNotIgnoreDownloadingMmsNotificationWithUnparseableRemoteUri() {
        seedLocalMessage(
            id = TEST_ID_BASE,
            protocol = MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            messageUri = "content://mms/not-a-row-id",
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )

        assertFalse(
            isMmsNotificationDownloadCountSynchronized(
                localCount = 1,
                remoteCount = 0,
            ),
        )
    }

    @Test
    fun syncCountFallsBackToRawCountsWhenRemoteMmsIdBatchQueryFails() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        telephonyProvider.addMmsRow(
            id = TEST_ID_BASE,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            messageType = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND,
        )
        telephonyProvider.failRemoteMmsIdBatchQuery = true

        assertTrue(
            isMmsNotificationDownloadCountSynchronized(
                localCount = 1,
                remoteCount = 1,
            ),
        )
        assertEquals(1, telephonyProvider.remoteMmsIdBatchQueryCount)
    }

    @Test
    fun syncCountDoesNotForceResyncWhenRemoteReplacementQueryFails() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        telephonyProvider.failRemoteMmsReplacementQuery = true

        assertTrue(
            isMmsNotificationDownloadCountSynchronized(
                localCount = 1,
                remoteCount = 0,
            ),
        )
        assertEquals(1, telephonyProvider.remoteMmsReplacementQueryCount)
    }

    @Test
    fun syncCountChecksExistingRemoteNotificationRowsInOneBatchQuery() {
        mmsNotificationDownloadStatuses.forEachIndexed { index, status ->
            val id = TEST_ID_BASE + index
            seedIncomingMmsPushNotification(
                id = id,
                status = status,
                timestampMillis = TEST_TIMESTAMP_MILLIS - index,
            )
            telephonyProvider.addMmsRow(
                id = id,
                timestampMillis = TEST_TIMESTAMP_MILLIS - index,
                messageType = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND,
            )
        }

        assertTrue(isSynchronized())

        assertEquals(1, telephonyProvider.remoteMmsIdBatchQueryCount)
    }

    @Test
    fun syncCountTreatsDownloadedMmsReplacingLocalNotificationAsUnsynchronized() {
        val notificationId = TEST_ID_BASE
        val downloadedMmsId = TEST_ID_BASE + protectedStatusCount

        seedIncomingMmsPushNotification(
            id = notificationId,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        telephonyProvider.addMmsRow(
            id = downloadedMmsId,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            threadId = notificationId,
            transactionId = transactionIdFor(id = notificationId),
        )

        assertFalse(isSynchronized())
    }

    @Test
    fun syncCountTreatsDownloadedMmsReplacingExpiredLocalNotificationAsUnsynchronized() {
        val notificationId = TEST_ID_BASE
        val downloadedMmsId = TEST_ID_BASE + protectedStatusCount

        seedIncomingMmsPushNotification(
            id = notificationId,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            mmsExpiryMillis = EXPIRED_MMS_EXPIRY_MILLIS,
        )
        telephonyProvider.addMmsRow(
            id = downloadedMmsId,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            threadId = notificationId,
            transactionId = transactionIdFor(id = notificationId),
        )

        assertFalse(isSynchronized())
    }

    @Test
    fun syncCountTreatsExpiredMissingDownloadingMmsNotificationAsUnsynchronized() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            mmsExpiryMillis = EXPIRED_MMS_EXPIRY_MILLIS,
        )

        assertFalse(isSynchronized())
    }

    @Test
    fun syncCountTreatsTerminalIncomingMmsNotificationStatusesAsUnsynchronized() {
        terminalMmsNotificationStatuses.forEachIndexed { index, status ->
            seedIncomingMmsPushNotification(
                id = TEST_ID_BASE + index,
                status = status,
                timestampMillis = TEST_TIMESTAMP_MILLIS - index,
            )
        }

        assertFalse(isSynchronized())
    }

    @Test
    fun syncCountTreatsCompletedLocalMmsMissingRemoteRowAsUnsynchronized() {
        seedLocalMessage(
            id = TEST_ID_BASE,
            protocol = MessageData.PROTOCOL_MMS,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            messageUri = "content://mms/$TEST_ID_BASE",
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )

        assertFalse(isSynchronized())
    }

    @Test
    fun syncCountTreatsRemoteOnlyMmsNotificationAsUnsynchronized() {
        telephonyProvider.addMmsRow(
            id = TEST_ID_BASE,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            messageType = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND,
        )

        assertFalse(isSynchronized())
    }

    @Test
    fun syncCountDoesNotHideUnrelatedMismatchWhenIgnoringMissingDownloadingNotification() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        seedLocalMessage(
            id = TEST_ID_BASE + 1L,
            protocol = MessageData.PROTOCOL_SMS,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            messageUri = "content://sms/${TEST_ID_BASE + 1L}",
            timestampMillis = TEST_TIMESTAMP_MILLIS - 1L,
        )
        telephonyProvider.addMmsRow(
            id = TEST_ID_BASE + 2L,
            timestampMillis = TEST_TIMESTAMP_MILLIS - 2L,
        )

        assertFalse(isSynchronized())
    }

    @Test
    fun syncCountDoesNotHideUnrelatedMmsMismatchWhenIgnoringMissingDownloadingNotification() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        seedLocalMessage(
            id = TEST_ID_BASE + 1L,
            protocol = MessageData.PROTOCOL_MMS,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            messageUri = "content://mms/${TEST_ID_BASE + 1L}",
            timestampMillis = TEST_TIMESTAMP_MILLIS - 1_000L,
        )
        telephonyProvider.addMmsRow(
            id = TEST_ID_BASE + 2L,
            timestampMillis = TEST_TIMESTAMP_MILLIS - 2_000L,
        )

        assertFalse(isSynchronized())
    }

    @Test
    fun syncCountTreatsMatchingRemainingMessagesWithMissingDownloadingNotificationAsSynchronized() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        seedLocalMessage(
            id = TEST_ID_BASE + 1L,
            protocol = MessageData.PROTOCOL_MMS,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            messageUri = "content://mms/${TEST_ID_BASE + 1L}",
            timestampMillis = TEST_TIMESTAMP_MILLIS - 1_000L,
        )
        telephonyProvider.addMmsRow(
            id = TEST_ID_BASE + 1L,
            timestampMillis = TEST_TIMESTAMP_MILLIS - 1_000L,
        )

        assertTrue(isSynchronized())
    }

    @Test
    fun syncKeepsMmsWhenMatchingRemoteRowStillExists() {
        seedLocalMessage(
            id = TEST_ID_BASE,
            protocol = MessageData.PROTOCOL_MMS,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            messageUri = "content://mms/$TEST_ID_BASE",
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        telephonyProvider.addMmsRow(id = TEST_ID_BASE, timestampMillis = TEST_TIMESTAMP_MILLIS)

        runSyncBatch()

        assertMessageCount(id = TEST_ID_BASE, expectedCount = 1)
    }

    @Test
    fun syncKeepsIncomingMmsNotificationWhenMatchingRemoteNotificationRowStillExists() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )
        telephonyProvider.addMmsRow(
            id = TEST_ID_BASE,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            messageType = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND,
        )

        runSyncBatch()

        assertIncomingMmsNotificationStillDownloading(id = TEST_ID_BASE)
        assertEquals(1, countMessagesByUri(messageUri = "content://mms/$TEST_ID_BASE"))
    }

    @Test
    fun syncDeletesYetToManualDownloadMmsNotificationWhenRemoteRowIsMissing() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )

        runSyncBatch()

        assertMessageCount(id = TEST_ID_BASE, expectedCount = 0)
    }

    @Test
    fun syncCountTreatsYetToManualDownloadMmsNotificationMissingRemoteRowAsUnsynchronized() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )

        assertFalse(isSynchronized())
    }

    @Test
    fun syncDeletesExpiredIncomingMmsNotificationWhenRemoteRowIsMissing() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            mmsExpiryMillis = EXPIRED_MMS_EXPIRY_MILLIS,
        )

        runSyncBatch()

        assertMessageCount(id = TEST_ID_BASE, expectedCount = 0)
    }

    @Test
    fun syncDeletesTerminalIncomingMmsNotificationStatusesWhenRemoteRowIsMissing() {
        terminalMmsNotificationStatuses.forEachIndexed { index, status ->
            seedIncomingMmsPushNotification(
                id = TEST_ID_BASE + index,
                status = status,
                timestampMillis = TEST_TIMESTAMP_MILLIS - index,
            )
        }

        runSyncBatch()

        terminalMmsNotificationStatuses.forEachIndexed { index, status ->
            assertMessageCount(
                id = TEST_ID_BASE + index,
                expectedCount = 0,
                failureMessage = "Terminal MMS notification status $status was not deleted",
            )
        }
    }

    @Test
    fun syncKeepsUnknownExpiryIncomingMmsNotificationWhenRemoteRowIsMissing() {
        seedIncomingMmsPushNotification(
            id = TEST_ID_BASE,
            status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            timestampMillis = TEST_TIMESTAMP_MILLIS,
            mmsExpiryMillis = UNKNOWN_MMS_EXPIRY_MILLIS,
        )

        runSyncBatch()

        val message = requireNotNull(readMessage(id = TEST_ID_BASE))
        assertEquals(MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION, message.getProtocol())
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING, message.getStatus())
    }

    @Test
    fun syncStillDeletesCompletedMmsWhenRemoteRowIsMissing() {
        seedLocalMessage(
            id = TEST_ID_BASE,
            protocol = MessageData.PROTOCOL_MMS,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            messageUri = "content://mms/$TEST_ID_BASE",
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )

        runSyncBatch()

        assertMessageCount(id = TEST_ID_BASE, expectedCount = 0)
    }

    @Test
    fun syncStillDeletesSmsWhenRemoteRowIsMissing() {
        seedLocalMessage(
            id = TEST_ID_BASE,
            protocol = MessageData.PROTOCOL_SMS,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            messageUri = "content://sms/$TEST_ID_BASE",
            timestampMillis = TEST_TIMESTAMP_MILLIS,
        )

        runSyncBatch()

        assertMessageCount(id = TEST_ID_BASE, expectedCount = 0)
    }

    private fun testFactory(context: Context, dataModel: DataModel): Factory {
        val gServices = mockk<BugleGservices>(relaxed = true)
        every { gServices.getLong(any(), any()) } answers { secondArg() }
        every { gServices.getInt(any(), any()) } answers { secondArg() }
        every { gServices.getBoolean(any(), any()) } answers { secondArg() }
        every { gServices.getString(any(), any()) } answers { secondArg() }
        every { gServices.getFloat(any(), any()) } answers { secondArg() }

        val prefs = mockk<BuglePrefs>(relaxed = true)
        every { prefs.getSharedPreferencesName() } returns BuglePrefs.SHARED_PREFERENCES_NAME
        every { prefs.getInt(any(), any()) } answers { secondArg() }
        every { prefs.getLong(any(), any()) } answers { secondArg() }
        every { prefs.getBoolean(any(), any()) } answers { secondArg() }
        every { prefs.getString(any(), any()) } answers { secondArg() }

        val phoneUtils = mockk<PhoneUtils>(relaxed = true)
        every { phoneUtils.getSubIdFromTelephony(any(), any()) } answers {
            firstArg<Cursor>().getInt(secondArg<Int>())
        }
        every { phoneUtils.getCanonicalBySimLocale(any()) } answers { firstArg() }
        every { phoneUtils.formatForDisplay(any()) } answers { firstArg() }

        return mockk<Factory>(relaxed = true).also { factory ->
            every { factory.getApplicationContext() } returns context
            every { factory.getDataModel() } returns dataModel
            every { factory.getBugleGservices() } returns gServices
            every { factory.getApplicationPrefs() } returns prefs
            every { factory.getSubscriptionPrefs(any()) } returns prefs
            every { factory.getWidgetPrefs() } returns prefs
            every { factory.getPhoneUtils(any()) } returns phoneUtils
        }
    }

    private fun runSyncBatch() {
        var failure: Throwable? = null
        val thread = Thread(
            {
                try {
                    runSyncBatchOnWorker()
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

    private fun runSyncBatchOnWorker() {
        val cursorPair = SyncCursorPair(
            -1L,
            TEST_TIMESTAMP_MILLIS + 1_000L,
        )
        val smsToAdd = ArrayList<DatabaseMessages.SmsMessage>()
        val mmsToAdd = LongSparseArray<DatabaseMessages.MmsMessage>()
        val messagesToDelete = ArrayList<DatabaseMessages.LocalDatabaseMessage>()

        cursorPair.query(database)

        try {
            cursorPair.scan(
                4_000,
                80,
                smsToAdd,
                mmsToAdd,
                messagesToDelete,
                dataModel.syncManager.threadInfoCache,
            )
        } finally {
            cursorPair.close()
        }

        val mmsMessagesToAdd = mmsToAdd.values()
        mmsMessagesToAdd.forEach { mms ->
            if (mms.mType == Mms.MESSAGE_BOX_INBOX) {
                mms.setSender(telephonyProvider.senderForThread(threadId = mms.mThreadId))
            }
        }

        val batch = SyncMessageBatch(
            smsToAdd,
            ArrayList(mmsMessagesToAdd),
            messagesToDelete,
            dataModel.syncManager.threadInfoCache,
        )
        batch.updateLocalDatabase()
    }

    private fun isSynchronized(): Boolean {
        return SyncCursorPair(
            -1L,
            TEST_TIMESTAMP_MILLIS + 1_000L,
        ).isSynchronized(database)
    }

    private fun isMmsNotificationDownloadCountSynchronized(
        localCount: Int,
        remoteCount: Int,
    ): Boolean {
        return IsMmsNotificationDownloadCountSynchronized(
            context = context,
            database = database,
        ).invoke(
            localCount = localCount,
            remoteCount = remoteCount,
            localSelection = ALL_MESSAGES_SELECTION,
            localSelectionArgs = null,
            smsSelection = ALL_MESSAGES_SELECTION,
            smsSelectionArgs = null,
            mmsSelection = ALL_MESSAGES_SELECTION,
            mmsSelectionArgs = null,
        )
    }

    private fun seedIncomingMmsPushNotification(
        id: Long,
        status: Int,
        timestampMillis: Long,
        mmsExpiryMillis: Long = UNEXPIRED_MMS_EXPIRY_MILLIS,
        selfParticipantId: Long = SELF_PARTICIPANT_DB_ID,
    ) {
        seedLocalMessage(
            id = id,
            protocol = MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION,
            status = status,
            messageUri = "content://mms/$id",
            timestampMillis = timestampMillis,
            mmsExpiryMillis = mmsExpiryMillis,
            selfParticipantId = selfParticipantId,
        )
    }

    private fun seedSelfParticipant(
        id: Long = SELF_PARTICIPANT_DB_ID,
        subId: Int = ParticipantData.DEFAULT_SELF_SUB_ID,
    ) {
        val values = ParticipantData
            .getSelfParticipant(subId)
            .toContentValues()
        values.put(BaseColumns._ID, id)

        database.insert(DatabaseHelper.PARTICIPANTS_TABLE, null, values)
    }

    private fun seedLocalMessage(
        id: Long,
        protocol: Int,
        status: Int,
        messageUri: String,
        timestampMillis: Long,
        mmsExpiryMillis: Long = UNEXPIRED_MMS_EXPIRY_MILLIS,
        selfParticipantId: Long = SELF_PARTICIPANT_DB_ID,
    ) {
        val participantId = id + PARTICIPANT_ID_OFFSET
        val conversationId = id + CONVERSATION_ID_OFFSET

        database.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to ParticipantData.OTHER_THAN_SELF_SUB_ID,
                ParticipantColumns.SIM_SLOT_ID to -1,
                ParticipantColumns.NORMALIZED_DESTINATION to "+1555$id",
                ParticipantColumns.SEND_DESTINATION to "+1555$id",
                ParticipantColumns.DISPLAY_DESTINATION to "+1555$id",
                ParticipantColumns.FULL_NAME to "Issue 144 sender $id",
                ParticipantColumns.FIRST_NAME to "Issue144-$id",
                ParticipantColumns.CONTACT_ID to
                    ParticipantData.PARTICIPANT_CONTACT_ID_NOT_RESOLVED,
                ParticipantColumns.BLOCKED to 0,
                ParticipantColumns.SUBSCRIPTION_COLOR to 0,
                BaseColumns._ID to participantId,
            ),
        )
        database.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.SMS_THREAD_ID to id,
                ConversationColumns.NAME to "Issue 144 conversation $id",
                ConversationColumns.LATEST_MESSAGE_ID to id,
                ConversationColumns.SNIPPET_TEXT to "Synthetic issue 144 message $id",
                ConversationColumns.SORT_TIMESTAMP to timestampMillis,
                ConversationColumns.LAST_READ_TIMESTAMP to 0L,
                ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION to "+1555$id",
                ConversationColumns.CURRENT_SELF_ID to selfParticipantId.toString(),
                ConversationColumns.PARTICIPANT_COUNT to 1,
                ConversationColumns.NOTIFICATION_ENABLED to 1,
                ConversationColumns.NOTIFICATION_VIBRATION to 1,
                BaseColumns._ID to conversationId,
            ),
        )
        database.insert(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ConversationParticipantsColumns.CONVERSATION_ID to conversationId,
                ConversationParticipantsColumns.PARTICIPANT_ID to participantId,
            ),
        )
        database.insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to participantId,
                MessageColumns.SENT_TIMESTAMP to timestampMillis,
                MessageColumns.RECEIVED_TIMESTAMP to timestampMillis,
                MessageColumns.PROTOCOL to protocol,
                MessageColumns.STATUS to status,
                MessageColumns.SEEN to 0,
                MessageColumns.READ to 0,
                MessageColumns.SMS_MESSAGE_URI to messageUri,
                MessageColumns.MMS_TRANSACTION_ID to transactionIdFor(id = id),
                MessageColumns.MMS_CONTENT_LOCATION to "http://example.invalid/mms/$id",
                MessageColumns.MMS_EXPIRY to mmsExpiryMillis,
                MessageColumns.RAW_TELEPHONY_STATUS to 0,
                MessageColumns.SELF_PARTICIPANT_ID to selfParticipantId,
                BaseColumns._ID to id,
            ),
        )
    }

    private fun countMessages(id: Long): Int {
        return countRows(
            table = DatabaseHelper.MESSAGES_TABLE,
            selection = "${BaseColumns._ID}=?",
            selectionArgs = arrayOf(id.toString()),
        )
    }

    private fun countMessagesByUri(messageUri: String): Int {
        return countRows(
            table = DatabaseHelper.MESSAGES_TABLE,
            selection = "${MessageColumns.SMS_MESSAGE_URI}=?",
            selectionArgs = arrayOf(messageUri),
        )
    }

    private fun countConversations(id: String): Int {
        return countRows(
            table = DatabaseHelper.CONVERSATIONS_TABLE,
            selection = "${BaseColumns._ID}=?",
            selectionArgs = arrayOf(id),
        )
    }

    private fun countRows(
        table: String,
        selection: String,
        selectionArgs: Array<String>,
    ): Int {
        return database.query(
            table,
            arrayOf(BaseColumns._ID),
            selection,
            selectionArgs,
            null,
            null,
            null,
            null,
        ).use { cursor ->
            cursor.count
        }
    }

    private fun readMessage(id: Long): MessageData? {
        return database.query(
            DatabaseHelper.MESSAGES_TABLE,
            MessageData.getProjection(),
            "${BaseColumns._ID}=?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            null,
        ).use { cursor ->
            when {
                !cursor.moveToFirst() -> null
                else -> {
                    MessageData().apply {
                        bind(cursor)
                    }
                }
            }
        }
    }

    private fun assertMessageCount(
        id: Long,
        expectedCount: Int,
        failureMessage: String = "Unexpected message count for $id",
    ) {
        assertEquals(failureMessage, expectedCount, countMessages(id))
    }

    private fun assertIncomingMmsNotificationStillDownloading(id: Long) {
        val message = requireNotNull(readMessage(id = id))

        assertEquals(MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION, message.getProtocol())
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING, message.getStatus())
        assertMessageCount(id = id, expectedCount = 1)
    }

    private fun deleteSyntheticRows() {
        database.delete(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            "${ConversationParticipantsColumns.CONVERSATION_ID} BETWEEN ? AND ?",
            arrayOf(
                (TEST_ID_BASE + CONVERSATION_ID_OFFSET).toString(),
                (TEST_ID_BASE + protectedStatusCount + CONVERSATION_ID_OFFSET).toString(),
            ),
        )
        database.delete(
            DatabaseHelper.MESSAGES_TABLE,
            "${BaseColumns._ID} BETWEEN ? AND ?",
            arrayOf(
                TEST_ID_BASE.toString(),
                (TEST_ID_BASE + protectedStatusCount).toString(),
            ),
        )
        database.delete(
            DatabaseHelper.CONVERSATIONS_TABLE,
            "${BaseColumns._ID} BETWEEN ? AND ?",
            arrayOf(
                (TEST_ID_BASE + CONVERSATION_ID_OFFSET).toString(),
                (TEST_ID_BASE + protectedStatusCount + CONVERSATION_ID_OFFSET).toString(),
            ),
        )
        database.delete(
            DatabaseHelper.PARTICIPANTS_TABLE,
            "${BaseColumns._ID} BETWEEN ? AND ?",
            arrayOf(
                (TEST_ID_BASE + PARTICIPANT_ID_OFFSET).toString(),
                (TEST_ID_BASE + protectedStatusCount + PARTICIPANT_ID_OFFSET).toString(),
            ),
        )
    }

    private class FakeTelephonyProvider : ContentProvider() {
        private val mmsRows = mutableListOf<Map<String, Any?>>()
        private val threadRecipientIds = mutableMapOf<Long, Long>()

        var remoteMmsIdBatchQueryCount = 0
            private set

        var remoteMmsReplacementQueryCount = 0
            private set

        var failRemoteMmsIdBatchQuery = false
        var failRemoteMmsReplacementQuery = false

        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI("sms", null, SMS)
            addURI("mms", null, MMS)
            addURI("mms-sms", "conversations", THREADS)
            addURI("mms-sms", "canonical-address/#", CANONICAL_ADDRESS)
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
            return when (matcher.match(uri)) {
                SMS -> {
                    val projectedColumns = projection ?: smsProjectionColumns

                    if (projectedColumns.isCountProjection()) {
                        countCursor(projection = projectedColumns, count = 0)
                    } else {
                        matrixCursor(projectedColumns, emptyList())
                    }
                }
                MMS -> {
                    val projectedColumns = projection ?: mmsProjectionColumns
                    val rows = selectedMmsRows(
                        selection = selection,
                        selectionArgs = selectionArgs,
                    )

                    if (projectedColumns.isCountProjection()) {
                        countCursor(projection = projectedColumns, count = rows.size)
                    } else {
                        matrixCursor(projectedColumns, rows)
                    }
                }
                THREADS -> matrixCursor(
                    projection ?: threadProjectionColumns,
                    threadRows(selectionArgs = selectionArgs),
                )
                CANONICAL_ADDRESS -> matrixCursor(
                    projection ?: canonicalAddressProjectionColumns,
                    canonicalAddressRows(uri = uri),
                )
                else -> matrixCursor(projection ?: emptyArray(), emptyList())
            }
        }

        override fun getType(uri: Uri): String? {
            return null
        }

        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            return null
        }

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int {
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

        fun addMmsRow(
            id: Long,
            timestampMillis: Long,
            threadId: Long = id,
            transactionId: String? = transactionIdFor(id = id),
            messageType: Int = PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF,
        ) {
            mmsRows += mapOf(
                Mms._ID to id,
                Mms.MESSAGE_BOX to Mms.MESSAGE_BOX_INBOX,
                Mms.SUBJECT to null,
                Mms.SUBJECT_CHARSET to 106,
                Mms.MESSAGE_SIZE to 0,
                Mms.DATE to timestampMillis / 1_000L,
                Mms.DATE_SENT to 0L,
                Mms.THREAD_ID to threadId,
                Mms.PRIORITY to 129,
                Mms.STATUS to 0,
                Mms.READ to 0,
                Mms.SEEN to 0,
                Mms.CONTENT_LOCATION to "http://example.invalid/mms/$id",
                Mms.TRANSACTION_ID to transactionId,
                Mms.MESSAGE_TYPE to messageType,
                Mms.EXPIRY to (timestampMillis + 86_400_000L) / 1_000L,
                Mms.RESPONSE_STATUS to 0,
                Mms.RETRIEVE_STATUS to 0,
                Mms.SUBSCRIPTION_ID to ParticipantData.DEFAULT_SELF_SUB_ID,
            )
        }

        fun setThreadRecipient(threadId: Long, recipientId: Long) {
            threadRecipientIds[threadId] = recipientId
        }

        fun senderForThread(threadId: Long): String {
            val recipientId = threadRecipientIds[threadId] ?: threadId
            return "+1555$recipientId"
        }

        private fun countCursor(projection: Array<out String>, count: Int): MatrixCursor {
            return MatrixCursor(projection).apply {
                addRow(arrayOf(count))
            }
        }

        private fun matrixCursor(
            projection: Array<out String>,
            rows: List<Map<String, Any?>>,
        ): MatrixCursor {
            return MatrixCursor(projection).apply {
                rows.forEach { row ->
                    projection
                        .map { column -> row[column] }
                        .toTypedArray()
                        .let(::addRow)
                }
            }
        }

        private fun selectedMmsRows(
            selection: String?,
            selectionArgs: Array<out String>?,
        ): List<Map<String, Any?>> {
            return when {
                selection == null -> orderedMmsRows()
                selection == "${Mms._ID}=?" -> selectedMmsRowsById(selectionArgs = selectionArgs)
                selection.isMmsIdBatchSelection() -> {
                    remoteMmsIdBatchQueryCount++
                    if (failRemoteMmsIdBatchQuery) {
                        throw IllegalArgumentException("Remote MMS id batch query failed")
                    }

                    selectedMmsRowsByIds(selectionArgs = selectionArgs)
                }
                selection.isReplacementSelection() -> {
                    remoteMmsReplacementQueryCount++
                    if (failRemoteMmsReplacementQuery) {
                        throw IllegalStateException("Remote MMS replacement query failed")
                    }

                    selectedReplacementMmsRows(
                        selection = selection,
                        selectionArgs = selectionArgs,
                    )
                }
                selection.isSyncMmsSelection() -> orderedMmsRows()
                else -> error("Unexpected MMS query: selection=$selection")
            }
        }

        private fun selectedMmsRowsById(
            selectionArgs: Array<out String>?,
        ): List<Map<String, Any?>> {
            val rowId = selectionArgs?.firstOrNull()?.toLongOrNull()
                ?: error("Missing MMS row id selection arg")
            return selectedMmsRowsByIds(rowIds = setOf(rowId))
        }

        private fun selectedMmsRowsByIds(
            selectionArgs: Array<out String>?,
        ): List<Map<String, Any?>> {
            val rowIds = selectionArgs
                ?.map { value -> value.toLong() }
                ?.toSet()
                ?: error("Missing MMS row id selection args")
            return selectedMmsRowsByIds(rowIds = rowIds)
        }

        private fun selectedMmsRowsByIds(rowIds: Set<Long>): List<Map<String, Any?>> {
            return orderedMmsRows().filter { row -> (row[Mms._ID] as Long) in rowIds }
        }

        private fun selectedReplacementMmsRows(
            selection: String,
            selectionArgs: Array<out String>?,
        ): List<Map<String, Any?>> {
            val args = selectionArgs ?: error("Missing replacement MMS selection args")
            val transactionIdCount = selection.inClausePlaceholderCount(column = Mms.TRANSACTION_ID)
            val messageBoxArgIndex = args.size - transactionIdCount - 2
            check(messageBoxArgIndex >= 0) {
                "Replacement MMS selection args do not match selection: $selection"
            }

            val messageBox = args[messageBoxArgIndex].toInt()
            val messageType = args[messageBoxArgIndex + 1].toInt()
            val transactionIds = args
                .drop(messageBoxArgIndex + 2)
                .toSet()

            return orderedMmsRows()
                .filter { row -> row[Mms.MESSAGE_BOX] == messageBox }
                .filter { row -> row[Mms.MESSAGE_TYPE] == messageType }
                .filter { row -> (row[Mms.TRANSACTION_ID] as? String) in transactionIds }
        }

        private fun orderedMmsRows(): List<Map<String, Any?>> {
            return mmsRows.sortedByDescending { row -> row[Mms.DATE] as Long }
        }

        private fun threadRows(selectionArgs: Array<out String>?): List<Map<String, Any?>> {
            val threadId = selectionArgs?.firstOrNull()?.toLongOrNull() ?: return emptyList()
            val recipientId = threadRecipientIds[threadId] ?: threadId

            return listOf(
                mapOf(
                    BaseColumns._ID to threadId,
                    THREAD_RECIPIENT_IDS to recipientId,
                ),
            )
        }

        private fun canonicalAddressRows(uri: Uri): List<Map<String, Any?>> {
            val recipientId = uri.lastPathSegment ?: return emptyList()

            return listOf(
                mapOf(CANONICAL_ADDRESS_COLUMN to "+1555$recipientId"),
            )
        }

        private companion object {
            private const val SMS = 1
            private const val MMS = 2
            private const val THREADS = 3
            private const val CANONICAL_ADDRESS = 4
        }
    }

    private companion object {
        private const val SELF_PARTICIPANT_DB_ID = 1L
        private const val OTHER_SELF_PARTICIPANT_DB_ID = 2L
        private const val OTHER_SELF_PARTICIPANT_SUB_ID = 2
        private const val TEST_ID_BASE = 144_000L
        private const val PARTICIPANT_ID_OFFSET = 10_000L
        private const val CONVERSATION_ID_OFFSET = 20_000L
        private const val TEST_TIMESTAMP_MILLIS = 1_780_920_000_000L
        private const val EXPIRED_MMS_EXPIRY_MILLIS = 1L
        private const val UNKNOWN_MMS_EXPIRY_MILLIS = 0L
        private const val UNEXPIRED_MMS_EXPIRY_MILLIS = 4_102_444_800_000L
        private const val ALL_MESSAGES_SELECTION = "1=1"
        private const val THREAD_RECIPIENT_IDS = "recipient_ids"
        private const val CANONICAL_ADDRESS_COLUMN = "address"

        private val protectedStatusCount = mmsNotificationDownloadStatuses.size.toLong()

        private val terminalMmsNotificationStatuses = intArrayOf(
            MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED,
            MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE,
        )

        private val smsProjectionColumns = arrayOf(
            Sms._ID,
            Sms.TYPE,
            Sms.ADDRESS,
            Sms.BODY,
            Sms.DATE,
            Sms.DATE_SENT,
            Sms.THREAD_ID,
            Sms.STATUS,
            Sms.READ,
            Sms.SEEN,
            Sms.SUBSCRIPTION_ID,
        )

        private val mmsProjectionColumns = arrayOf(
            Mms._ID,
            Mms.MESSAGE_BOX,
            Mms.SUBJECT,
            Mms.SUBJECT_CHARSET,
            Mms.MESSAGE_SIZE,
            Mms.DATE,
            Mms.DATE_SENT,
            Mms.THREAD_ID,
            Mms.PRIORITY,
            Mms.STATUS,
            Mms.READ,
            Mms.SEEN,
            Mms.CONTENT_LOCATION,
            Mms.TRANSACTION_ID,
            Mms.MESSAGE_TYPE,
            Mms.EXPIRY,
            Mms.RESPONSE_STATUS,
            Mms.RETRIEVE_STATUS,
            Mms.SUBSCRIPTION_ID,
        )

        private val threadProjectionColumns = arrayOf(
            BaseColumns._ID,
            THREAD_RECIPIENT_IDS,
        )

        private val canonicalAddressProjectionColumns = arrayOf(
            CANONICAL_ADDRESS_COLUMN,
        )

        private fun transactionIdFor(id: Long): String {
            return "issue144-tx-$id"
        }
    }
}

private fun Array<out String>.isCountProjection(): Boolean {
    return size == 1 && first() == "count()"
}

private fun String.isMmsIdBatchSelection(): Boolean {
    return startsWith("${Mms._ID} IN (")
}

private fun String.isReplacementSelection(): Boolean {
    return contains("${Mms.MESSAGE_BOX}=?") &&
        contains("${Mms.MESSAGE_TYPE}=?") &&
        contains("${Mms.TRANSACTION_ID} IN (")
}

private fun String.isSyncMmsSelection(): Boolean {
    return contains("${Mms.MESSAGE_BOX} IN (") &&
        contains("${Mms.MESSAGE_TYPE} IN (")
}

private fun String.inClausePlaceholderCount(column: String): Int {
    val inClause = substringAfter("$column IN (", missingDelimiterValue = "")
        .substringBefore(")")
    check(inClause.isNotEmpty()) {
        "Missing IN clause for $column in $this"
    }

    return inClause.count { char -> char == '?' }
}

private fun <T> LongSparseArray<T>.values(): List<T> {
    return (0 until size()).map { index -> valueAt(index) }
}

internal class SyncMessagesActionMmsReplacementRaceTestApplication : BugleApplication() {
    override fun onCreate() {
        setTestsRunning()
    }
}
