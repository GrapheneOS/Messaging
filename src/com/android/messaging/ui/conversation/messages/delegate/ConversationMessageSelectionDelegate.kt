package com.android.messaging.ui.conversation.messages.delegate

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.media.model.AttachmentToSave
import com.android.messaging.data.media.repository.ConversationAttachmentsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.ui.conversation.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMessageAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageDeleteConfirmationUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect as Effect
import com.android.messaging.ui.conversation.screen.model.ConversationScreenNavEvent as NavEvent
import com.android.messaging.ui.conversation.screen.model.availableMessageActions
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface ConversationMessageSelectionActions {
    fun onMessageClick(messageId: MessageId)

    fun onMessageDownloadClick(messageId: MessageId)

    fun onMessageLongClick(messageId: MessageId)

    fun onMessageResendClick(messageId: MessageId)

    fun onMessageActionClick(
        messageId: MessageId,
        action: ConversationMessageAction,
    )

    fun onMessageSelectionActionClick(action: ConversationMessageAction)

    fun dismissDeleteMessageConfirmation()

    fun dismissMessageSelection()

    fun confirmDeleteSelectedMessages()
}

internal interface ConversationMessageSelectionDelegate :
    ConversationScreenDelegate<ConversationMessageSelectionUiState>,
    ConversationMessageSelectionActions {
    val effects: Flow<Effect>
    val navigationEvents: Flow<NavEvent>

    fun onDefaultSmsRoleRequestResult(resultCode: Int): Boolean
}

internal class ConversationMessageSelectionDelegateImpl @Inject constructor(
    private val checkConversationActionRequirements: CheckConversationActionRequirements,
    private val clipboardManager: ClipboardManager,
    private val conversationAttachmentsRepository: ConversationAttachmentsRepository,
    private val conversationMessagesDelegate: ConversationMessagesDelegate,
    private val conversationsRepository: ConversationsRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMessageSelectionDelegate {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    private val _navigationEvents = MutableSharedFlow<NavEvent>(extraBufferCapacity = 1)
    private val _state = MutableStateFlow(ConversationMessageSelectionUiState())
    private val messageSelectionState = MutableStateFlow(
        ConversationMessageSelectionState(),
    )

    override val effects = _effects.asSharedFlow()
    override val navigationEvents = _navigationEvents.asSharedFlow()
    override val state = _state.asStateFlow()

    private var boundScope: CoroutineScope? = null
    private var pendingDefaultSmsRoleMessageAction: MessageActionRequiringReadiness? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<ConversationId?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        bindSelectionUiState(scope = scope)
        bindConversationChanges(
            scope = scope,
            conversationIdFlow = conversationIdFlow,
        )
    }

    override fun onMessageClick(messageId: MessageId) {
        if (state.value.isSelectionMode) {
            toggleMessageSelection(messageId = messageId)
        }
    }

    override fun onMessageDownloadClick(messageId: MessageId) {
        downloadMessageWhenActionRequirementsSatisfied(messageId = messageId)
    }

    override fun onMessageLongClick(messageId: MessageId) {
        toggleMessageSelection(messageId = messageId)
    }

    override fun onMessageResendClick(messageId: MessageId) {
        resendMessageWhenActionRequirementsSatisfied(messageId = messageId)
    }

    override fun onMessageActionClick(
        messageId: MessageId,
        action: ConversationMessageAction,
    ) {
        messageOrNull(messageId = messageId)
            ?.takeIf { action in it.availableMessageActions() }
            ?.let { message ->
                runSingleMessageAction(message = message, action = action)
            }
    }

    override fun onMessageSelectionActionClick(action: ConversationMessageAction) {
        when (action) {
            ConversationMessageAction.Delete -> {
                requestDeleteSelectedMessages()
            }

            else -> {
                singleSelectedMessageOrNull()?.let { message ->
                    runSingleMessageAction(message = message, action = action)
                }
            }
        }
    }

    override fun dismissDeleteMessageConfirmation() {
        messageSelectionState.update { currentState ->
            currentState.copy(
                pendingDeleteMessageIds = persistentSetOf(),
            )
        }
    }

    override fun dismissMessageSelection() {
        clearMessageSelection()
    }

    override fun confirmDeleteSelectedMessages() {
        val deleteConfirmation = state.value.deleteConfirmation ?: return

        deleteMessagesWhenActionRequirementsSatisfied(
            messageIds = deleteConfirmation.messageIds,
        )
    }

    override fun onDefaultSmsRoleRequestResult(resultCode: Int): Boolean {
        val pendingMessageAction = pendingDefaultSmsRoleMessageAction ?: return false
        pendingDefaultSmsRoleMessageAction = null

        if (resultCode == Activity.RESULT_OK) {
            runMessageActionWhenRequirementsSatisfied(messageAction = pendingMessageAction)
        }

        return true
    }

    private fun bindSelectionUiState(scope: CoroutineScope) {
        scope.launch(defaultDispatcher) {
            combine(
                conversationMessagesDelegate.state,
                messageSelectionState,
            ) { messagesUiState, selectionState ->
                buildMessageSelectionUiState(
                    messagesUiState = messagesUiState,
                    selectionState = selectionState,
                )
            }.collect { selectionUiState ->
                _state.value = selectionUiState
            }
        }
    }

    private fun bindConversationChanges(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<ConversationId?>,
    ) {
        scope.launch(defaultDispatcher) {
            conversationIdFlow.collect {
                clearMessageSelection()
            }
        }
    }

    private fun clearMessageSelection() {
        messageSelectionState.value = ConversationMessageSelectionState()
    }

    private fun runSingleMessageAction(
        message: ConversationMessageUiModel,
        action: ConversationMessageAction,
    ) {
        when (action) {
            ConversationMessageAction.Copy -> copyMessageText(message = message)
            ConversationMessageAction.Delete -> {
                requestDeleteMessagesWhenActionRequirementsSatisfied(
                    messageIds = persistentSetOf(message.messageId),
                )
            }
            ConversationMessageAction.Details -> openMessageDetails(message = message)
            ConversationMessageAction.Download -> downloadMessage(message = message)
            ConversationMessageAction.Forward -> forwardMessage(message = message)
            ConversationMessageAction.Resend -> resendMessage(message = message)
            ConversationMessageAction.SaveAttachment -> {
                saveMessageAttachments(message = message)
            }
            ConversationMessageAction.Share -> shareMessage(message = message)
        }
    }

    private fun copyMessageText(message: ConversationMessageUiModel) {
        val text = message.text?.takeIf(String::isNotBlank) ?: return

        clipboardManager.setPrimaryClip(
            ClipData.newPlainText(
                null,
                text,
            ),
        )

        clearMessageSelection()
    }

    private fun downloadMessage(message: ConversationMessageUiModel) {
        clearMessageSelection()
        downloadMessageWhenActionRequirementsSatisfied(messageId = message.messageId)
    }

    private fun emitEffect(effect: Effect) {
        boundScope?.launch(defaultDispatcher) {
            _effects.emit(effect)
        }
    }

    private fun forwardMessage(message: ConversationMessageUiModel) {
        clearMessageSelection()
        _navigationEvents.tryEmit(NavEvent.ForwardMessage(message.messageId))
    }

    private fun openMessageDetails(message: ConversationMessageUiModel) {
        clearMessageSelection()
        _navigationEvents.tryEmit(NavEvent.NavigateToMessageDetails(message.messageId))
    }

    private fun requestDeleteSelectedMessages() {
        val selectedMessageIds = state.value.selectedMessageIds

        if (selectedMessageIds.isEmpty()) {
            return
        }

        requestDeleteMessagesWhenActionRequirementsSatisfied(messageIds = selectedMessageIds)
    }

    private fun resendMessage(message: ConversationMessageUiModel) {
        clearMessageSelection()

        resendMessageWhenActionRequirementsSatisfied(messageId = message.messageId)
    }

    private fun downloadMessageWhenActionRequirementsSatisfied(messageId: MessageId) {
        runMessageActionWhenRequirementsSatisfied(
            messageAction = MessageActionRequiringReadiness.Download(
                messageId = messageId,
            ),
        )
    }

    private fun requestDeleteMessagesWhenActionRequirementsSatisfied(
        messageIds: ImmutableSet<MessageId>,
    ) {
        runWhenConversationActionRequirementsSatisfied(
            isSending = false,
            onReady = {
                messageSelectionState.update { currentState ->
                    currentState.copy(
                        pendingDeleteMessageIds = messageIds,
                    )
                }
            },
            onBlocked = ::clearBlockedDeleteActionState,
        )
    }

    private fun deleteMessagesWhenActionRequirementsSatisfied(
        messageIds: ImmutableSet<MessageId>,
    ) {
        runWhenConversationActionRequirementsSatisfied(
            isSending = false,
            onReady = {
                clearMessageSelection()
                conversationsRepository.deleteMessages(messageIds = messageIds)
            },
            onBlocked = ::clearBlockedDeleteActionState,
        )
    }

    private fun resendMessageWhenActionRequirementsSatisfied(messageId: MessageId) {
        runMessageActionWhenRequirementsSatisfied(
            messageAction = MessageActionRequiringReadiness.Resend(
                messageId = messageId,
            ),
        )
    }

    private fun runMessageActionWhenRequirementsSatisfied(
        messageAction: MessageActionRequiringReadiness,
    ) {
        runWhenConversationActionRequirementsSatisfied(
            isSending = messageAction.isSending,
            onReady = {
                runMessageAction(messageAction = messageAction)
            },
            onMissingDefaultSmsRole = {
                pendingDefaultSmsRoleMessageAction = messageAction
            },
        )
    }

    private fun runWhenConversationActionRequirementsSatisfied(
        isSending: Boolean,
        onReady: () -> Unit,
        onBlocked: () -> Unit = {},
        onMissingDefaultSmsRole: () -> Unit = {},
    ) {
        when (checkConversationActionRequirements()) {
            ConversationActionRequirementsResult.Ready -> {
                onReady()
            }

            ConversationActionRequirementsResult.SmsNotCapable -> {
                onBlocked()
                emitEffect(
                    effect = Effect.ShowMessage(
                        messageResId = R.string.sms_disabled,
                    ),
                )
            }

            ConversationActionRequirementsResult.NoPreferredSmsSim -> {
                onBlocked()
                emitEffect(
                    effect = Effect.ShowMessage(
                        messageResId = R.string.no_preferred_sim_selected,
                    ),
                )
            }

            ConversationActionRequirementsResult.MissingDefaultSmsRole -> {
                onBlocked()
                onMissingDefaultSmsRole()
                emitEffect(
                    effect = Effect.RequestDefaultSmsRole(
                        isSending = isSending,
                    ),
                )
            }
        }
    }

    private fun runMessageAction(messageAction: MessageActionRequiringReadiness) {
        when (messageAction) {
            is MessageActionRequiringReadiness.Download -> {
                conversationsRepository.downloadMessage(messageId = messageAction.messageId)
            }

            is MessageActionRequiringReadiness.Resend -> {
                conversationsRepository.resendMessage(messageId = messageAction.messageId)
            }
        }
    }

    private fun clearBlockedDeleteActionState() {
        pendingDefaultSmsRoleMessageAction = null
        clearMessageSelection()
    }

    private fun singleSelectedMessageOrNull(): ConversationMessageUiModel? {
        return state.value.selectedMessageIds.singleOrNull()?.let(::messageOrNull)
    }

    private fun messageOrNull(messageId: MessageId): ConversationMessageUiModel? {
        return when (val messagesUiState = conversationMessagesDelegate.state.value) {
            is ConversationMessagesUiState.Present -> {
                messagesUiState.messages.firstOrNull { message ->
                    message.messageId == messageId
                }
            }

            ConversationMessagesUiState.Loading -> null
        }
    }

    private fun saveMessageAttachments(message: ConversationMessageUiModel) {
        val attachments = message.parts
            .asSequence()
            .filterIsInstance<ConversationMessagePartUiModel.Attachment>()
            .filterNot { it.contentType.isBlank() }
            .mapNotNull { attachment ->
                when (val contentUri = attachment.contentUri) {
                    null -> null

                    else -> {
                        AttachmentToSave(
                            contentType = attachment.contentType,
                            contentUri = contentUri.toString(),
                        )
                    }
                }
            }
            .toList()

        clearMessageSelection()

        if (attachments.isEmpty()) {
            return
        }

        boundScope?.launch(defaultDispatcher) {
            conversationAttachmentsRepository
                .saveAttachmentsToMediaStore(attachments = attachments)
                .collect { result ->
                    _effects.emit(
                        Effect.ShowSaveAttachmentsResult(
                            imageCount = result.imageCount,
                            videoCount = result.videoCount,
                            otherCount = result.otherCount,
                            failCount = result.failCount,
                        ),
                    )
                }
        }
    }

    private fun shareMessage(message: ConversationMessageUiModel) {
        val messageText = message.text?.takeIf(String::isNotBlank)

        val firstAttachment = when {
            messageText != null -> null
            else -> {
                message.parts
                    .asSequence()
                    .mapNotNull { part ->
                        part as? ConversationMessagePartUiModel.Attachment
                    }
                    .firstOrNull { attachment ->
                        attachment.contentType.isNotBlank() && attachment.contentUri != null
                    }
            }
        }

        clearMessageSelection()
        emitEffect(
            effect = Effect.ShareMessage(
                attachmentContentType = firstAttachment?.contentType,
                attachmentContentUri = firstAttachment?.contentUri?.toString(),
                text = messageText,
            ),
        )
    }

    private fun toggleMessageSelection(messageId: MessageId) {
        if (messageId.isBlank()) {
            return
        }

        val selectedMessageIds = state.value.selectedMessageIds

        val updatedMessageIds = when {
            selectedMessageIds.contains(messageId) -> {
                (selectedMessageIds - messageId).toImmutableSet()
            }

            else -> {
                (selectedMessageIds + messageId).toImmutableSet()
            }
        }

        messageSelectionState.update { currentState ->
            currentState.copy(
                selectedMessageIds = updatedMessageIds,
                pendingDeleteMessageIds = persistentSetOf(),
            )
        }
    }

    private fun buildMessageSelectionUiState(
        messagesUiState: ConversationMessagesUiState,
        selectionState: ConversationMessageSelectionState,
    ): ConversationMessageSelectionUiState {
        val messages = when (messagesUiState) {
            is ConversationMessagesUiState.Present -> messagesUiState.messages
            ConversationMessagesUiState.Loading -> return ConversationMessageSelectionUiState()
        }

        val messagesById = messages.associateBy(ConversationMessageUiModel::messageId)
        val currentMessageIds = messagesById.keys

        val selectedMessageIds = selectionState
            .selectedMessageIds
            .asSequence()
            .filter(currentMessageIds::contains)
            .toImmutableSet()

        val pendingDeleteMessageIds = selectionState
            .pendingDeleteMessageIds
            .asSequence()
            .filter(currentMessageIds::contains)
            .toImmutableSet()

        val selectedMessage = when (selectedMessageIds.size) {
            1 -> messagesById[selectedMessageIds.first()]
            else -> null
        }

        return ConversationMessageSelectionUiState(
            selectedMessageIds = selectedMessageIds,
            availableActions = availableSelectionActions(
                selectedMessage = selectedMessage,
                selectedMessageCount = selectedMessageIds.size,
            ),
            deleteConfirmation = pendingDeleteMessageIds
                .takeIf { messageIds ->
                    messageIds.isNotEmpty()
                }
                ?.let { messageIds ->
                    ConversationMessageDeleteConfirmationUiState(
                        messageIds = messageIds,
                    )
                },
        )
    }

    private fun availableSelectionActions(
        selectedMessage: ConversationMessageUiModel?,
        selectedMessageCount: Int,
    ): ImmutableSet<ConversationMessageAction> {
        return when {
            selectedMessageCount <= 0 -> persistentSetOf()
            selectedMessageCount > 1 || selectedMessage == null -> {
                persistentSetOf(
                    ConversationMessageAction.Delete,
                )
            }

            else -> selectedMessage.availableMessageActions()
        }
    }
}

private data class ConversationMessageSelectionState(
    val selectedMessageIds: ImmutableSet<MessageId> = persistentSetOf(),
    val pendingDeleteMessageIds: ImmutableSet<MessageId> = persistentSetOf(),
)

private sealed interface MessageActionRequiringReadiness {
    val messageId: MessageId
    val isSending: Boolean

    data class Download(
        override val messageId: MessageId,
    ) : MessageActionRequiringReadiness {
        override val isSending: Boolean = false
    }

    data class Resend(
        override val messageId: MessageId,
    ) : MessageActionRequiringReadiness {
        override val isSending: Boolean = true
    }
}
