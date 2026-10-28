package com.android.messaging.ui.conversation.messages.delegate

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.message.ConversationMessagePosition
import com.android.messaging.data.conversation.model.message.ConversationMessagesWindow
import com.android.messaging.data.conversation.model.message.ConversationMessagesWindowRequest
import com.android.messaging.data.conversation.repository.ConversationVCardMetadataRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.media.usecase.ResolveAudioDurationMillis
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.messages.mapper.withAudioDurations
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface ConversationMessagesDelegate :
    ConversationScreenDelegate<ConversationMessagesUiState> {
    fun refresh()

    fun loadOlderMessages()

    /** Grows a window around an older message towards the newest one. */
    fun loadNewerMessages()

    /** Swaps a window around an older message for the newest messages. */
    fun loadNewestMessages()

    /**
     * Moves the window around [messageId] unless it already holds it, or marks it missing once it
     * turns out to be gone from the conversation.
     */
    fun loadMessage(messageId: MessageId)

    /**
     * Opens [conversationId], when it is bound next, with the window around [messageId], rather than
     * loading its newest messages only to move away from them.
     */
    fun startAtMessage(conversationId: ConversationId, messageId: MessageId)

    fun highlightMessage(messageId: MessageId)

    fun clearMessageHighlight(messageId: MessageId)
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

    private val windowRequests = windowStates
        .map { windowState -> windowState.request }
        .distinctUntilChanged()

    private val loadedWindowBounds = MutableStateFlow<ConversationMessagesWindowBounds?>(
        value = null,
    )

    private val hasPendingLoadOlderMessages = MutableStateFlow(value = false)
    private val hasPendingLoadNewerMessages = MutableStateFlow(value = false)

    private val messageTargets = MutableStateFlow(value = ConversationMessageTargets())
    private val startingMessages = MutableStateFlow<ConversationStartingMessage?>(value = null)

    private var boundScope: CoroutineScope? = null
    private var boundConversationIdFlow: StateFlow<ConversationId?>? = null
    private var loadMessageJob: Job? = null

    init {
        savedStateHandle.setSavedStateProvider(WINDOW_KEY) {
            val windowState = windowStates.value

            Bundle().apply {
                putString(WINDOW_CONVERSATION_ID_KEY, windowState.conversationId)

                when (val request = windowState.request) {
                    is ConversationMessagesWindowRequest.Newest -> {
                        putInt(WINDOW_SIZE_KEY, request.size)
                    }

                    is ConversationMessagesWindowRequest.Around -> {
                        putInt(WINDOW_SIZE_KEY, request.olderSize)
                        putInt(WINDOW_NEWER_SIZE_KEY, request.newerSize)
                        putString(WINDOW_POSITION_MESSAGE_ID_KEY, request.position.messageId.value)
                        putLong(WINDOW_POSITION_TIMESTAMP_KEY, request.position.receivedTimestamp)
                    }
                }
            }
        }
    }

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<ConversationId?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope
        boundConversationIdFlow = conversationIdFlow

        scope.launch(defaultDispatcher) {
            conversationIdFlow.collectLatest { conversationId ->
                _state.value = ConversationMessagesUiState.Loading
                loadedWindowBounds.value = null
                hasPendingLoadOlderMessages.value = false
                hasPendingLoadNewerMessages.value = false
                messageTargets.value = ConversationMessageTargets()

                if (conversationId == null) {
                    return@collectLatest
                }

                resetWindowUnlessRestored(conversationId = conversationId)
                moveWindowToStartingMessage(conversationId = conversationId)

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
        growWindow()
    }

    override fun loadNewerMessages() {
        hasPendingLoadNewerMessages.value = true
        growWindow()
    }

    override fun loadNewestMessages() {
        val conversationId = boundConversationIdFlow?.value ?: return

        if (windowStates.value.request is ConversationMessagesWindowRequest.Around) {
            moveWindow(
                conversationId = conversationId,
                request = ConversationMessagesWindowRequest.Newest(
                    size = CONVERSATION_MESSAGES_WINDOW_STEP,
                ),
            )
        }
    }

    override fun loadMessage(messageId: MessageId) {
        val scope = boundScope
        val conversationId = boundConversationIdFlow?.value

        if (scope == null || conversationId == null || isMessageLoaded(messageId = messageId)) {
            return
        }

        loadMessageJob?.cancel()
        loadMessageJob = scope.launch(defaultDispatcher) {
            val position = conversationsRepository.getMessagePosition(
                conversationId = conversationId,
                messageId = messageId,
            )

            if (boundConversationIdFlow?.value != conversationId) {
                return@launch
            }

            moveWindowAround(
                conversationId = conversationId,
                messageId = messageId,
                position = position,
            )
        }
    }

    override fun startAtMessage(conversationId: ConversationId, messageId: MessageId) {
        startingMessages.value = ConversationStartingMessage(
            conversationId = conversationId,
            messageId = messageId,
        )
    }

    override fun highlightMessage(messageId: MessageId) {
        messageTargets.update { targets ->
            targets.copy(highlightedMessageId = messageId)
        }
    }

    override fun clearMessageHighlight(messageId: MessageId) {
        messageTargets.update { targets ->
            when (targets.highlightedMessageId) {
                messageId -> targets.copy(highlightedMessageId = null)
                else -> targets
            }
        }
    }

    private fun isMessageLoaded(messageId: MessageId): Boolean {
        val messagesState = _state.value as? ConversationMessagesUiState.Present ?: return false

        return messagesState.messages.any { message -> message.messageId == messageId }
    }

    private suspend fun moveWindowToStartingMessage(conversationId: ConversationId) {
        val startingMessage = startingMessages
            .getAndUpdate { null }
            ?.takeIf { startingMessage -> startingMessage.conversationId == conversationId }
            ?: return

        moveWindowAround(
            conversationId = conversationId,
            messageId = startingMessage.messageId,
            position = conversationsRepository.getMessagePosition(
                conversationId = conversationId,
                messageId = startingMessage.messageId,
            ),
        )
    }

    /** [position] is where [messageId] sorts, or null once it is gone from the conversation. */
    private fun moveWindowAround(
        conversationId: ConversationId,
        messageId: MessageId,
        position: ConversationMessagePosition?,
    ) {
        when (position) {
            null -> {
                messageTargets.update { targets ->
                    targets.copy(missingMessageId = messageId)
                }
            }

            else -> {
                moveWindow(
                    conversationId = conversationId,
                    request = ConversationMessagesWindowRequest.Around(
                        position = position,
                        olderSize = CONVERSATION_MESSAGES_AROUND_SIDE,
                        newerSize = CONVERSATION_MESSAGES_AROUND_SIDE,
                    ),
                )
            }
        }
    }

    private fun moveWindow(
        conversationId: ConversationId,
        request: ConversationMessagesWindowRequest,
    ) {
        hasPendingLoadOlderMessages.value = false
        hasPendingLoadNewerMessages.value = false

        // Also before the first window of the conversation is in, which then starts out here. A
        // window for a conversation left meanwhile is replaced as soon as the next one binds.
        windowStates.update { windowState ->
            when {
                windowState.conversationId == conversationId.value &&
                    windowState.request == request -> windowState

                else -> ConversationMessagesWindowState(
                    conversationId = conversationId.value,
                    request = request,
                    windowId = windowState.windowId + 1,
                )
            }
        }
    }

    /**
     * Doubles what the window on screen loaded on each side that was asked for and has more. While
     * a window for another request is on its way nothing grows, and its arrival looks again.
     */
    private fun growWindow() {
        val loadedBounds = loadedWindowBounds.value ?: return

        if (!windowStates.value.request.isSameWindowAs(other = loadedBounds.request)) {
            return
        }

        val growsOlder = loadedBounds.hasOlder &&
            hasPendingLoadOlderMessages.compareAndSet(expect = true, update = false)
        val growsNewer = loadedBounds.hasNewer &&
            hasPendingLoadNewerMessages.compareAndSet(expect = true, update = false)

        if (growsOlder || growsNewer) {
            windowStates.update { windowState ->
                windowState.copy(
                    request = windowState.request.grownFrom(
                        loaded = loadedBounds.request,
                        growsOlder = growsOlder,
                        growsNewer = growsNewer,
                    ),
                )
            }
        }
    }

    /**
     * A window around a message that has grown to the newest one holds what the newest messages
     * window of its size does, and from then on it is that window: new messages join it.
     */
    private fun showNewestMessagesOnceReached(window: ConversationMessagesWindow) {
        if (window.request !is ConversationMessagesWindowRequest.Around || window.hasNewer) {
            return
        }

        hasPendingLoadNewerMessages.value = false

        windowStates.update { windowState ->
            when (windowState.request) {
                window.request -> {
                    windowState.copy(
                        request = ConversationMessagesWindowRequest.Newest(
                            size = maxOf(CONVERSATION_MESSAGES_WINDOW_STEP, window.messages.size),
                        ),
                    )
                }

                else -> windowState
            }
        }
    }

    /**
     * Null for a window loaded for where the window was before it moved, which it is about to
     * replace: showing it would only flash it.
     */
    private fun onWindowLoaded(
        window: ConversationMessagesWindow,
    ): ConversationMessagesWindowContent? {
        val windowState = windowStates.value

        if (!windowState.request.isSameWindowAs(other = window.request)) {
            return null
        }

        loadedWindowBounds.value = ConversationMessagesWindowBounds(
            request = window.request,
            hasOlder = window.hasOlder,
            hasNewer = window.hasNewer,
        )
        showNewestMessagesOnceReached(window = window)
        growWindow()

        return ConversationMessagesWindowContent(
            windowId = windowState.windowId,
            messages = window
                .messages
                .asSequence()
                .mapNotNull(conversationMessageUiModelMapper::map)
                .toImmutableList(),
            hasNewerMessages = window.hasNewer,
            latestUnloadedMessage = window
                .latestMessage
                ?.let(conversationMessageUiModelMapper::map),
        )
    }

    private fun resetWindowUnlessRestored(conversationId: ConversationId) {
        windowStates.update { windowState ->
            when (windowState.conversationId) {
                conversationId.value -> windowState
                else -> ConversationMessagesWindowState(
                    conversationId = conversationId.value,
                    request = ConversationMessagesWindowRequest.Newest(
                        size = CONVERSATION_MESSAGES_WINDOW_STEP,
                    ),
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
                    windows = windowRequests,
                )
                .mapNotNull(::onWindowLoaded)
                .map { content ->
                    content.copy(
                        messages = withAudioDurations(
                            messages = content.messages,
                            resolveAudioDurationMillis = resolveAudioDurationMillis,
                        ),
                    )
                }
                .flatMapLatest { content ->
                    observeMessagesWithVCardMetadata(
                        messages = content.messages,
                    ).map { messages ->
                        content.copy(messages = messages)
                    }
                },
            observeYouTubeLinkPreviewsEnabled(),
            messageTargets,
        ) { content, youTubeLinkPreviewsEnabled, targets ->
            ConversationMessagesUiState.Present(
                messages = content.messages,
                youTubeLinkPreviewsEnabled = youTubeLinkPreviewsEnabled,
                highlightedMessageId = targets.highlightedMessageId,
                missingMessageId = targets.missingMessageId,
                windowId = content.windowId,
                hasNewerMessages = content.hasNewerMessages,
                latestUnloadedMessage = content.latestUnloadedMessage,
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

    private fun ConversationMessagesWindowRequest.isSameWindowAs(
        other: ConversationMessagesWindowRequest,
    ): Boolean {
        return when (this) {
            is ConversationMessagesWindowRequest.Newest -> {
                other is ConversationMessagesWindowRequest.Newest
            }

            is ConversationMessagesWindowRequest.Around -> {
                other is ConversationMessagesWindowRequest.Around && other.position == position
            }
        }
    }

    /** Sizes from [loaded] rather than from this, so that asking twice before it grows grows once. */
    private fun ConversationMessagesWindowRequest.grownFrom(
        loaded: ConversationMessagesWindowRequest,
        growsOlder: Boolean,
        growsNewer: Boolean,
    ): ConversationMessagesWindowRequest {
        return when {
            this is ConversationMessagesWindowRequest.Newest &&
                loaded is ConversationMessagesWindowRequest.Newest -> {
                copy(size = grownSize(size, loaded.size, grows = growsOlder))
            }

            this is ConversationMessagesWindowRequest.Around &&
                loaded is ConversationMessagesWindowRequest.Around -> {
                copy(
                    olderSize = grownSize(olderSize, loaded.olderSize, grows = growsOlder),
                    newerSize = grownSize(newerSize, loaded.newerSize, grows = growsNewer),
                )
            }

            else -> this
        }
    }

    private fun grownSize(size: Int, loadedSize: Int, grows: Boolean): Int {
        return when {
            grows -> maxOf(size, loadedSize * 2)
            else -> size
        }
    }

    private data class ConversationStartingMessage(
        val conversationId: ConversationId,
        val messageId: MessageId,
    )

    private data class ConversationMessageTargets(
        val highlightedMessageId: MessageId? = null,
        val missingMessageId: MessageId? = null,
    )

    /** What the window on screen was loaded for, and whether the conversation goes on past it. */
    private data class ConversationMessagesWindowBounds(
        val request: ConversationMessagesWindowRequest,
        val hasOlder: Boolean,
        val hasNewer: Boolean,
    )

    private data class ConversationMessagesWindowContent(
        val windowId: Int,
        val messages: ImmutableList<ConversationMessageUiModel>,
        val hasNewerMessages: Boolean,
        val latestUnloadedMessage: ConversationMessageUiModel?,
    )

    /**
     * The loaded window and the conversation it was loaded for, kept together so that they are saved
     * and restored as one value and a window can never be read back onto another conversation.
     *
     * [windowId] changes each time the window moves elsewhere in the conversation, and stays the
     * same while it only grows.
     */
    private data class ConversationMessagesWindowState(
        val conversationId: String?,
        val request: ConversationMessagesWindowRequest,
        val windowId: Int = 0,
    )

    /** The window a restored process is handed, or a fresh one. */
    private fun SavedStateHandle.restoredWindowState(): ConversationMessagesWindowState {
        val savedWindow = get<Bundle>(WINDOW_KEY)

        return ConversationMessagesWindowState(
            conversationId = savedWindow?.getString(WINDOW_CONVERSATION_ID_KEY),
            request = savedWindow?.restoredWindowAround()
                ?: ConversationMessagesWindowRequest.Newest(
                    size = savedWindow
                        ?.getInt(WINDOW_SIZE_KEY, CONVERSATION_MESSAGES_WINDOW_STEP)
                        .restoredSize(minimum = CONVERSATION_MESSAGES_WINDOW_STEP),
                ),
        )
    }

    private fun Bundle.restoredWindowAround(): ConversationMessagesWindowRequest.Around? {
        val messageId = getString(WINDOW_POSITION_MESSAGE_ID_KEY) ?: return null

        return ConversationMessagesWindowRequest.Around(
            position = ConversationMessagePosition(
                messageId = MessageId(messageId),
                receivedTimestamp = getLong(WINDOW_POSITION_TIMESTAMP_KEY),
            ),
            olderSize = getInt(WINDOW_SIZE_KEY)
                .restoredSize(minimum = CONVERSATION_MESSAGES_AROUND_SIDE),
            newerSize = getInt(WINDOW_NEWER_SIZE_KEY)
                .restoredSize(minimum = CONVERSATION_MESSAGES_AROUND_SIDE),
        )
    }

    /** Also neutralizes corrupt or zero saved values before the provider's `limit > 0` assertion. */
    private fun Int?.restoredSize(minimum: Int): Int {
        return (this ?: minimum).coerceIn(minimum, MAX_RESTORED_WINDOW_SIZE)
    }

    private companion object {
        private const val CONVERSATION_MESSAGES_WINDOW_STEP = 500

        /** Each side of a window around a message: as much as a fresh window holds, in all. */
        private const val CONVERSATION_MESSAGES_AROUND_SIDE = CONVERSATION_MESSAGES_WINDOW_STEP / 2

        /**
         * 4000 is three doublings from the 500 step: enough to keep a restored deep scroll
         * position in the window while keeping the first query after process death fast.
         * LazyListState clamps an out-of-range restored index to the oldest loaded row, where
         * the existing scroll-back trigger grows the window again on demand. A window around a
         * message is capped on each side.
         */
        private const val MAX_RESTORED_WINDOW_SIZE = CONVERSATION_MESSAGES_WINDOW_STEP * 8

        private const val WINDOW_KEY = "conversation_messages_window"
        private const val WINDOW_SIZE_KEY = "size"
        private const val WINDOW_NEWER_SIZE_KEY = "newer_size"
        private const val WINDOW_POSITION_MESSAGE_ID_KEY = "position_message"
        private const val WINDOW_POSITION_TIMESTAMP_KEY = "position_timestamp"
        private const val WINDOW_CONVERSATION_ID_KEY = "conversation"
    }
}
