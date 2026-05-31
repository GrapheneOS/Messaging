package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
internal class ConversationMessagesDelegateVCardTest : BaseConversationMessagesDelegateTest() {

    @Test
    fun bind_withVCardPart_observesMetadataAndAppliesMappedUiModel() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val contentUri = "content://mms/part/vcard-1"
            val vCard = vCardPart(contentUri = contentUri, vCardUiModel = vCardUiModel("original"))
            val message = messageUiModel(messageId = "m1", parts = listOf(vCard))
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            val metadata = ConversationVCardAttachmentMetadata.Loading
            givenVCardMetadata(contentUri = contentUri, metadata = flowOf(metadata))
            val mapped = vCardUiModel("mapped")
            givenVCardUiModel(metadata = metadata, uiModel = mapped)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        message.copy(parts = persistentListOf(vCard.copy(vCardUiModel = mapped))),
                    ),
                ),
                delegate.state.value,
            )
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                vCardMetadataRepository.observeAttachmentMetadata(contentUri = contentUri)
            }
        }
    }

    @Test
    fun bind_withDuplicateVCardContentUris_observesUriOnceAndUpdatesBothParts() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val contentUri = "content://mms/part/shared-vcard"
            val firstVCard = vCardPart(contentUri = contentUri, vCardUiModel = vCardUiModel("a"))
            val secondVCard = vCardPart(contentUri = contentUri, vCardUiModel = vCardUiModel("b"))
            val first = messageUiModel(messageId = "first", parts = listOf(firstVCard))
            val second = messageUiModel(messageId = "second", parts = listOf(secondVCard))
            givenConversationMessages(messages = flowOf(messagesOf(first, second)))
            val metadata = ConversationVCardAttachmentMetadata.Loading
            givenVCardMetadata(contentUri = contentUri, metadata = flowOf(metadata))
            val mapped = vCardUiModel("mapped")
            givenVCardUiModel(metadata = metadata, uiModel = mapped)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        first.copy(
                            parts = persistentListOf(firstVCard.copy(vCardUiModel = mapped))
                        ),
                        second.copy(
                            parts = persistentListOf(secondVCard.copy(vCardUiModel = mapped))
                        ),
                    ),
                ),
                delegate.state.value,
            )
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                vCardMetadataRepository.observeAttachmentMetadata(contentUri = contentUri)
            }
        }
    }

    @Test
    fun bind_withMultipleDistinctVCardUris_observesEachAndMapsRespectively() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val firstUri = "content://mms/part/vcard-1"
            val secondUri = "content://mms/part/vcard-2"
            val firstVCard = vCardPart(contentUri = firstUri, vCardUiModel = vCardUiModel("a"))
            val secondVCard = vCardPart(contentUri = secondUri, vCardUiModel = vCardUiModel("b"))
            val message = messageUiModel(messageId = "m1", parts = listOf(firstVCard, secondVCard))
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            val firstMetadata = ConversationVCardAttachmentMetadata.Loading
            val secondMetadata = ConversationVCardAttachmentMetadata.Failed
            givenVCardMetadata(contentUri = firstUri, metadata = flowOf(firstMetadata))
            givenVCardMetadata(contentUri = secondUri, metadata = flowOf(secondMetadata))
            val firstMapped = vCardUiModel("mapped-1")
            val secondMapped = vCardUiModel("mapped-2")
            givenVCardUiModel(metadata = firstMetadata, uiModel = firstMapped)
            givenVCardUiModel(metadata = secondMetadata, uiModel = secondMapped)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        message.copy(
                            parts = persistentListOf(
                                firstVCard.copy(vCardUiModel = firstMapped),
                                secondVCard.copy(vCardUiModel = secondMapped),
                            ),
                        ),
                    ),
                ),
                delegate.state.value,
            )
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                vCardMetadataRepository.observeAttachmentMetadata(contentUri = firstUri)
            }
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                vCardMetadataRepository.observeAttachmentMetadata(contentUri = secondUri)
            }
        }
    }

    @Test
    fun bind_withNullContentUriVCardAlongsidePresentOne_mapsNullMetadataForNullUriPart() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val contentUri = "content://mms/part/vcard-present"
            val presentVCard = vCardPart(
                contentUri = contentUri,
                vCardUiModel = vCardUiModel("present"),
            )
            val nullUriVCard = vCardPart(
                contentUri = null,
                vCardUiModel = vCardUiModel("null-original"),
            )
            val message = messageUiModel(
                messageId = "m1",
                parts = listOf(presentVCard, nullUriVCard),
            )
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            val metadata = ConversationVCardAttachmentMetadata.Loading
            givenVCardMetadata(contentUri = contentUri, metadata = flowOf(metadata))
            val presentMapped = vCardUiModel("present-mapped")
            val nullMapped = vCardUiModel("null-mapped")
            givenVCardUiModel(metadata = metadata, uiModel = presentMapped)
            givenVCardUiModel(metadata = null, uiModel = nullMapped)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        message.copy(
                            parts = persistentListOf(
                                presentVCard.copy(vCardUiModel = presentMapped),
                                nullUriVCard.copy(vCardUiModel = nullMapped),
                            ),
                        ),
                    ),
                ),
                delegate.state.value,
            )
        }
    }

    @Test
    fun bind_withOnlyNullContentUriVCardParts_leavesMessagesUnmodified() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val nullUriVCard = vCardPart(contentUri = null, vCardUiModel = vCardUiModel("original"))
            val message = messageUiModel(messageId = "m1", parts = listOf(nullUriVCard))
            givenConversationMessages(messages = flowOf(messagesOf(message)))

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(message)),
                delegate.state.value,
            )
            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                vCardMetadataRepository.observeAttachmentMetadata(contentUri = any())
            }
            verify(exactly = 0) {
                vCardUiModelMapper.map(metadata = any())
            }
        }
    }

    @Test
    fun bind_withVCardAndNonVCardParts_leavesNonVCardPartsUnchanged() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val contentUri = "content://mms/part/vcard-mixed"
            val text = textPart(text = "caption")
            val image = imagePart()
            val audio = audioPart()
            val file = filePart()
            val video = videoPart()
            val vCard = vCardPart(contentUri = contentUri, vCardUiModel = vCardUiModel("original"))
            val message = messageUiModel(
                messageId = "m1",
                parts = listOf(text, image, audio, file, video, vCard),
            )
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            val metadata = ConversationVCardAttachmentMetadata.Loading
            givenVCardMetadata(contentUri = contentUri, metadata = flowOf(metadata))
            val mapped = vCardUiModel("mapped")
            givenVCardUiModel(metadata = metadata, uiModel = mapped)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        message.copy(
                            parts = persistentListOf(
                                text,
                                image,
                                audio,
                                file,
                                video,
                                vCard.copy(vCardUiModel = mapped),
                            ),
                        ),
                    ),
                ),
                delegate.state.value,
            )
        }
    }

    @Test
    fun bind_whenVCardMetadataEmitsNewValue_updatesPresentState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val contentUri = "content://mms/part/vcard-live"
            val vCard = vCardPart(contentUri = contentUri, vCardUiModel = vCardUiModel("original"))
            val message = messageUiModel(messageId = "m1", parts = listOf(vCard))
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            val loadingMetadata = ConversationVCardAttachmentMetadata.Loading
            val failedMetadata = ConversationVCardAttachmentMetadata.Failed
            val metadataFlow = MutableStateFlow<ConversationVCardAttachmentMetadata>(
                loadingMetadata,
            )
            givenVCardMetadata(contentUri = contentUri, metadata = metadataFlow)
            val loadingModel = vCardUiModel("loading-model")
            val failedModel = vCardUiModel("failed-model")
            givenVCardUiModel(metadata = loadingMetadata, uiModel = loadingModel)
            givenVCardUiModel(metadata = failedMetadata, uiModel = failedModel)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()
            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        message.copy(
                            parts = persistentListOf(vCard.copy(vCardUiModel = loadingModel))
                        ),
                    ),
                ),
                delegate.state.value,
            )

            metadataFlow.value = failedMetadata
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        message.copy(
                            parts = persistentListOf(vCard.copy(vCardUiModel = failedModel))
                        ),
                    ),
                ),
                delegate.state.value,
            )
        }
    }

    @Test
    fun bind_withMultipleDistinctVCardUris_staysLoadingUntilEveryMetadataFlowEmits() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val firstUri = "content://mms/part/vcard-1"
            val secondUri = "content://mms/part/vcard-2"
            val firstVCard = vCardPart(contentUri = firstUri, vCardUiModel = vCardUiModel("a"))
            val secondVCard = vCardPart(contentUri = secondUri, vCardUiModel = vCardUiModel("b"))
            val message = messageUiModel(messageId = "m1", parts = listOf(firstVCard, secondVCard))
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            val firstMetadata = ConversationVCardAttachmentMetadata.Loading
            val secondMetadata = ConversationVCardAttachmentMetadata.Failed
            val pendingSecondMetadata = MutableSharedFlow<ConversationVCardAttachmentMetadata>(
                replay = 1,
            )
            givenVCardMetadata(contentUri = firstUri, metadata = flowOf(firstMetadata))
            givenVCardMetadata(contentUri = secondUri, metadata = pendingSecondMetadata)
            val firstMapped = vCardUiModel("mapped-1")
            val secondMapped = vCardUiModel("mapped-2")
            givenVCardUiModel(metadata = firstMetadata, uiModel = firstMapped)
            givenVCardUiModel(metadata = secondMetadata, uiModel = secondMapped)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()
            assertEquals(ConversationMessagesUiState.Loading, delegate.state.value)

            pendingSecondMetadata.tryEmit(secondMetadata)
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        message.copy(
                            parts = persistentListOf(
                                firstVCard.copy(vCardUiModel = firstMapped),
                                secondVCard.copy(vCardUiModel = secondMapped),
                            ),
                        ),
                    ),
                ),
                delegate.state.value,
            )
        }
    }

    @Test
    fun bind_whenMessageListChangesWhileVCardMetadataPending_cancelsStaleMetadataSubscription() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val staleUri = "content://mms/part/vcard-stale"
            val staleVCard = vCardPart(contentUri = staleUri, vCardUiModel = vCardUiModel("stale"))
            val staleMessage = messageUiModel(messageId = "stale", parts = listOf(staleVCard))
            val replacementMessage = messageUiModel(
                messageId = "replacement",
                parts = listOf(textPart()),
            )
            val messagesFlow = MutableStateFlow(messagesOf(staleMessage))
            givenConversationMessages(messages = messagesFlow)
            val staleMetadata = ConversationVCardAttachmentMetadata.Loading
            val pendingStaleMetadata = MutableSharedFlow<ConversationVCardAttachmentMetadata>(
                replay = 1,
            )
            givenVCardMetadata(contentUri = staleUri, metadata = pendingStaleMetadata)
            givenVCardUiModel(metadata = staleMetadata, uiModel = vCardUiModel("stale-mapped"))

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()
            assertEquals(ConversationMessagesUiState.Loading, delegate.state.value)

            messagesFlow.value = messagesOf(replacementMessage)
            runCurrent()
            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(replacementMessage)),
                delegate.state.value,
            )

            pendingStaleMetadata.tryEmit(staleMetadata)
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(replacementMessage)),
                delegate.state.value,
            )
        }
    }

    @Test
    fun bind_withMixedVCardAndPlainMessages_enrichesOnlyVCardMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val contentUri = "content://mms/part/vcard-mixed-messages"
            val vCard = vCardPart(contentUri = contentUri, vCardUiModel = vCardUiModel("original"))
            val vCardMessage = messageUiModel(messageId = "with-vcard", parts = listOf(vCard))
            val plainMessage = messageUiModel(
                messageId = "plain",
                parts = listOf(textPart(), imagePart()),
            )
            givenConversationMessages(messages = flowOf(messagesOf(vCardMessage, plainMessage)))
            val metadata = ConversationVCardAttachmentMetadata.Loading
            givenVCardMetadata(contentUri = contentUri, metadata = flowOf(metadata))
            val mapped = vCardUiModel("mapped")
            givenVCardUiModel(metadata = metadata, uiModel = mapped)

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    persistentListOf(
                        vCardMessage.copy(
                            parts = persistentListOf(vCard.copy(vCardUiModel = mapped))
                        ),
                        plainMessage,
                    ),
                ),
                delegate.state.value,
            )
        }
    }
}
