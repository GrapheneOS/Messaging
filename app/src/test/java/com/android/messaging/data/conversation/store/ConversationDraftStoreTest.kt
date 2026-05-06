package com.android.messaging.data.conversation.store

import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.data.ConversationListItemData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

internal class ConversationDraftStoreTest {

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
    fun getSelfParticipantIdReturnsNullWhenConversationSelfIdIsMissing() {
        val conversation = ConversationListItemData()
        every {
            ConversationListItemData.getExistingConversation(databaseWrapper, CONVERSATION_ID)
        } returns conversation

        val selfParticipantId = store.getSelfParticipantId(
            conversationId = CONVERSATION_ID,
        )

        assertNull(selfParticipantId)
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-id"
    }
}
