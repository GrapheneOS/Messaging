package com.android.messaging.data.participantdestination.store

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.participantdestination.model.BareNumberParticipant
import com.android.messaging.data.participantdestination.model.Renormalization
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.unmockkAll
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ParticipantDestinationDatabaseStoreImplTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val databaseStore = ParticipantDestinationDatabaseStoreImpl()
    private lateinit var db: DatabaseWrapper

    @Before
    fun setUp() {
        installTestFactory(context = context)
        db = createInMemoryActionSyncTestDatabase(context = context)
        every { DataModel.get().database } returns db
    }

    @After
    fun tearDown() {
        db.database.close()
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun aBareRowWithANormalizedTwin_isMergedIntoTheTwin() {
        val bareId = db.insertParticipant(destination = BARE_NUMBER, isBlocked = true)
        val twinId = db.insertParticipant(destination = CANONICAL_NUMBER, fullName = CONTACT_NAME)
        val otherId = db.insertParticipant(destination = OTHER_NUMBER)
        val directId = db.insertConversation(participantIds = listOf(bareId), name = BARE_NUMBER)
        val groupId = db.insertConversation(participantIds = listOf(bareId, twinId, otherId))
        val messageId = db.insertMessage(conversationId = directId, senderId = bareId)

        renormalizeBareNumbers()

        assertFalse("the bare row is still there", db.participantExists(id = bareId))
        assertEquals(twinId, db.senderOf(messageId = messageId))
        assertEquals(listOf(twinId), db.participantsOf(conversationId = directId))
        assertEquals(listOf(twinId, otherId), db.participantsOf(conversationId = groupId))
        assertEquals(2, db.participantCountOf(conversationId = groupId))
        assertEquals(
            CANONICAL_NUMBER,
            db.conversationColumn(
                conversationId = directId,
                column = ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION,
            ),
        )
        assertEquals(
            "the conversation still shows the bare number",
            CONTACT_NAME,
            db.conversationColumn(conversationId = directId, column = ConversationColumns.NAME),
        )
        assertTrue(
            "blocking the bare row must keep blocking the sender",
            db.isBlocked(id = twinId),
        )
    }

    @Test
    fun aBareRowMergedIntoABlockedTwin_hasItsConversationArchived() {
        val bareId = db.insertParticipant(destination = BARE_NUMBER)
        db.insertParticipant(destination = CANONICAL_NUMBER, isBlocked = true)
        val otherId = db.insertParticipant(destination = OTHER_NUMBER)
        val directId = db.insertConversation(participantIds = listOf(bareId))
        val groupId = db.insertConversation(participantIds = listOf(bareId, otherId))

        renormalizeBareNumbers()

        assertEquals("1", db.archiveStatusOf(conversationId = directId))
        assertEquals(
            "blocking a sender leaves group conversations in the inbox",
            "0",
            db.archiveStatusOf(conversationId = groupId),
        )
    }

    @Test
    fun aBlockedBareRowMergedIntoItsTwin_hasTheTwinsConversationArchived() {
        db.insertParticipant(destination = BARE_NUMBER, isBlocked = true)
        val twinId = db.insertParticipant(destination = CANONICAL_NUMBER)
        val otherId = db.insertParticipant(destination = OTHER_NUMBER)
        val directId = db.insertConversation(participantIds = listOf(twinId))
        val groupId = db.insertConversation(participantIds = listOf(twinId, otherId))

        renormalizeBareNumbers()

        assertTrue("the sender isn't blocked", db.isBlocked(id = twinId))
        assertEquals("1", db.archiveStatusOf(conversationId = directId))
        assertEquals(
            "blocking a sender leaves group conversations in the inbox",
            "0",
            db.archiveStatusOf(conversationId = groupId),
        )
    }

    @Test
    fun aBareRowWithoutATwin_isNormalizedInPlace() {
        val bareId = db.insertParticipant(destination = LONE_BARE_NUMBER)
        val conversationId = db.insertConversation(participantIds = listOf(bareId))

        renormalizeBareNumbers()

        assertEquals(
            LONE_CANONICAL_NUMBER,
            db.participantColumn(id = bareId, column = ParticipantColumns.NORMALIZED_DESTINATION),
        )
        assertEquals(
            ParticipantData.PARTICIPANT_CONTACT_ID_NOT_RESOLVED.toString(),
            db.participantColumn(id = bareId, column = ParticipantColumns.CONTACT_ID),
        )
        assertEquals(
            LONE_CANONICAL_NUMBER,
            db.conversationColumn(
                conversationId = conversationId,
                column = ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION,
            ),
        )
    }

    @Test
    fun rowsThatAreNotBareNumbers_areNotReadAsBareNumbers() {
        val bareId = db.insertParticipant(destination = BARE_NUMBER, isBlocked = true)
        listOf(CANONICAL_NUMBER, "AMAZON", "someone@example.com", "").forEach { destination ->
            db.insertParticipant(destination = destination)
        }
        db.insertParticipant(destination = LONE_BARE_NUMBER, subId = US_SUB_ID)

        assertEquals(
            listOf(
                BareNumberParticipant(
                    participantId = bareId.toString(),
                    destination = BARE_NUMBER,
                    isBlocked = true,
                ),
            ),
            databaseStore.readBareNumberParticipants(),
        )
    }

    @Test
    fun theSimsOfABareNumber_areThoseOfItsConversationsAndOfTheMessagesItSent() {
        val bareId = db.insertParticipant(destination = BARE_NUMBER)
        val twinId = db.insertParticipant(destination = CANONICAL_NUMBER)
        db.insertParticipant(destination = LONE_BARE_NUMBER)
        val bareConversationId = db.insertConversation(participantIds = listOf(bareId))
        val twinConversationId = db.insertConversation(participantIds = listOf(twinId))
        db.insertMessage(
            conversationId = bareConversationId,
            senderId = db.selfIdOf(subId = US_SUB_ID),
            selfSubId = US_SUB_ID,
        )
        db.insertMessage(
            conversationId = twinConversationId,
            senderId = bareId,
            selfSubId = FAROESE_SUB_ID,
        )
        db.insertMessage(
            conversationId = twinConversationId,
            senderId = twinId,
            selfSubId = GREENLANDIC_SUB_ID,
        )

        assertEquals(
            mapOf(bareId.toString() to setOf(US_SUB_ID, FAROESE_SUB_ID)),
            databaseStore.readSubIdsByParticipant(),
        )
    }

    @Test
    fun aMessageWithoutItsSim_wentThroughTheUnknownSim() {
        val bareId = db.insertParticipant(destination = BARE_NUMBER)
        db.insertMessage(
            conversationId = db.insertConversation(participantIds = listOf(bareId)),
            senderId = bareId,
        )

        assertEquals(
            mapOf(bareId.toString() to setOf(ParticipantData.DEFAULT_SELF_SUB_ID)),
            databaseStore.readSubIdsByParticipant(),
        )
    }

    @Test
    fun theSenderDestinations_leaveOutTheSelfParticipants() {
        db.insertParticipant(destination = BARE_NUMBER)
        db.insertParticipant(destination = CANONICAL_NUMBER)
        db.insertParticipant(destination = OTHER_NUMBER, subId = US_SUB_ID)

        assertEquals(setOf(BARE_NUMBER, CANONICAL_NUMBER), databaseStore.readSenderDestinations())
    }

    @Test
    fun theLastParticipantId_isTheHighestOne() {
        db.insertParticipant(destination = BARE_NUMBER)
        val lastId = db.insertParticipant(destination = CANONICAL_NUMBER)

        assertEquals(lastId, databaseStore.readLastParticipantId())
    }

    @Test
    fun noParticipants_haveTheLastParticipantIdZero() {
        db.delete(DatabaseHelper.PARTICIPANTS_TABLE, null, null)

        assertEquals(0L, databaseStore.readLastParticipantId())
    }

    // Updating a conversation's name asserts it runs off the main thread
    private fun renormalizeBareNumbers() {
        val renormalizations = databaseStore.readBareNumberParticipants().map { participant ->
            Renormalization(
                participant = participant,
                canonicalDestination = CANONICAL_DESTINATIONS.getValue(participant.destination),
            )
        }
        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit(
                Callable {
                    databaseStore.applyRenormalizations(renormalizations = renormalizations)
                },
            ).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } finally {
            executor.shutdown()
        }
    }

    private fun DatabaseWrapper.insertParticipant(
        destination: String,
        fullName: String? = null,
        isBlocked: Boolean = false,
        subId: Int = ParticipantData.OTHER_THAN_SELF_SUB_ID,
    ): Long {
        return insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to subId,
                ParticipantColumns.NORMALIZED_DESTINATION to destination,
                ParticipantColumns.SEND_DESTINATION to destination,
                ParticipantColumns.DISPLAY_DESTINATION to destination,
                ParticipantColumns.FULL_NAME to fullName,
                ParticipantColumns.CONTACT_ID to if (fullName == null) 0 else 1,
                ParticipantColumns.BLOCKED to if (isBlocked) 1 else 0,
            ),
        )
    }

    private fun DatabaseWrapper.insertConversation(
        participantIds: List<Long>,
        name: String = "Conversation",
    ): Long {
        val destination = when (participantIds.size) {
            1 -> participantColumn(
                id = participantIds.single(),
                column = ParticipantColumns.NORMALIZED_DESTINATION,
            )
            else -> null
        }
        val conversationId = insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.NAME to name,
                ConversationColumns.PARTICIPANT_COUNT to participantIds.size,
                ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION to destination,
            ),
        )
        participantIds.forEach { participantId ->
            insert(
                DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
                null,
                contentValuesOf(
                    ConversationParticipantsColumns.CONVERSATION_ID to conversationId,
                    ConversationParticipantsColumns.PARTICIPANT_ID to participantId,
                ),
            )
        }
        return conversationId
    }

    private fun DatabaseWrapper.insertMessage(
        conversationId: Long,
        senderId: Long,
        selfSubId: Int? = null,
    ): Long {
        return insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to senderId,
                MessageColumns.SELF_PARTICIPANT_ID to selfSubId?.let { subId ->
                    selfIdOf(subId = subId)
                },
            ),
        )
    }

    private fun DatabaseWrapper.selfIdOf(subId: Int): Long {
        val selfIds = queryLongs(
            sql = "SELECT _id FROM ${DatabaseHelper.PARTICIPANTS_TABLE} " +
                "WHERE ${ParticipantColumns.SUB_ID}=?",
            argument = subId.toLong(),
        )
        return selfIds.firstOrNull() ?: insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(ParticipantColumns.SUB_ID to subId),
        )
    }

    private fun DatabaseWrapper.participantExists(id: Long): Boolean {
        return queryLongs(
            sql = "SELECT _id FROM ${DatabaseHelper.PARTICIPANTS_TABLE} WHERE _id=?",
            argument = id,
        ).isNotEmpty()
    }

    private fun DatabaseWrapper.isBlocked(id: Long): Boolean {
        return participantColumn(id = id, column = ParticipantColumns.BLOCKED) == "1"
    }

    private fun DatabaseWrapper.senderOf(messageId: Long): Long {
        return queryLongs(
            sql = "SELECT ${MessageColumns.SENDER_PARTICIPANT_ID} " +
                "FROM ${DatabaseHelper.MESSAGES_TABLE} WHERE _id=?",
            argument = messageId,
        ).single()
    }

    private fun DatabaseWrapper.participantsOf(conversationId: Long): List<Long> {
        return queryLongs(
            sql = "SELECT ${ConversationParticipantsColumns.PARTICIPANT_ID} " +
                "FROM ${DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE} " +
                "WHERE ${ConversationParticipantsColumns.CONVERSATION_ID}=? " +
                "ORDER BY ${ConversationParticipantsColumns.PARTICIPANT_ID}",
            argument = conversationId,
        )
    }

    private fun DatabaseWrapper.participantCountOf(conversationId: Long): Int {
        return conversationColumn(
            conversationId = conversationId,
            column = ConversationColumns.PARTICIPANT_COUNT,
        )?.toInt() ?: -1
    }

    private fun DatabaseWrapper.participantColumn(id: Long, column: String): String? {
        return queryString(
            sql = "SELECT $column FROM ${DatabaseHelper.PARTICIPANTS_TABLE} WHERE _id=?",
            argument = id,
        )
    }

    private fun DatabaseWrapper.archiveStatusOf(conversationId: Long): String? {
        return conversationColumn(
            conversationId = conversationId,
            column = ConversationColumns.ARCHIVE_STATUS,
        )
    }

    private fun DatabaseWrapper.conversationColumn(conversationId: Long, column: String): String? {
        return queryString(
            sql = "SELECT $column FROM ${DatabaseHelper.CONVERSATIONS_TABLE} WHERE _id=?",
            argument = conversationId,
        )
    }

    private fun DatabaseWrapper.queryString(sql: String, argument: Long): String? {
        return rawQuery(sql, arrayOf(argument.toString())).use { cursor ->
            assertTrue("no row for $argument", cursor.moveToFirst())
            cursor.getString(0)
        }
    }

    private fun DatabaseWrapper.queryLongs(sql: String, argument: Long): List<Long> {
        return rawQuery(sql, arrayOf(argument.toString())).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getLong(0))
                }
            }
        }
    }

    private companion object {
        const val US_SUB_ID = 2
        const val FAROESE_SUB_ID = 3
        const val GREENLANDIC_SUB_ID = 6
        const val BARE_NUMBER = "54810027"
        const val CANONICAL_NUMBER = "+37254810027"
        const val LONE_BARE_NUMBER = "54810028"
        const val LONE_CANONICAL_NUMBER = "+37254810028"
        const val OTHER_NUMBER = "+15550100"
        const val CONTACT_NAME = "Repro Estonia"
        const val TIMEOUT_SECONDS = 10L

        val CANONICAL_DESTINATIONS = mapOf(
            BARE_NUMBER to CANONICAL_NUMBER,
            LONE_BARE_NUMBER to LONE_CANONICAL_NUMBER,
        )
    }
}
