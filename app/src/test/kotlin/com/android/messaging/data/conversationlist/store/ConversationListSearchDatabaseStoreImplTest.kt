package com.android.messaging.data.conversationlist.store

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversationlist.model.SearchableMessage
import com.android.messaging.data.conversationlist.model.SearchableParticipant
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class ConversationListSearchDatabaseStoreImplTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private lateinit var db: DatabaseWrapper
    private val store = ConversationListSearchDatabaseStoreImpl()

    @Before
    fun setUp() {
        installTestFactory(
            context = context,
            dataModel = mockk<DataModel>(relaxed = true) {
                every { database } answers { db }
            },
        )
        db = createInMemoryActionSyncTestDatabase(context = context)
    }

    @After
    fun tearDown() {
        db.database.close()
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun readParticipants_groupsEachConversationsParticipantsUnderIt() {
        val anna = db.insertParticipant(
            destination = ANNA_DESTINATION,
            fullName = "Anna Smith",
            firstName = "Anna",
        )
        val bob = db.insertParticipant(
            destination = BOB_DESTINATION,
            fullName = null,
            firstName = null,
        )
        val direct = db.insertConversation(participantIds = listOf(anna))
        val group = db.insertConversation(participantIds = listOf(anna, bob))

        val participants = store.readParticipants()

        assertEquals(
            mapOf(
                direct to listOf(annaParticipant()),
                group to listOf(annaParticipant(), bobParticipant()),
            ),
            participants,
        )
    }

    @Test
    fun readParticipants_participantInNoConversation_isLeftOut() {
        db.insertParticipant(
            destination = ANNA_DESTINATION,
            fullName = "Anna Smith",
            firstName = "Anna",
        )

        assertTrue(store.readParticipants().isEmpty())
    }

    @Test
    fun findMessages_matchesWordStartsIgnoringCaseAndAccentsNewestFirst() {
        val conversation = db.insertConversation(participantIds = emptyList())
        val older = db.insertMessage(
            conversationId = conversation,
            text = "Café tomorrow?",
            timestamp = 1L,
        )
        val newer = db.insertMessage(
            conversationId = conversation,
            text = "CAFETERIA lunch",
            timestamp = 2L,
        )
        db.insertMessage(conversationId = conversation, text = "decaf only", timestamp = 3L)

        assertEquals(listOf(newer, older), store.findMessages(query = "cafe").map { it.messageId })
    }

    @Test
    fun findMessages_cyrillicInAnotherCase_findsTheMessage() {
        val conversation = db.insertConversation(participantIds = emptyList())
        val message = db.insertMessage(conversationId = conversation, text = "Привет, как дела?")

        assertEquals(listOf(message), store.findMessages(query = "ПРИВ").map { it.messageId })
    }

    @Test
    fun findMessages_everyWordHasToMatch() {
        val conversation = db.insertConversation(participantIds = emptyList())
        val both = db.insertMessage(conversationId = conversation, text = "Lunch at noon")
        db.insertMessage(conversationId = conversation, text = "Lunch later")

        assertEquals(listOf(both), store.findMessages(query = "noon lunch").map { it.messageId })
    }

    @Test
    fun findMessages_queryWithTextIndexSyntax_matchesItsWordsAsTheyAre() {
        val conversation = db.insertConversation(participantIds = emptyList())
        val withEveryWord = db.insertMessage(conversationId = conversation, text = "Lunch or noon?")
        db.insertMessage(conversationId = conversation, text = "Lunch")

        val messages = store.findMessages(query = "\"lunch\" OR -noon* (")

        assertEquals(listOf(withEveryWord), messages.map { it.messageId })
    }

    @Test
    fun findMessages_queryWithoutLettersOrDigits_findsNothing() {
        val conversation = db.insertConversation(participantIds = emptyList())
        db.insertMessage(conversationId = conversation, text = "?!")

        assertTrue(store.findMessages(query = " ?! ").isEmpty())
    }

    @Test
    fun findMessages_leavesOutDrafts() {
        val conversation = db.insertConversation(participantIds = emptyList())
        db.insertMessage(
            conversationId = conversation,
            text = "Lunch draft",
            status = MessageData.BUGLE_STATUS_OUTGOING_DRAFT,
        )

        assertTrue(store.findMessages(query = "lunch").isEmpty())
    }

    @Test
    fun findMessages_namesTheSenderTheWayTheConversationListDoes() {
        val anna = db.insertParticipant(
            destination = ANNA_DESTINATION,
            fullName = "Anna Smith",
            firstName = "Anna",
        )
        val bob = db.insertParticipant(
            destination = BOB_DESTINATION,
            fullName = "Bob Jones",
            firstName = null,
        )
        val unknown = db.insertParticipant(
            destination = UNKNOWN_DESTINATION,
            fullName = null,
            firstName = null,
        )
        val conversation = db.insertConversation(participantIds = listOf(anna, bob, unknown))
        val fromAnna = db.insertMessage(
            conversationId = conversation,
            text = "lunch 1",
            senderId = anna,
        )
        db.insertMessage(
            conversationId = conversation,
            text = "lunch 2",
            senderId = bob,
            timestamp = 2L,
        )
        db.insertMessage(
            conversationId = conversation,
            text = "lunch 3",
            senderId = unknown,
            timestamp = 3L,
        )
        db.insertMessage(
            conversationId = conversation,
            text = "lunch 4",
            status = MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
            timestamp = 4L,
        )

        val messages = store.findMessages(query = "lunch")

        assertEquals(
            listOf(null, UNKNOWN_DESTINATION, "Bob Jones", "Anna"),
            messages.map { it.senderName },
        )
        assertEquals(listOf(false, true, true, true), messages.map { it.isIncoming })
        assertEquals(
            SearchableMessage(
                messageId = fromAnna,
                conversationId = conversation,
                text = "lunch 1",
                receivedTimestamp = 1L,
                isIncoming = true,
                senderName = "Anna",
            ),
            messages.last(),
        )
    }

    @Test
    fun findMessages_messageWithSeveralMatchingParts_isFoundOnce() {
        val conversation = db.insertConversation(participantIds = emptyList())
        val message = db.insertMessage(
            conversationId = conversation,
            text = "Lunch first",
            moreTexts = listOf("Lunch second"),
        )

        assertEquals(listOf(message), store.findMessages(query = "lunch").map { it.messageId })
    }

    @Test
    fun findMessages_withoutTheTextIndex_stillFindsTheMessagesByScanningTheirText() {
        val conversation = db.insertConversation(participantIds = emptyList())
        val message = db.insertMessage(conversationId = conversation, text = "See you at LUNCH")
        db.execSQL("DROP TRIGGER ${DatabaseHelper.PARTS_TEXT_INDEX_INSERT_TRIGGER}")

        assertEquals(listOf(message), store.findMessages(query = "lunch").map { it.messageId })
    }

    private fun annaParticipant(): SearchableParticipant {
        return SearchableParticipant(
            fullName = "Anna Smith",
            firstName = "Anna",
            displayDestination = ANNA_DESTINATION,
            normalizedDestination = ANNA_DESTINATION,
        )
    }

    private fun bobParticipant(): SearchableParticipant {
        return SearchableParticipant(
            fullName = null,
            firstName = null,
            displayDestination = BOB_DESTINATION,
            normalizedDestination = BOB_DESTINATION,
        )
    }

    private fun DatabaseWrapper.insertParticipant(
        destination: String,
        fullName: String?,
        firstName: String?,
    ): Long {
        return insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to ParticipantData.OTHER_THAN_SELF_SUB_ID,
                ParticipantColumns.NORMALIZED_DESTINATION to destination,
                ParticipantColumns.SEND_DESTINATION to destination,
                ParticipantColumns.DISPLAY_DESTINATION to destination,
                ParticipantColumns.FULL_NAME to fullName,
                ParticipantColumns.FIRST_NAME to firstName,
            ),
        )
    }

    private fun DatabaseWrapper.insertConversation(participantIds: List<Long>): ConversationId {
        val conversationId = insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.PARTICIPANT_COUNT to participantIds.size,
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

        return ConversationId(conversationId.toString())
    }

    private fun DatabaseWrapper.insertMessage(
        conversationId: ConversationId,
        text: String,
        timestamp: Long = 1L,
        status: Int = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
        senderId: Long? = null,
        moreTexts: List<String> = emptyList(),
    ): MessageId {
        val messageId = insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId.value,
                MessageColumns.RECEIVED_TIMESTAMP to timestamp,
                MessageColumns.STATUS to status,
                MessageColumns.SENDER_PARTICIPANT_ID to senderId,
            ),
        )

        (listOf(text) + moreTexts).forEach { partText ->
            insert(
                DatabaseHelper.PARTS_TABLE,
                null,
                contentValuesOf(
                    PartColumns.MESSAGE_ID to messageId,
                    PartColumns.CONVERSATION_ID to conversationId.value,
                    PartColumns.TEXT to partText,
                ),
            )
        }

        return MessageId(messageId.toString())
    }

    private companion object {
        private const val ANNA_DESTINATION = "+15550100"
        private const val BOB_DESTINATION = "+15550101"
        private const val UNKNOWN_DESTINATION = "+15550102"
    }
}
