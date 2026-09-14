package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessagesDelegateAudioDurationTest :
    BaseConversationMessagesDelegateTest() {

    @Test
    fun bind_withAudioPart_seedsTheClipLengthBeforeItIsPlayed() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val audio = audioPart(contentUri = AUDIO_CONTENT_URI)
            val message = messageUiModel(messageId = "m1", parts = listOf(audio))
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            givenAudioDuration(contentUri = AUDIO_CONTENT_URI, durationMillis = DURATION_MILLIS)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        message.copy(
                            parts = persistentListOf(
                                audio.copy(durationMillis = DURATION_MILLIS),
                            ),
                        ),
                    ),
                ),
                delegate.state.value,
            )
        }
    }

    @Test
    fun bind_withDuplicateAudioContentUris_resolvesUriOnceAndUpdatesBothParts() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val firstAudio = audioPart(contentUri = AUDIO_CONTENT_URI)
            val secondAudio = audioPart(contentUri = AUDIO_CONTENT_URI)
            val first = messageUiModel(messageId = "first", parts = listOf(firstAudio))
            val second = messageUiModel(messageId = "second", parts = listOf(secondAudio))
            givenConversationMessages(messages = flowOf(messagesOf(first, second)))
            givenAudioDuration(contentUri = AUDIO_CONTENT_URI, durationMillis = DURATION_MILLIS)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        first.copy(
                            parts = persistentListOf(
                                firstAudio.copy(durationMillis = DURATION_MILLIS),
                            ),
                        ),
                        second.copy(
                            parts = persistentListOf(
                                secondAudio.copy(durationMillis = DURATION_MILLIS),
                            ),
                        ),
                    ),
                ),
                delegate.state.value,
            )
            coVerify(exactly = 1) {
                resolveAudioDurationMillis(contentUri = AUDIO_CONTENT_URI)
            }
        }
    }

    @Test
    fun bind_withoutAudioParts_skipsTheDurationLookup() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val message = messageUiModel(messageId = "m1", parts = listOf(textPart()))
            givenConversationMessages(messages = flowOf(messagesOf(message)))

            createBoundDelegate(conversationIdFlow = MutableStateFlow(CONVERSATION_ID))
            runCurrent()

            coVerify(exactly = 0) {
                resolveAudioDurationMillis(contentUri = any())
            }
        }
    }

    @Test
    fun bind_withAudioPartWithoutContentUri_leavesTheDurationUnknown() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val audio = audioPart(contentUri = null)
            val message = messageUiModel(messageId = "m1", parts = listOf(audio))
            givenConversationMessages(messages = flowOf(messagesOf(message)))

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(message)),
                delegate.state.value,
            )
            coVerify(exactly = 0) {
                resolveAudioDurationMillis(contentUri = any())
            }
        }
    }

    private fun givenAudioDuration(
        contentUri: String,
        durationMillis: Long,
    ) {
        coEvery {
            resolveAudioDurationMillis(contentUri = contentUri)
        } returns durationMillis
    }

    private companion object {
        private const val AUDIO_CONTENT_URI = "content://mms/part/audio-1"
        private const val DURATION_MILLIS = 18_000L
    }
}
