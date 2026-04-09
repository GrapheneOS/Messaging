package com.android.messaging.ui.conversation.v2.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.ui.conversation.v2.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerAttachmentUiState
import com.android.messaging.ui.conversation.v2.composer.ui.ConversationComposerSection
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerOverlay
import com.android.messaging.ui.conversation.v2.mediapicker.rememberConversationMediaPickerState
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.messages.ui.ConversationMessages
import com.android.messaging.ui.conversation.v2.metadata.ui.ConversationTopAppBar
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenScaffoldUiState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ConversationScreen(
    modifier: Modifier = Modifier,
    conversationId: String? = null,
    onNavigateBack: () -> Unit = {},
    screenModel: ConversationScreenModel = viewModel<ConversationViewModel>(),
) {
    val messageFieldFocusRequester = remember {
        FocusRequester()
    }
    val mediaPickerState = rememberConversationMediaPickerState()
    val scaffoldUiState by screenModel.scaffoldUiState.collectAsStateWithLifecycle()
    val mediaPickerOverlayUiState by screenModel
        .mediaPickerOverlayUiState
        .collectAsStateWithLifecycle()

    LaunchedEffect(conversationId) {
        screenModel.onConversationChanged(conversationId = conversationId)
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        screenModel.persistDraft()
    }

    ConversationScreenEffects(screenModel = screenModel)

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        ConversationScreenScaffold(
            modifier = Modifier
                .fillMaxSize(),
            conversationId = conversationId,
            uiState = scaffoldUiState,
            isMediaPickerOpen = mediaPickerState.isOpen,
            messageFieldFocusRequester = messageFieldFocusRequester,
            onNavigateBack = onNavigateBack,
            onOpenMediaPicker = mediaPickerState::open,
            onMessageTextChange = screenModel::onMessageTextChanged,
            onPendingAttachmentRemove = screenModel::onRemovePendingAttachment,
            onResolvedAttachmentClick = screenModel::onAttachmentClicked,
            onResolvedAttachmentRemove = screenModel::onRemoveResolvedAttachment,
            onSendClick = screenModel::onSendClick,
            onAttachmentClick = screenModel::onMessageAttachmentClicked,
            onExternalUriClick = screenModel::onExternalUriClicked,
        )

        ConversationMediaPickerOverlay(
            modifier = Modifier
                .fillMaxSize(),
            state = mediaPickerState,
            mediaPickerUiState = mediaPickerOverlayUiState.mediaPicker,
            attachments = mediaPickerOverlayUiState.attachments,
            conversationTitle = mediaPickerOverlayUiState.conversationTitle,
            isSendActionEnabled = mediaPickerOverlayUiState.isSendActionEnabled,
            messageFieldFocusRequester = messageFieldFocusRequester,
            onAttachmentPreviewClick = screenModel::onAttachmentClicked,
            onAttachmentCaptionChange = screenModel::onUpdateAttachmentCaption,
            onAttachmentRemove = screenModel::onRemoveResolvedAttachment,
            onGalleryMediaConfirmed = screenModel::onGalleryMediaConfirmed,
            onGalleryVisibilityChanged = screenModel::onGalleryVisibilityChanged,
            onCapturedMediaReady = screenModel::onCapturedMediaReady,
            onSendClick = screenModel::onSendClick,
        )
    }
}

@Composable
private fun ConversationScreenScaffold(
    modifier: Modifier = Modifier,
    conversationId: String?,
    uiState: ConversationScreenScaffoldUiState,
    isMediaPickerOpen: Boolean,
    messageFieldFocusRequester: FocusRequester,
    onNavigateBack: () -> Unit,
    onOpenMediaPicker: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onPendingAttachmentRemove: (String) -> Unit,
    onResolvedAttachmentClick: (ConversationComposerAttachmentUiState.Resolved) -> Unit,
    onResolvedAttachmentRemove: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ConversationTopAppBar(
                metadata = uiState.metadata,
                onNavigateBack = onNavigateBack,
            )
        },
        bottomBar = {
            if (!isMediaPickerOpen) {
                ConversationComposerSection(
                    attachments = uiState.composer.attachments,
                    messageText = uiState.composer.messageText,
                    isMessageFieldEnabled = uiState.composer.isMessageFieldEnabled,
                    isAttachmentActionEnabled = uiState.composer.isAttachmentActionEnabled,
                    isSendActionEnabled = uiState.composer.isSendEnabled,
                    messageFieldFocusRequester = messageFieldFocusRequester,
                    onAttachmentClick = onOpenMediaPicker,
                    onMessageTextChange = onMessageTextChange,
                    onPendingAttachmentRemove = onPendingAttachmentRemove,
                    onResolvedAttachmentClick = onResolvedAttachmentClick,
                    onResolvedAttachmentRemove = onResolvedAttachmentRemove,
                    onSendClick = onSendClick,
                )
            }
        },
    ) { contentPadding ->
        ConversationScreenContent(
            modifier = Modifier.fillMaxSize(),
            conversationId = conversationId,
            uiState = uiState,
            contentPadding = contentPadding,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
        )
    }
}

@Composable
private fun ConversationScreenContent(
    modifier: Modifier = Modifier,
    conversationId: String?,
    uiState: ConversationScreenScaffoldUiState,
    contentPadding: PaddingValues,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
) {
    when (val messagesState = uiState.messages) {
        is ConversationMessagesUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues = contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG),
                )
            }
        }

        is ConversationMessagesUiState.Present -> {
            val messagesListState = rememberMessagesListState(
                conversationId = conversationId,
            )

            AutoScrollToLatestMessage(
                conversationId = conversationId,
                messages = messagesState.messages,
                listState = messagesListState,
            )

            ConversationMessages(
                modifier = modifier.padding(paddingValues = contentPadding),
                messages = messagesState.messages,
                listState = messagesListState,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
            )
        }
    }
}

@Composable
private fun AutoScrollToLatestMessage(
    conversationId: String?,
    messages: ImmutableList<ConversationMessageUiModel>,
    listState: LazyListState,
) {
    val latestMessage = messages.lastOrNull()
    val latestMessageId = latestMessage?.messageId

    var previousLatestMessageId by remember(conversationId) {
        mutableStateOf(value = latestMessageId)
    }

    var wasScrolledToLatestMessage by remember(
        conversationId,
        listState,
    ) {
        mutableStateOf(
            value = isScrolledToLatestMessage(listState = listState),
        )
    }

    LaunchedEffect(
        conversationId,
        listState,
    ) {
        snapshotFlow {
            isScrolledToLatestMessage(listState = listState)
        }.collect { isScrolledToLatestMessage ->
            wasScrolledToLatestMessage = isScrolledToLatestMessage
        }
    }

    LaunchedEffect(
        conversationId,
        latestMessageId,
    ) {
        val autoScrollDecision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = previousLatestMessageId,
                latestMessageId = latestMessageId,
                hasLatestMessage = latestMessage != null,
                isLatestMessageIncoming = latestMessage?.isIncoming ?: false,
                wasScrolledToLatestMessage = wasScrolledToLatestMessage,
            ),
        )

        previousLatestMessageId = autoScrollDecision.updatedLatestMessageId
        if (!autoScrollDecision.shouldScrollToLatestMessage) {
            return@LaunchedEffect
        }

        listState.animateScrollToItem(index = 0)
    }
}

private fun isScrolledToLatestMessage(listState: LazyListState): Boolean {
    return listState.firstVisibleItemIndex == 0 &&
        listState.firstVisibleItemScrollOffset == 0
}

@Composable
private fun rememberMessagesListState(
    conversationId: String?,
): LazyListState {
    return rememberSaveable(
        conversationId,
        saver = LazyListState.Saver,
    ) {
        LazyListState(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )
    }
}
