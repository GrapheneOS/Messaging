package com.android.messaging.datamodel.action

import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import com.android.messaging.BugleApplication
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.ActionSyncTestDataModel
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.createTestFactory
import com.android.messaging.util.BuglePrefsKeys
import io.mockk.every
import io.mockk.unmockkAll
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver

internal class OutgoingPipelineTestApplication : BugleApplication() {
    override fun onCreate() {
        setTestsRunning()
    }
}

internal data class OutgoingMessageRow(
    val id: String,
    val conversationId: String,
    val selfId: String,
    val protocol: Int,
    val status: Int,
    val receivedTimestamp: Long,
    val sentTimestamp: Long,
    val retryStartTimestamp: Long,
    val seen: Boolean,
    val rawTelephonyStatus: Int,
    val smsMessageUri: String?,
)

private class PipelineTestDataModel(
    private val actionService: ActionService,
    private val backgroundWorker: BackgroundWorker,
) : ActionSyncTestDataModel() {

    override fun getActionService(): ActionService {
        return actionService
    }

    override fun getBackgroundWorkerForActionService(): BackgroundWorker {
        return backgroundWorker
    }
}

/**
 * The legacy outgoing pipeline's surroundings: an in-memory bugle_db with one self per SIM, the
 * fake telephony layer, and an action service that only runs actions when asked to.
 */
internal class OutgoingPipelineWorld {

    val context: Context = RuntimeEnvironment.getApplication()
    val telephony = FakeTelephony()
    val incomingMms = FakeIncomingMms(telephony = telephony)
    val actionService = RecordingActionService()
    val backgroundWorker = CapturingBackgroundWorker()
    val runner = OutgoingPipelineActionRunner(
        actionService = actionService,
        backgroundWorker = backgroundWorker,
    )

    private val prefsBySubId = mutableMapOf<Int, FakeBuglePrefs>()
    private val dataModel = PipelineTestDataModel(
        actionService = actionService,
        backgroundWorker = backgroundWorker,
    )
    private lateinit var database: DatabaseWrapper

    fun setUp() {
        telephony.install()
        incomingMms.install()
        val factory = createTestFactory(
            context = context,
            dataModel = dataModel,
            prefs = FakeBuglePrefs(),
            phoneUtils = telephony.phoneUtils,
        )
        every { factory.getSubscriptionPrefs(any()) } answers { prefsFor(subId = firstArg()) }
        FactoryTestAccess.install(factory)
        onWorker {
            database = createInMemoryActionSyncTestDatabase(context)
            dataModel.setDatabase(database)
            clearParticipantCaches()
            seedSelves()
        }
    }

    fun tearDown() {
        ShadowContentResolver.reset()
        FactoryTestAccess.reset()
        unmockkAll()
    }

    fun <T> onWorker(block: () -> T): T {
        var result: Result<T>? = null
        val worker = Thread(
            { result = runCatching(block) },
            WORKER_THREAD_NAME,
        )
        worker.start()
        worker.join()
        return requireNotNull(result).getOrThrow()
    }

    fun <T> withDatabase(block: (DatabaseWrapper) -> T): T {
        return onWorker { block(database) }
    }

    fun focusConversation(conversationId: String?) {
        dataModel.setFocusedConversation(conversationId)
    }

    fun showConversationListAtNewest(scrolledToNewest: Boolean) {
        dataModel.setConversationListScrolledToNewestConversation(scrolledToNewest)
    }

    fun prefsFor(subId: Int): FakeBuglePrefs {
        return synchronized(prefsBySubId) {
            prefsBySubId.getOrPut(subId) { FakeBuglePrefs(sharedPreferencesName = "sub_$subId") }
        }
    }

    fun pendingRetryAttempt(subId: Int): Int {
        return prefsFor(subId = subId).getInt(
            BuglePrefsKeys.PROCESS_PENDING_MESSAGES_RETRY_COUNT,
            0,
        )
    }

    fun selfIdFor(subId: Int): String {
        return requireNotNull(SELF_IDS[subId]) { "No self is seeded for subId $subId" }
    }

    fun createConversation(recipients: List<String>, selfSubId: Int? = null): String {
        return onWorker {
            val participants = ArrayList(
                recipients.map { recipient ->
                    ParticipantData.getFromRawPhoneBySimLocale(
                        recipient,
                        FakeTelephony.SUB_ID_SIM_1,
                    )
                },
            )
            val conversationId = BugleDatabaseOperations.getOrCreateConversation(
                database,
                telephony.threadIdFor(recipients = recipients),
                false,
                participants,
                false,
                false,
                null,
            )
            selfSubId?.let { subId ->
                inTransaction {
                    BugleDatabaseOperations.updateConversationSelfIdInTransaction(
                        database,
                        conversationId,
                        selfIdFor(subId = subId),
                    )
                }
            }
            conversationId
        }
    }

    fun seedOutgoingSms(
        conversationId: String,
        selfSubId: Int,
        text: String,
        status: Int,
        receivedTimestamp: Long,
        retryStartTimestamp: Long = receivedTimestamp,
    ): String {
        val message = MessageData.createDraftSmsMessage(
            conversationId,
            selfIdFor(subId = selfSubId),
            text,
        )
        return seedOutgoing(
            message = message,
            smsMessageUri = telephony.allocateSmsUri(),
            status = status,
            receivedTimestamp = receivedTimestamp,
            retryStartTimestamp = retryStartTimestamp,
        )
    }

    fun seedOutgoingMms(
        conversationId: String,
        selfSubId: Int,
        text: String,
        status: Int,
        receivedTimestamp: Long,
        retryStartTimestamp: Long = receivedTimestamp,
    ): String {
        val message = MessageData.createDraftMmsMessage(
            conversationId,
            selfIdFor(subId = selfSubId),
            text,
            null,
        )
        return seedOutgoing(
            message = message,
            smsMessageUri = null,
            status = status,
            receivedTimestamp = receivedTimestamp,
            retryStartTimestamp = retryStartTimestamp,
        )
    }

    fun message(messageId: String): OutgoingMessageRow {
        return requireNotNull(
            queryMessages(
                selection = "${BaseColumns._ID} = ?",
                selectionArgs = arrayOf(messageId),
            ).singleOrNull(),
        ) { "Message $messageId is missing" }
    }

    fun messagesIn(conversationId: String): List<OutgoingMessageRow> {
        return queryMessages(
            selection = "${MessageColumns.CONVERSATION_ID} = ?",
            selectionArgs = arrayOf(conversationId),
        )
    }

    fun allMessages(): List<OutgoingMessageRow> {
        return queryMessages(selection = null, selectionArgs = null)
    }

    fun conversationIdForThread(threadId: Long): String? {
        return onWorker {
            BugleDatabaseOperations.getExistingConversation(database, threadId, false)
        }
    }

    private fun seedOutgoing(
        message: MessageData,
        smsMessageUri: Uri?,
        status: Int,
        receivedTimestamp: Long,
        retryStartTimestamp: Long,
    ): String {
        return onWorker {
            message.updateSendingMessage(message.conversationId, smsMessageUri, receivedTimestamp)
            inTransaction {
                BugleDatabaseOperations.insertNewMessageInTransaction(database, message)
                BugleDatabaseOperations.updateMessageRow(
                    database,
                    message.messageId,
                    contentValuesOf(
                        MessageColumns.STATUS to status,
                        MessageColumns.RETRY_START_TIMESTAMP to retryStartTimestamp,
                    ),
                )
            }
            message.messageId
        }
    }

    private fun queryMessages(
        selection: String?,
        selectionArgs: Array<String>?,
    ): List<OutgoingMessageRow> {
        return onWorker {
            database.query(
                DatabaseHelper.MESSAGES_TABLE,
                MESSAGE_COLUMNS,
                selection,
                selectionArgs,
                null,
                null,
                "${MessageColumns.RECEIVED_TIMESTAMP} ASC, ${BaseColumns._ID} ASC",
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            OutgoingMessageRow(
                                id = cursor.getString(0),
                                conversationId = cursor.getString(1),
                                selfId = cursor.getString(2),
                                protocol = cursor.getInt(3),
                                status = cursor.getInt(4),
                                receivedTimestamp = cursor.getLong(5),
                                sentTimestamp = cursor.getLong(6),
                                retryStartTimestamp = cursor.getLong(7),
                                seen = cursor.getInt(8) != 0,
                                rawTelephonyStatus = cursor.getInt(9),
                                smsMessageUri = cursor.getString(10),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun seedSelves() {
        SELF_IDS.forEach { (subId, selfId) ->
            val values = ParticipantData.getSelfParticipant(subId).toContentValues().apply {
                put(BaseColumns._ID, selfId.toLong())
                put(ParticipantColumns.SIM_SLOT_ID, SIM_SLOTS.getValue(subId))
            }
            database.insert(DatabaseHelper.PARTICIPANTS_TABLE, null, values)
        }
    }

    private fun clearParticipantCaches() {
        BugleDatabaseOperations.clearParticipantIdCache()
        val cacheField = ParticipantData::class.java.getDeclaredField(PARTICIPANT_ID_CACHE_FIELD)
        cacheField.isAccessible = true
        (cacheField.get(null) as MutableMap<*, *>).clear()
    }

    private fun inTransaction(block: () -> Unit) {
        database.beginTransaction()
        try {
            block()
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    companion object {
        const val SUB_ID_INACTIVE_SIM = 3
        private const val WORKER_THREAD_NAME = "outgoing-pipeline-test-worker"
        private const val PARTICIPANT_ID_CACHE_FIELD = "sSubIdtoParticipantIdCache"
        private val SELF_IDS = linkedMapOf(
            ParticipantData.DEFAULT_SELF_SUB_ID to "1",
            FakeTelephony.SUB_ID_SIM_1 to "2",
            FakeTelephony.SUB_ID_SIM_2 to "3",
            SUB_ID_INACTIVE_SIM to "4",
        )
        private val SIM_SLOTS = mapOf(
            ParticipantData.DEFAULT_SELF_SUB_ID to ParticipantData.INVALID_SLOT_ID,
            FakeTelephony.SUB_ID_SIM_1 to 0,
            FakeTelephony.SUB_ID_SIM_2 to 1,
            SUB_ID_INACTIVE_SIM to ParticipantData.INVALID_SLOT_ID,
        )
        private val MESSAGE_COLUMNS = arrayOf(
            BaseColumns._ID,
            MessageColumns.CONVERSATION_ID,
            MessageColumns.SELF_PARTICIPANT_ID,
            MessageColumns.PROTOCOL,
            MessageColumns.STATUS,
            MessageColumns.RECEIVED_TIMESTAMP,
            MessageColumns.SENT_TIMESTAMP,
            MessageColumns.RETRY_START_TIMESTAMP,
            MessageColumns.SEEN,
            MessageColumns.RAW_TELEPHONY_STATUS,
            MessageColumns.SMS_MESSAGE_URI,
        )
    }
}
