package com.android.messaging.data.conversationlist.store

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.SearchableParticipant
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
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

    private companion object {
        private const val ANNA_DESTINATION = "+15550100"
        private const val BOB_DESTINATION = "+15550101"
    }
}
