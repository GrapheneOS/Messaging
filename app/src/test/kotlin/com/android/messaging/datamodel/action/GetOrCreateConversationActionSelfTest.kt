package com.android.messaging.datamodel.action

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Telephony opens a conversation with emergency services on the SIM connected to the satellite
 * network, so the conversation has to send from that SIM rather than the default one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
internal class GetOrCreateConversationActionSelfTest {

    private lateinit var context: Context
    private lateinit var database: DatabaseWrapper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext

        val dataModel = mockk<DataModel>(relaxed = true)
        installTestFactory(context = context, dataModel = dataModel)
        database = createInMemoryActionSyncTestDatabase(context)
        every { dataModel.database } returns database
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun setConversationSelf_sendsFromTheRequestedActiveSubscription() {
        val defaultSelfId = insertSelf(subId = 11, slotId = 0)
        val satelliteSelfId = insertSelf(subId = 12, slotId = 1)
        val conversationId = insertConversation(selfId = defaultSelfId)

        setConversationSelfOnWorkerThread(conversationId, subId = 12)

        assertEquals(satelliteSelfId, readConversationSelfId(conversationId))
    }

    @Test
    fun setConversationSelf_keepsTheCurrentSimForAnInactiveSubscription() {
        val defaultSelfId = insertSelf(subId = 21, slotId = 0)
        insertSelf(subId = 22, slotId = ParticipantData.INVALID_SLOT_ID)
        val conversationId = insertConversation(selfId = defaultSelfId)

        setConversationSelfOnWorkerThread(conversationId, subId = 22)

        assertEquals(defaultSelfId, readConversationSelfId(conversationId))
    }

    /** Actions run on a worker thread, and the database helpers assert that they are not on main. */
    private fun setConversationSelfOnWorkerThread(conversationId: String, subId: Int) {
        var failure: Throwable? = null
        thread {
            try {
                GetOrCreateConversationAction.setConversationSelf(database, conversationId, subId)
            } catch (t: Throwable) {
                failure = t
            }
        }.join()
        failure?.let { throw it }
    }

    private fun insertSelf(subId: Int, slotId: Int): String {
        val rowId = database.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to subId,
                ParticipantColumns.SIM_SLOT_ID to slotId,
                ParticipantColumns.NORMALIZED_DESTINATION to "SELF($subId)",
                ParticipantColumns.CONTACT_ID to ParticipantData.PARTICIPANT_CONTACT_ID_NOT_FOUND,
            ),
        )
        assertTrue("participant insert failed", rowId >= 0)
        return rowId.toString()
    }

    private fun insertConversation(selfId: String): String {
        val rowId = database.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.NAME to "911",
                ConversationColumns.CURRENT_SELF_ID to selfId,
            ),
        )
        assertTrue("conversation insert failed", rowId >= 0)
        return rowId.toString()
    }

    private fun readConversationSelfId(conversationId: String): String? {
        return database.query(
            DatabaseHelper.CONVERSATIONS_TABLE,
            arrayOf(ConversationColumns.CURRENT_SELF_ID),
            "${ConversationColumns._ID}=?",
            arrayOf(conversationId),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}
