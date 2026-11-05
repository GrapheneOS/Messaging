package com.android.messaging.domain.conversation.usecase.action

import app.cash.turbine.test
import com.android.messaging.data.conversation.event.ConversationArchiveEventsImpl
import com.android.messaging.data.conversation.model.ArchivedConversation
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveConversationImplTest {

    private val conversationsRepository = mockk<ConversationsRepository>(relaxed = true)
    private val conversationArchiveEvents = ConversationArchiveEventsImpl()
    private val archiveConversation = ArchiveConversationImpl(
        conversationsRepository = conversationsRepository,
        conversationArchiveEvents = conversationArchiveEvents,
    )

    @Test
    fun invoke_notifiesConversationListAndArchives() {
        runTest {
            conversationArchiveEvents.archivedConversations.test {
                archiveConversation(conversationId = CONVERSATION_ID)

                assertEquals(
                    ArchivedConversation(conversationId = CONVERSATION_ID, wasPinned = false),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                conversationsRepository.archiveConversation(conversationId = CONVERSATION_ID)
            }
        }
    }

    @Test
    fun invoke_readsThePinBeforeArchivingClearsIt() {
        runTest {
            coEvery {
                conversationsRepository.isConversationPinned(conversationId = CONVERSATION_ID)
            } returns true

            conversationArchiveEvents.archivedConversations.test {
                archiveConversation(conversationId = CONVERSATION_ID)

                assertEquals(
                    ArchivedConversation(conversationId = CONVERSATION_ID, wasPinned = true),
                    awaitItem(),
                )
            }
            coVerifyOrder {
                conversationsRepository.isConversationPinned(conversationId = CONVERSATION_ID)
                conversationsRepository.archiveConversation(conversationId = CONVERSATION_ID)
            }
        }
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conversation-42")
    }
}
