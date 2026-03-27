package com.android.messaging.ui.conversation.v2.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.ui.conversation.v2.composer.ui.ConversationComposeBar
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.messages.ui.ConversationMessages
import com.android.messaging.ui.conversation.v2.metadata.ui.ConversationTopAppBar

@Composable
internal fun ConversationScreen(
    modifier: Modifier = Modifier,
    conversationId: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: ConversationViewModel = viewModel(),
) {
    LaunchedEffect(conversationId) {
        viewModel.onConversationChanged(conversationId = conversationId)
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        viewModel.persistDraft()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ConversationTopAppBar(
                metadata = uiState.metadata,
                onNavigateBack = onNavigateBack,
            )
        },
        bottomBar = {
            ConversationComposeBar(
                messageText = uiState.composer.messageText,
                isMessageFieldEnabled = uiState.composer.isMessageFieldEnabled,
                isAttachmentActionEnabled = uiState.composer.isAttachmentActionEnabled,
                isSendActionEnabled = uiState.composer.isSendEnabled,
                onAttachmentClick = viewModel::onAttachmentClick,
                onMessageTextChange = { messageText ->
                    viewModel.onMessageTextChanged(text = messageText)
                },
                onSendClick = viewModel::onSendClick,
            )
        },
    ) { contentPadding ->
        ConversationScreenContent(
            modifier = Modifier.padding(paddingValues = contentPadding),
            conversationId = conversationId,
            uiState = uiState,
        )
    }
}

@Composable
private fun ConversationScreenContent(
    modifier: Modifier = Modifier,
    conversationId: String?,
    uiState: ConversationUiState,
) {
    when (val messagesState = uiState.messages) {
        is ConversationMessagesUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is ConversationMessagesUiState.Present -> {
            val messagesListState = rememberMessagesListState(
                conversationId = conversationId,
            )

            ConversationMessages(
                modifier = modifier,
                messages = messagesState.messages,
                listState = messagesListState,
            )
        }
    }
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
