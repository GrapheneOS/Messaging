package com.android.messaging.ui.conversation.messages.delegate

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.repository.ConversationVCardMetadataRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.media.usecase.ResolveAudioDurationMillis
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


internal interface ConversationMessagesDelegate :
    ConversationScreenDelegate<ConversationMessagesUiState> {
    fun refresh()

    fun loadOlderMessages()
}

internal class ConversationMessagesDelegateImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val resolveAudioDurationMillis: ResolveAudioDurationMillis,
    private val conversationMessageUiModelMapper: ConversationMessageUiModelMapper,
    private val conversationVCardAttachmentUiModelMapper: ConversationVCardAttachmentUiModelMapper,
    private val conversationVCardMetadataRepository: ConversationVCardMetadataRepository,
    savedStateHandle: SavedStateHandle,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMessagesDelegate {

    private val _state = MutableStateFlow<ConversationMessagesUiState>(
        value = ConversationMessagesUiState.Loading,
    )

    override val state = _state.asStateFlow()

    private val refreshTriggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val windowStates = MutableStateFlow(value = savedStateHandle.restoredWindowState())

    private val windowSizes = windowStates
        .map { windowState -> windowState.size }
        .distinctUntilChanged()

    private val loadedMessageCounts = MutableStateFlow(value = 0)
    private val hasOlderMessages = MutableStateFlow(value = false)

    private val hasPendingLoadOlderMessages = MutableStateFlow(value = false)

    private var isBound = false

    init {
        savedStateHandle.setSavedStateProvider(WINDOW_KEY) {
            val windowState = windowStates.value

            Bundle().apply {
                putString(WINDOW_CONVERSATION_ID_KEY, windowState.conversationId)
                putInt(WINDOW_SIZE_KEY, windowState.size)
            }
        }
    }

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<ConversationId?>,
    ) {
        if (isBound) {
            return
        }

        isBound = true

        scope.launch(defaultDispatcher) {
            conversationIdFlow.collectLatest { conversationId ->
                _state.value = ConversationMessagesUiState.Loading
                loadedMessageCounts.value = 0
                hasOlderMessages.value = false
                hasPendingLoadOlderMessages.value = false

                if (conversationId == null) {
                    return@collectLatest
                }

                resetWindowUnlessRestored(conversationId = conversationId)

                observeConversationMessagesUiState(
                    conversationId = conversationId,
                ).collect { currentMessagesUiState ->
                    _state.value = currentMessagesUiState
                }
            }
        }
    }

    override fun refresh() {
        refreshTriggers.tryEmit(Unit)
    }

    override fun loadOlderMessages() {
        hasPendingLoadOlderMessages.value = true

        if (hasOlderMessages.value) {
            growWindow()
        }
    }

    private fun growWindow() {
        if (!hasPendingLoadOlderMessages.compareAndSet(expect = true, update = false)) {
            return
        }

        windowStates.update { windowState ->
            windowState.copy(
                size = maxOf(windowState.size, loadedMessageCounts.value * 2),
            )
        }
    }

    private fun resetWindowUnlessRestored(conversationId: ConversationId) {
        windowStates.update { windowState ->
            when (windowState.conversationId) {
                conversationId.value -> windowState
                else -> ConversationMessagesWindowState(
                    conversationId = conversationId.value,
                    size = CONVERSATION_MESSAGES_WINDOW_STEP,
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeConversationMessagesUiState(
        conversationId: ConversationId,
    ): Flow<ConversationMessagesUiState> {
        return combine(
            conversationsRepository
                .getConversationMessages(
                    conversationId = conversationId,
                    windowSizes = windowSizes,
                )
                .onEach { window ->
                    loadedMessageCounts.value = window.messages.size
                    hasOlderMessages.value = window.hasMore

                    if (window.hasMore && hasPendingLoadOlderMessages.value) {
                        growWindow()
                    }
                }
                .map { window ->
                    window
                        .messages
                        .asSequence()
                        .mapNotNull(conversationMessageUiModelMapper::map)
                        .toImmutableList()
                }
                .map(::withAudioDurations)
                .flatMapLatest { messages ->
                    observeMessagesWithVCardMetadata(
                        messages = messages,
                    )
                },
            observeYouTubeLinkPreviewsEnabled(),
        ) { messages, youTubeLinkPreviewsEnabled ->
            ConversationMessagesUiState.Present(
                messages = messages,
                youTubeLinkPreviewsEnabled = youTubeLinkPreviewsEnabled,
            )
        }
            .flowOn(defaultDispatcher)
    }

    private fun observeYouTubeLinkPreviewsEnabled(): Flow<Boolean> {
        return refreshTriggers
            .onStart { emit(Unit) }
            .map { appSettingsRepository.isYouTubeLinkPreviewsEnabled() }
            .distinctUntilChanged()
    }

    private suspend fun withAudioDurations(
        messages: ImmutableList<ConversationMessageUiModel>,
    ): ImmutableList<ConversationMessageUiModel> {
        val audioContentUris = messages
            .asSequence()
            .flatMap(ConversationMessageUiModel::parts)
            .filterIsInstance<ConversationMessagePartUiModel.Attachment.Audio>()
            .mapNotNullTo(mutableSetOf()) { audioPart -> audioPart.contentUri?.toString() }

        if (audioContentUris.isEmpty()) {
            return messages
        }

        val durationsByContentUri = coroutineScope {
            audioContentUris
                .map { contentUri ->
                    async { contentUri to resolveAudioDurationMillis(contentUri) }
                }
                .awaitAll()
                .toMap()
        }

        return messages
            .map { message ->
                val parts = message.parts.map { part ->
                    when (part) {
                        is ConversationMessagePartUiModel.Attachment.Audio -> {
                            part.copy(
                                durationMillis = part
                                    .contentUri
                                    ?.toString()
                                    ?.let(durationsByContentUri::get)
                                    ?: part.durationMillis,
                            )
                        }

                        else -> part
                    }
                }

                when (parts) {
                    message.parts -> message
                    else -> message.copy(parts = parts.toImmutableList())
                }
            }
            .toImmutableList()
    }

    private fun observeMessagesWithVCardMetadata(
        messages: List<ConversationMessageUiModel>,
    ): Flow<ImmutableList<ConversationMessageUiModel>> {
        val vCardContentUris = messages
            .asSequence()
            .flatMap { message -> message.parts.asSequence() }
            .mapNotNull { part ->
                (part as? ConversationMessagePartUiModel.Attachment.VCard)
                    ?.contentUri
                    ?.toString()
            }
            .distinct()
            .toList()

        if (vCardContentUris.isEmpty()) {
            return flowOf(messages.toImmutableList())
        }

        val vCardMetadataFlows = vCardContentUris.map { contentUri ->
            conversationVCardMetadataRepository
                .observeAttachmentMetadata(
                    contentUri = contentUri,
                    refreshes = refreshTriggers,
                )
                .map { metadata ->
                    contentUri to metadata
                }
        }

        return combine(flows = vCardMetadataFlows) { contentUriAndMetadata ->
            val vCardAttachmentMetadata = contentUriAndMetadata.associate { pair ->
                pair.first to pair.second
            }

            updateMessagesWithVCardUiModel(
                messages = messages,
                vCardAttachmentMetadata = vCardAttachmentMetadata,
            )
        }
    }

    private fun updateMessagesWithVCardUiModel(
        messages: List<ConversationMessageUiModel>,
        vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
    ): ImmutableList<ConversationMessageUiModel> {
        return messages
            .map { message ->
                updateMessageUiModelWithVCardUiModel(
                    message = message,
                    vCardAttachmentMetadata = vCardAttachmentMetadata,
                )
            }
            .toImmutableList()
    }

    private fun updateMessageUiModelWithVCardUiModel(
        message: ConversationMessageUiModel,
        vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
    ): ConversationMessageUiModel {
        return message.copy(
            parts = message
                .parts
                .asSequence()
                .map { part ->
                    updateMessagePartUiModelWithVCardUiModel(
                        part = part,
                        vCardAttachmentMetadata = vCardAttachmentMetadata,
                    )
                }
                .toImmutableList(),
        )
    }

    private fun updateMessagePartUiModelWithVCardUiModel(
        part: ConversationMessagePartUiModel,
        vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
    ): ConversationMessagePartUiModel {
        return when (part) {
            is ConversationMessagePartUiModel.Attachment.VCard -> {
                val contentUri = part.contentUri?.toString()
                val metadata = contentUri?.let(vCardAttachmentMetadata::get)

                part.copy(
                    vCardUiModel = conversationVCardAttachmentUiModelMapper.map(
                        metadata = metadata,
                    ),
                )
            }

            is ConversationMessagePartUiModel.Attachment.Audio,
            is ConversationMessagePartUiModel.Attachment.File,
            is ConversationMessagePartUiModel.Attachment.Image,
            is ConversationMessagePartUiModel.Attachment.Video,
            is ConversationMessagePartUiModel.Text,
            -> {
                part
            }
        }
    }

    /**
     * The loaded window and the conversation it was loaded for, kept together so that they are saved
     * and restored as one value and a window can never be read back onto another conversation.
     */
    private data class ConversationMessagesWindowState(
        val conversationId: String?,
        val size: Int,
    )

    /** The window a restored process is handed, or a fresh one. */
    private fun SavedStateHandle.restoredWindowState(): ConversationMessagesWindowState {
        val savedWindow = get<Bundle>(WINDOW_KEY)

        return ConversationMessagesWindowState(
            conversationId = savedWindow?.getString(WINDOW_CONVERSATION_ID_KEY),
            size = savedWindow?.getInt(WINDOW_SIZE_KEY, CONVERSATION_MESSAGES_WINDOW_STEP)
                ?: CONVERSATION_MESSAGES_WINDOW_STEP,
        )
    }

    private companion object {
        private const val CONVERSATION_MESSAGES_WINDOW_STEP = 500

        private const val WINDOW_KEY = "conversation_messages_window"
        private const val WINDOW_SIZE_KEY = "size"
        private const val WINDOW_CONVERSATION_ID_KEY = "conversation"
    }
}
