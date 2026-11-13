package com.android.messaging.domain.conversation.usecase.action

import app.cash.turbine.test
import com.android.messaging.data.conversation.event.ConversationArchiveEvents
import com.android.messaging.data.conversation.event.ConversationArchiveEventsImpl
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UnarchiveConversationImplTest {

    private val conversationsRepository = mockk<ConversationsRepository>(relaxed = true)

    @Test
    fun invoke_notifiesArchivedListAndUnarchives() {
        runTest {
            val conversationArchiveEvents = ConversationArchiveEventsImpl()
            val unarchiveConversation = UnarchiveConversationImpl(
                conversationsRepository = conversationsRepository,
                conversationArchiveEvents = conversationArchiveEvents,
            )

            conversationArchiveEvents.unarchivedConversationIds.test {
                unarchiveConversation(conversationId = CONVERSATION_ID)

                assertEquals(CONVERSATION_ID, awaitItem())
            }
            coVerify(exactly = 1) {
                conversationsRepository.unarchiveConversation(conversationId = CONVERSATION_ID)
            }
        }
    }

    @Test
    fun invoke_notifiesBeforeTheWrite() {
        runTest {
            val conversationArchiveEvents = spyk<ConversationArchiveEvents>(
                ConversationArchiveEventsImpl(),
            )
            val unarchiveConversation = UnarchiveConversationImpl(
                conversationsRepository = conversationsRepository,
                conversationArchiveEvents = conversationArchiveEvents,
            )

            unarchiveConversation(conversationId = CONVERSATION_ID)

            coVerifyOrder {
                conversationArchiveEvents.notifyUnarchived(conversationId = CONVERSATION_ID)
                conversationsRepository.unarchiveConversation(conversationId = CONVERSATION_ID)
            }
        }
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conversation-42")
    }
}
