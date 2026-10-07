package com.android.messaging.ui.conversation.messagedetails

import android.content.ClipboardManager
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.data.conversation.model.message.ConversationMessageDetailsResult
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.domain.media.usecase.ResolveAudioDurationMillis
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.messagedetails.mapper.MessageDetailsUiStateMapper
import com.android.messaging.ui.conversation.messagedetails.model.MessageDetailsUiState
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class MessageDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversationsRepository = mockk<ConversationsRepository>()
    private val appSettingsRepository = mockk<AppSettingsRepository> {
        coEvery { isYouTubeLinkPreviewsEnabled() } returns false
    }
    private val messageDetailsUiStateMapper = mockk<MessageDetailsUiStateMapper>()
    private val resolveAudioDurationMillis = mockk<ResolveAudioDurationMillis>()
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
        val content = MessageDetailsUiState.Content(
            preview = messageUiModel(),
            details = details,
            youTubeLinkPreviewsEnabled = false,
        )

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
    fun uiState_resolvesTheLengthOfAnAudioClipInThePreview() = runTest {
        val message = mockk<ConversationMessageData>()
        val details = mockk<ConversationMessageDetails>()
        val audio = ConversationMessagePartUiModel.Attachment.Audio(
            text = null,
            contentType = "audio/mp4",
            contentUri = Uri.parse(AUDIO_CONTENT_URI),
            width = 0,
            height = 0,
        )

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
        } returns MessageDetailsUiState.Content(
            preview = messageUiModel(parts = persistentListOf(audio)),
            details = details,
            youTubeLinkPreviewsEnabled = false,
        )
        coEvery { resolveAudioDurationMillis(contentUri = AUDIO_CONTENT_URI) } returns 63_000L

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            MessageDetailsUiState.Content(
                preview = messageUiModel(
                    parts = persistentListOf(audio.copy(durationMillis = 63_000L)),
                ),
                details = details,
                youTubeLinkPreviewsEnabled = false,
            ),
            viewModel.uiState.value,
        )
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
            resolveAudioDurationMillis = resolveAudioDurationMillis,
            clipboardManager = clipboardManager,
            savedStateHandle = savedStateHandle,
        )
    }

    private fun messageUiModel(
        parts: ImmutableList<ConversationMessagePartUiModel> = persistentListOf(),
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = MessageId("m"),
            conversationId = ConversationId("c"),
            text = null,
            parts = parts,
            sentTimestamp = 1L,
            receivedTimestamp = 1L,
            displayTimestamp = 1L,
            status = ConversationMessageUiModel.Status.Outgoing.Complete,
            isIncoming = false,
            senderDisplayName = null,
            senderAvatarUri = null,
            senderContactId = 0L,
            senderContactLookupKey = null,
            senderNormalizedDestination = null,
            senderParticipantId = null,
            selfParticipantId = null,
            canClusterWithPrevious = false,
            canClusterWithNext = false,
            canCopyMessageToClipboard = false,
            canDownloadMessage = false,
            canForwardMessage = false,
            canResendMessage = false,
            canSaveAttachments = false,
            mmsDownload = null,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.MMS,
        )
    }

    private companion object {
        private const val AUDIO_CONTENT_URI = "content://mms/part/1"
    }
}
