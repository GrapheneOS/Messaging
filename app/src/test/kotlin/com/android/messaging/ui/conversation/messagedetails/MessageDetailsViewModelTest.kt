package com.android.messaging.ui.conversation.messagedetails

import android.content.ClipboardManager
import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.data.conversation.model.message.ConversationMessageDetailsResult
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
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class MessageDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversationsRepository = mockk<ConversationsRepository>()
    private val appSettingsRepository = mockk<AppSettingsRepository> {
        coEvery { isYouTubeLinkPreviewsEnabled() } returns false
    }
    private val messageDetailsUiStateMapper = mockk<MessageDetailsUiStateMapper>()
    private val clipboardManager = mockk<ClipboardManager>()

    @Test
    fun uiState_initialValue_isLoading() {
        val viewModel = createViewModel()

        assertEquals(MessageDetailsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun uiState_loadsMessageDetailsFromSeededIdsAndExposesMappedState() = runTest {
        val message = mockk<ConversationMessageData>()
        val details = mockk<ConversationMessageDetails>()
        val content = mockk<MessageDetailsUiState.Content>()

        coEvery {
            conversationsRepository.getMessageDetails(
                conversationId = ConversationId("c"),
                messageId = MessageId("m"),
            )
        } returns ConversationMessageDetailsResult(
            message = message,
            details = details,
        )

        every {
            messageDetailsUiStateMapper.map(
                message = message,
                details = details,
                youTubeLinkPreviewsEnabled = false,
            )
        } returns content

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(content, viewModel.uiState.value)
        coVerify {
            conversationsRepository.getMessageDetails(
                conversationId = ConversationId("c"),
                messageId = MessageId("m"),
            )
        }
        verify {
            messageDetailsUiStateMapper.map(
                message = message,
                details = details,
                youTubeLinkPreviewsEnabled = false,
            )
        }
    }

    @Test
    fun uiState_whenMapperReturnsUnavailable_exposesUnavailable() = runTest {
        coEvery {
            conversationsRepository.getMessageDetails(
                conversationId = ConversationId("c"),
                messageId = MessageId("m"),
            )
        } returns null

        every {
            messageDetailsUiStateMapper.map(
                message = null,
                details = null,
                youTubeLinkPreviewsEnabled = false,
            )
        } returns MessageDetailsUiState.Unavailable

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(MessageDetailsUiState.Unavailable, viewModel.uiState.value)
    }

    @Test
    fun construction_whenConversationIdMissing_throws() {
        val savedStateHandle = SavedStateHandle(
            mapOf(MESSAGE_DETAILS_MESSAGE_ID_ARG to "m"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            createViewModel(savedStateHandle)
        }
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            mapOf(
                MESSAGE_DETAILS_CONVERSATION_ID_ARG to "c",
                MESSAGE_DETAILS_MESSAGE_ID_ARG to "m",
            ),
        ),
    ): MessageDetailsViewModel {
        return MessageDetailsViewModel(
            conversationsRepository = conversationsRepository,
            appSettingsRepository = appSettingsRepository,
            messageDetailsUiStateMapper = messageDetailsUiStateMapper,
            clipboardManager = clipboardManager,
            savedStateHandle = savedStateHandle,
        )
    }
}
