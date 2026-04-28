package com.android.messaging.ui.conversation.v2.messages.delegate

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import com.android.messaging.R
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.domain.conversation.usecase.forward.CreateForwardedMessage
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.repository.ConversationAttachmentRepository
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageDeleteConfirmationUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
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

internal interface ConversationMessageSelectionDelegate :
    ConversationScreenDelegate<ConversationMessageSelectionUiState> {
    val effects: Flow<ConversationScreenEffect>

    fun onMessageClick(messageId: String)

    fun onMessageLongClick(messageId: String)

    fun onMessageResendClick(messageId: String)

    fun onMessageSelectionActionClick(action: ConversationMessageSelectionAction)

    fun dismissDeleteMessageConfirmation()

    fun dismissMessageSelection()

    fun confirmDeleteSelectedMessages()

    fun onDefaultSmsRoleRequestResult(resultCode: Int): Boolean
}

internal class ConversationMessageSelectionDelegateImpl @Inject constructor(
    private val checkConversationActionRequirements: CheckConversationActionRequirements,
    private val clipboardManager: ClipboardManager,
    private val conversationAttachmentRepository: ConversationAttachmentRepository,
    private val conversationMessagesDelegate: ConversationMessagesDelegate,
    private val createForwardedMessage: CreateForwardedMessage,
    private val conversationsRepository: ConversationsRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMessageSelectionDelegate {

    private val _effects = MutableSharedFlow<ConversationScreenEffect>(
        extraBufferCapacity = 1,
    )
    private val _state = MutableStateFlow(ConversationMessageSelectionUiState())
    private val messageSelectionState = MutableStateFlow(
        ConversationMessageSelectionState(),
    )

    override val effects = _effects.asSharedFlow()
    override val state = _state.asStateFlow()

    private var boundScope: CoroutineScope? = null
    private var pendingDefaultSmsRoleResendMessageId: String? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
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

    override fun onMessageClick(messageId: String) {
        if (state.value.isSelectionMode) {
            toggleMessageSelection(messageId = messageId)
        }
    }

    override fun onMessageLongClick(messageId: String) {
        toggleMessageSelection(messageId = messageId)
    }

    override fun onMessageResendClick(messageId: String) {
        resendMessageWhenActionRequirementsSatisfied(messageId = messageId)
    }

    override fun onMessageSelectionActionClick(action: ConversationMessageSelectionAction) {
        when (action) {
            ConversationMessageSelectionAction.Copy -> {
                copySelectedMessageText()
            }

            ConversationMessageSelectionAction.Delete -> {
                requestDeleteSelectedMessages()
            }

            ConversationMessageSelectionAction.Details -> {
                openSelectedMessageDetails()
            }

            ConversationMessageSelectionAction.Download -> {
                downloadSelectedMessage()
            }

            ConversationMessageSelectionAction.Forward -> {
                forwardSelectedMessage()
            }

            ConversationMessageSelectionAction.Resend -> {
                resendSelectedMessage()
            }

            ConversationMessageSelectionAction.SaveAttachment -> {
                saveSelectedMessageAttachments()
            }

            ConversationMessageSelectionAction.Share -> {
                shareSelectedMessage()
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

        clearMessageSelection()
        conversationsRepository.deleteMessages(
            messageIds = deleteConfirmation.messageIds,
        )
    }

    override fun onDefaultSmsRoleRequestResult(resultCode: Int): Boolean {
        val messageId = pendingDefaultSmsRoleResendMessageId ?: return false
        pendingDefaultSmsRoleResendMessageId = null

        if (resultCode != Activity.RESULT_OK) {
            return true
        }

        resendMessageWhenActionRequirementsSatisfied(messageId = messageId)
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
        conversationIdFlow: StateFlow<String?>,
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

    private fun copySelectedMessageText() {
        val selectedMessage = singleSelectedMessageOrNull() ?: return
        val text = selectedMessage.text?.takeIf(String::isNotBlank) ?: return

        clipboardManager.setPrimaryClip(
            ClipData.newPlainText(
                null,
                text,
            ),
        )

        clearMessageSelection()
    }

    private fun downloadSelectedMessage() {
        val selectedMessage = singleSelectedMessageOrNull() ?: return

        clearMessageSelection()
        conversationsRepository.downloadMessage(selectedMessage.messageId)
    }

    private fun emitEffect(effect: ConversationScreenEffect) {
        boundScope?.launch(defaultDispatcher) {
            _effects.emit(effect)
        }
    }

    private fun forwardSelectedMessage() {
        val selectedMessage = singleSelectedMessageOrNull() ?: return

        clearMessageSelection()

        boundScope?.launch(defaultDispatcher) {
            val forwardedMessage = createForwardedMessage(
                conversationId = selectedMessage.conversationId,
                messageId = selectedMessage.messageId,
            ) ?: return@launch

            _effects.emit(
                ConversationScreenEffect.LaunchForwardMessage(
                    message = forwardedMessage,
                ),
            )
        }
    }

    private fun openSelectedMessageDetails() {
        val selectedMessage = singleSelectedMessageOrNull() ?: return

        clearMessageSelection()
        boundScope?.launch(defaultDispatcher) {
            conversationsRepository
                .getMessageDetailsData(
                    conversationId = selectedMessage.conversationId,
                    messageId = selectedMessage.messageId,
                )
                ?.let { messageDetailsData ->
                    ConversationScreenEffect.ShowMessageDetails(
                        message = messageDetailsData.message,
                        participants = messageDetailsData.participants,
                        selfParticipant = messageDetailsData.selfParticipant,
                    )
                }?.let { effect ->
                    _effects.emit(effect)
                }
        }
    }

    private fun requestDeleteSelectedMessages() {
        val selectedMessageIds = state.value.selectedMessageIds

        if (selectedMessageIds.isEmpty()) {
            return
        }

        messageSelectionState.update { currentState ->
            currentState.copy(
                pendingDeleteMessageIds = selectedMessageIds,
            )
        }
    }

    private fun resendSelectedMessage() {
        val selectedMessage = singleSelectedMessageOrNull() ?: return

        clearMessageSelection()

        resendMessageWhenActionRequirementsSatisfied(messageId = selectedMessage.messageId)
    }

    private fun resendMessageWhenActionRequirementsSatisfied(messageId: String) {
        when (checkConversationActionRequirements()) {
            ConversationActionRequirementsResult.Ready -> {
                conversationsRepository.resendMessage(
                    messageId = messageId,
                )
            }

            ConversationActionRequirementsResult.SmsNotCapable -> {
                emitEffect(
                    effect = ConversationScreenEffect.ShowMessage(
                        messageResId = R.string.sms_disabled,
                    ),
                )
            }

            ConversationActionRequirementsResult.NoPreferredSmsSim -> {
                emitEffect(
                    effect = ConversationScreenEffect.ShowMessage(
                        messageResId = R.string.no_preferred_sim_selected,
                    ),
                )
            }

            ConversationActionRequirementsResult.MissingDefaultSmsRole -> {
                pendingDefaultSmsRoleResendMessageId = messageId

                emitEffect(
                    effect = ConversationScreenEffect.RequestDefaultSmsRole(
                        isSending = true,
                    ),
                )
            }
        }
    }

    private fun singleSelectedMessageOrNull(): ConversationMessageUiModel? {
        val messagesUiState = conversationMessagesDelegate.state.value
        val selectedMessageIds = state
            .value
            .selectedMessageIds
            .takeIf { it.size == 1 }
            ?: return null

        return when (messagesUiState) {
            is ConversationMessagesUiState.Present -> {
                messagesUiState.messages.firstOrNull { message ->
                    message.messageId == selectedMessageIds.first()
                }
            }

            ConversationMessagesUiState.Loading -> null
        }
    }

    private fun saveSelectedMessageAttachments() {
        val selectedMessage = singleSelectedMessageOrNull() ?: return

        val attachments = selectedMessage.parts
            .asSequence()
            .filterIsInstance<ConversationMessagePartUiModel.Attachment>()
            .filterNot { it.contentType.isBlank() }
            .mapNotNull { attachment ->
                when (val contentUri = attachment.contentUri) {
                    null -> null

                    else -> {
                        ConversationAttachmentRepository.AttachmentToSave(
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
            conversationAttachmentRepository
                .saveAttachmentsToMediaStore(attachments = attachments)
                .collect { result ->
                    _effects.emit(
                        ConversationScreenEffect.ShowSaveAttachmentsResult(
                            imageCount = result.imageCount,
                            videoCount = result.videoCount,
                            otherCount = result.otherCount,
                            failCount = result.failCount,
                        ),
                    )
                }
        }
    }

    private fun shareSelectedMessage() {
        val selectedMessage = singleSelectedMessageOrNull() ?: return
        val messageText = selectedMessage.text?.takeIf(String::isNotBlank)

        val firstAttachment = when {
            messageText != null -> null
            else -> {
                selectedMessage.parts
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
            effect = ConversationScreenEffect.ShareMessage(
                attachmentContentType = firstAttachment?.contentType,
                attachmentContentUri = firstAttachment?.contentUri?.toString(),
                text = messageText,
            ),
        )
    }

    private fun toggleMessageSelection(messageId: String) {
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
): ImmutableSet<ConversationMessageSelectionAction> {
    if (selectedMessageCount <= 0) {
        return persistentSetOf()
    }

    if (selectedMessageCount > 1 || selectedMessage == null) {
        return persistentSetOf(
            ConversationMessageSelectionAction.Delete,
        )
    }

    val actions = LinkedHashSet<ConversationMessageSelectionAction>()

    if (selectedMessage.canDownloadMessage) {
        actions += ConversationMessageSelectionAction.Download
    }

    if (selectedMessage.canResendMessage) {
        actions += ConversationMessageSelectionAction.Resend
    }

    actions += ConversationMessageSelectionAction.Delete

    if (selectedMessage.canForwardMessage) {
        actions += ConversationMessageSelectionAction.Share
        actions += ConversationMessageSelectionAction.Forward
    }

    if (selectedMessage.canSaveAttachments) {
        actions += ConversationMessageSelectionAction.SaveAttachment
    }

    if (selectedMessage.canCopyMessageToClipboard) {
        actions += ConversationMessageSelectionAction.Copy
    }

    actions += ConversationMessageSelectionAction.Details

    return actions.toImmutableSet()
}

private data class ConversationMessageSelectionState(
    val selectedMessageIds: ImmutableSet<String> = persistentSetOf(),
    val pendingDeleteMessageIds: ImmutableSet<String> = persistentSetOf(),
)
