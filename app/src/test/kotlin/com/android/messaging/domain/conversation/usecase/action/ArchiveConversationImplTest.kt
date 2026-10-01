package com.android.messaging.domain.conversation.usecase.action

import app.cash.turbine.test
import com.android.messaging.data.conversation.event.ConversationArchiveEventsImpl
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveConversationImplTest {

    @Test
    fun invoke_notifiesConversationListAndArchives() {
        runTest {
            val conversationsRepository = mockk<ConversationsRepository>(relaxed = true)
            val conversationArchiveEvents = ConversationArchiveEventsImpl()
            val archiveConversation = ArchiveConversationImpl(
                conversationsRepository = conversationsRepository,
                conversationArchiveEvents = conversationArchiveEvents,
            )

            conversationArchiveEvents.archivedConversationIds.test {
                archiveConversation(conversationId = CONVERSATION_ID)

                assertEquals(CONVERSATION_ID, awaitItem())
            }
            coVerify(exactly = 1) {
                conversationsRepository.archiveConversation(conversationId = CONVERSATION_ID)
            }
        }
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conversation-42")
    }
}
