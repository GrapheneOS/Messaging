package com.android.messaging.data.conversation.store

import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConversationDraftStoreTest {

    private val databaseWrapper = mockk<DatabaseWrapper>()
    private val dataModel = mockk<DataModel>()

    private val store = ConversationDraftStoreImpl()

    @Before
    fun setUp() {
        mockkStatic(DataModel::class)
        mockkStatic(ConversationListItemData::class)

        every { DataModel.get() } returns dataModel
        every { dataModel.database } returns databaseWrapper
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getSelfParticipantId_whenConversationIsMissing_returnsNull() {
        every {
            ConversationListItemData.getExistingConversation(databaseWrapper, CONVERSATION_ID)
        } returns null

        val selfParticipantId = store.getSelfParticipantId(conversationId = CONVERSATION_ID)

        assertNull(selfParticipantId)
    }

    @Test
    fun getSelfParticipantId_whenSelfIdIsBlank_returnsNull() {
        val conversation = mockk<ConversationListItemData>()
        every { conversation.selfId } returns "  "
        every {
            ConversationListItemData.getExistingConversation(databaseWrapper, CONVERSATION_ID)
        } returns conversation

        val selfParticipantId = store.getSelfParticipantId(conversationId = CONVERSATION_ID)

        assertNull(selfParticipantId)
    }

    @Test
    fun getSelfParticipantId_whenSelfIdIsPresent_returnsSelfId() {
        val conversation = mockk<ConversationListItemData>()
        every { conversation.selfId } returns SELF_PARTICIPANT_ID
        every {
            ConversationListItemData.getExistingConversation(databaseWrapper, CONVERSATION_ID)
        } returns conversation

        val selfParticipantId = store.getSelfParticipantId(conversationId = CONVERSATION_ID)

        assertEquals(SELF_PARTICIPANT_ID, selfParticipantId)
    }

    private companion object {
        private const val SELF_PARTICIPANT_ID = "self-1"
    }
}
