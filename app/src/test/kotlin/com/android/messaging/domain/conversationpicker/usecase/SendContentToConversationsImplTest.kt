package com.android.messaging.domain.conversationpicker.usecase

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.exception.BlankConversationIdException
import com.android.messaging.domain.conversationpicker.model.SendContentResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class SendContentToConversationsImplTest {

    private val sendConversationDraft = mockk<SendConversationDraft>()
    private val testDispatcher = StandardTestDispatcher()

    private val sentConversationIds = mutableListOf<ConversationId>()

    private val sendContentToConversations = SendContentToConversationsImpl(
        sendConversationDraft = sendConversationDraft,
        ioDispatcher = testDispatcher,
    )

    @Test
    fun invoke_allSendsSucceed_returnsSuccess() = runTest(testDispatcher) {
        recordSends { flowOf(Unit) }

        val result = sendContentToConversations(
            draft = DRAFT,
            conversationIds = linkedSetOf(ConversationId("a"), ConversationId("b")),
        )

        assertEquals(SendContentResult.Success, result)
        assertEquals(listOf(ConversationId("a"), ConversationId("b")), sentConversationIds)
    }

    @Test
    fun invoke_firstSendFails_stillAttemptsRestAndReturnsFailure() = runTest(testDispatcher) {
        recordSends { conversationId ->
            when (conversationId) {
                ConversationId("a") -> flow { throw BlankConversationIdException() }
                else -> flowOf(Unit)
            }
        }

        val result = sendContentToConversations(
            draft = DRAFT,
            conversationIds = linkedSetOf(ConversationId("a"), ConversationId("b")),
        )

        assertEquals(SendContentResult.Failure, result)
        assertEquals(listOf(ConversationId("a"), ConversationId("b")), sentConversationIds)
    }

    @Test
    fun invoke_emptyConversationIds_returnsSuccessWithoutSending() = runTest(testDispatcher) {
        recordSends { flowOf(Unit) }

        val result = sendContentToConversations(DRAFT, emptySet())

        assertEquals(SendContentResult.Success, result)
        assertTrue(sentConversationIds.isEmpty())
    }

    private fun recordSends(response: (conversationId: ConversationId) -> Flow<Unit>) {
        every { sendConversationDraft(any(), any()) } answers {
            val conversationId = ConversationId(firstArg<String>())
            sentConversationIds += conversationId
            response(conversationId)
        }
    }

    private companion object {
        private val DRAFT = ConversationDraft(
            messageText = "hello",
            subjectText = "",
            attachments = persistentListOf(),
        )
    }
}
