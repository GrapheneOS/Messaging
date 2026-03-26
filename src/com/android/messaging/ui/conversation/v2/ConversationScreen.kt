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
        viewModel.conversationId = conversationId
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
                value = "",
                enabled = false,
                onValueChange = {},
                onSendClick = {},
            )
        },
    ) { contentPadding ->
        ConversationScreenContent(
            modifier = Modifier.padding(contentPadding),
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
                initialMessageIndex = messagesState.messages.lastIndex.coerceAtLeast(minimumValue = 0),
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
    initialMessageIndex: Int,
): LazyListState {
    return rememberSaveable(
        conversationId,
        saver = LazyListState.Saver,
    ) {
        LazyListState(
            firstVisibleItemIndex = initialMessageIndex,
            firstVisibleItemScrollOffset = 0,
        )
    }
}
