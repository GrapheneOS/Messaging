package com.android.messaging.data.conversation.repository.conversations

import com.android.messaging.datamodel.action.DeleteConversationAction
import com.android.messaging.datamodel.action.DeleteMessageAction
import com.android.messaging.datamodel.action.RedownloadMmsAction
import com.android.messaging.datamodel.action.ResendMessageAction
import com.android.messaging.datamodel.action.UpdateConversationArchiveStatusAction
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationsRepositoryActionsTest : BaseConversationsRepositoryTest() {

    @Before
    fun setUpActions() {
        mockkStatic(DeleteMessageAction::class)
        mockkStatic(RedownloadMmsAction::class)
        mockkStatic(ResendMessageAction::class)
        mockkStatic(UpdateConversationArchiveStatusAction::class)
        mockkStatic(DeleteConversationAction::class)
        every { DeleteMessageAction.deleteMessage(any()) } just runs
        every { RedownloadMmsAction.redownloadMessage(any()) } just runs
        every { ResendMessageAction.resendMessage(any()) } just runs
        every { UpdateConversationArchiveStatusAction.archiveConversation(any()) } just runs
        every { UpdateConversationArchiveStatusAction.unarchiveConversation(any()) } just runs
        every { DeleteConversationAction.deleteConversation(any(), any()) } just runs
    }

    @After
    fun tearDownActions() {
        unmockkAll()
    }

    @Test
    fun deleteMessages_skipsBlankIdsAndDelegatesNonBlankIds() {
        createRepository().deleteMessages(
            messageIds = listOf("message-1", "", " ", "message-2"),
        )

        verify(exactly = 1) {
            DeleteMessageAction.deleteMessage("message-1")
        }
        verify(exactly = 1) {
            DeleteMessageAction.deleteMessage("message-2")
        }
        verify(exactly = 0) {
            DeleteMessageAction.deleteMessage("")
        }
        verify(exactly = 0) {
            DeleteMessageAction.deleteMessage(" ")
        }
    }

    @Test
    fun messageActions_skipBlankIdsAndDelegateNonBlankIds() {
        val repository = createRepository()

        repository.downloadMessage(messageId = "")
        repository.downloadMessage(messageId = "message-download")
        repository.resendMessage(messageId = " ")
        repository.resendMessage(messageId = "message-resend")

        verify(exactly = 0) {
            RedownloadMmsAction.redownloadMessage("")
        }
        verify(exactly = 1) {
            RedownloadMmsAction.redownloadMessage("message-download")
        }
        verify(exactly = 0) {
            ResendMessageAction.resendMessage(" ")
        }
        verify(exactly = 1) {
            ResendMessageAction.resendMessage("message-resend")
        }
    }

    @Test
    fun archiveActions_skipBlankIdsAndDelegateNonBlankIds() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repository = createRepository()

            repository.archiveConversation(conversationId = "")
            repository.archiveConversation(conversationId = "conversation-archive")
            repository.unarchiveConversation(conversationId = " ")
            repository.unarchiveConversation(conversationId = "conversation-unarchive")

            io.mockk.coVerify(exactly = 0) { conversationArchiveStore.archiveConversation("") }
            io.mockk.coVerify(exactly = 1) {
                conversationArchiveStore.archiveConversation("conversation-archive")
            }
            io.mockk.coVerify(exactly = 0) { conversationArchiveStore.unarchiveConversation(" ") }
            io.mockk.coVerify(exactly = 1) {
                conversationArchiveStore.unarchiveConversation("conversation-unarchive")
            }
        }
    }

    @Test
    fun deleteConversation_skipsBlankIdAndDelegatesNonBlankIdWithCutoff() {
        val repository = createRepository()

        repository.deleteConversation(conversationId = "", cutoffTimestamp = 123L)
        repository.deleteConversation(
            conversationId = "conversation-delete",
            cutoffTimestamp = 456L,
        )

        verify(exactly = 0) {
            DeleteConversationAction.deleteConversation("", 123L)
        }
        verify(exactly = 1) {
            DeleteConversationAction.deleteConversation("conversation-delete", 456L)
        }
    }
}
