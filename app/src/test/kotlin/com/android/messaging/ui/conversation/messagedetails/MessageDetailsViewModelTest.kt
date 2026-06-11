package com.android.messaging.ui.conversation.messagedetails

import android.content.ClipboardManager
import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.messagedetails.mapper.MessageDetailsUiStateMapper
import com.android.messaging.ui.conversation.messagedetails.model.MessageDetailsUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class MessageDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversationsRepository = mockk<ConversationsRepository>()
    private val messageDetailsUiStateMapper = mockk<MessageDetailsUiStateMapper>()
    private val clipboardManager = mockk<ClipboardManager>()

    @Test
    fun uiState_initialValue_isLoading() {
        val viewModel = createViewModel()

        assertEquals(MessageDetailsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun onArguments_loadsMessageAndDetailsAndExposesMappedState() = runTest {
        val message = mockk<ConversationMessageData>()
        val details = mockk<ConversationMessageDetails>()
        val content = mockk<MessageDetailsUiState.Content>()

        coEvery {
            conversationsRepository.getConversationMessage(
                conversationId = "c",
                messageId = "m",
            )
        } returns message

        coEvery {
            conversationsRepository.getMessageDetails(
                conversationId = "c",
                messageId = "m",
            )
        } returns details

        every {
            messageDetailsUiStateMapper.map(
                message = message,
                details = details,
            )
        } returns content

        val viewModel = createViewModel()
        viewModel.onArguments(
            conversationId = "c",
            messageId = "m",
        )
        advanceUntilIdle()

        assertEquals(content, viewModel.uiState.value)
        coVerify {
            conversationsRepository.getConversationMessage(
                conversationId = "c",
                messageId = "m",
            )
            conversationsRepository.getMessageDetails(
                conversationId = "c",
                messageId = "m",
            )
        }
        verify {
            messageDetailsUiStateMapper.map(
                message = message,
                details = details,
            )
        }
    }

    @Test
    fun onArguments_whenMapperReturnsUnavailable_exposesUnavailable() = runTest {
        coEvery {
            conversationsRepository.getConversationMessage(
                conversationId = "c",
                messageId = "m",
            )
        } returns null

        coEvery {
            conversationsRepository.getMessageDetails(
                conversationId = "c",
                messageId = "m",
            )
        } returns null

        every {
            messageDetailsUiStateMapper.map(
                message = null,
                details = null,
            )
        } returns MessageDetailsUiState.Unavailable

        val viewModel = createViewModel()
        viewModel.onArguments(
            conversationId = "c",
            messageId = "m",
        )
        advanceUntilIdle()

        assertEquals(MessageDetailsUiState.Unavailable, viewModel.uiState.value)
    }

    @Test
    fun onArguments_storesArgumentsInSavedStateHandle() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)

        viewModel.onArguments(
            conversationId = "c",
            messageId = "m",
        )

        assertEquals("c", savedStateHandle.get<String?>("conversation_id"))
        assertEquals("m", savedStateHandle.get<String?>("message_id"))
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): MessageDetailsViewModel {
        return MessageDetailsViewModel(
            conversationsRepository = conversationsRepository,
            messageDetailsUiStateMapper = messageDetailsUiStateMapper,
            clipboardManager = clipboardManager,
            savedStateHandle = savedStateHandle,
        )
    }
}
