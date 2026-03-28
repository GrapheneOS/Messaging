package com.android.messaging.ui.conversation.v2.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.attachmentchooser.AttachmentChooserActivity
import com.android.messaging.ui.conversation.v2.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.ui.ConversationComposeBar
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.messages.ui.ConversationMessages
import com.android.messaging.ui.conversation.v2.metadata.ui.ConversationTopAppBar
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationUiState

@Composable
internal fun ConversationScreen(
    modifier: Modifier = Modifier,
    conversationId: String? = null,
    onNavigateBack: () -> Unit = {},
    screenModel: ConversationScreenModel = viewModel<ConversationViewModel>(),
) {
    val context = LocalContext.current
    val attachmentChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {}

    LaunchedEffect(conversationId) {
        screenModel.onConversationChanged(conversationId = conversationId)
    }

    LaunchedEffect(screenModel, context, attachmentChooserLauncher) {
        screenModel.effects.collect { effect ->
            when (effect) {
                is ConversationScreenEffect.LaunchAttachmentChooser -> {
                    val chooserIntent = Intent(
                        context,
                        AttachmentChooserActivity::class.java,
                    ).apply {
                        putExtra(
                            UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID,
                            effect.conversationId,
                        )
                    }

                    attachmentChooserLauncher.launch(chooserIntent)
                }
            }
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        screenModel.persistDraft()
    }

    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

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
                onAttachmentClick = screenModel::onAttachmentClick,
                onMessageTextChange = { messageText ->
                    screenModel.onMessageTextChanged(text = messageText)
                },
                onSendClick = screenModel::onSendClick,
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
                modifier = modifier,
                messages = messagesState.messages,
                listState = messagesListState,
            )
        }
    }
}

@Composable
private fun AutoScrollToLatestMessage(
    conversationId: String?,
    messages: List<ConversationMessageUiModel>,
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
        }
            .collect { isScrolledToLatestMessage ->
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

private fun isScrolledToLatestMessage(
    listState: LazyListState,
): Boolean {
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
