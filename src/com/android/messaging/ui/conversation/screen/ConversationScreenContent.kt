package com.android.messaging.ui.conversation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.contentSurfaceShape
import com.android.messaging.ui.conversation.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.messages.ui.ConversationMessages
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.subscription.mapper.resolveDisplayName
import kotlinx.collections.immutable.ImmutableList

private const val SMOOTH_SCROLL_JUMP_THRESHOLD = 15

private data class ConversationLatestScrollSnapshot(
    val isScrolledToLatestMessage: Boolean,
    val isListDragged: Boolean,
)

@Composable
internal fun ConversationScreenContent(
    modifier: Modifier = Modifier,
    conversationId: String?,
    uiState: ConversationScreenScaffoldUiState,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    pendingScrollPosition: Int?,
    onPendingScrollPositionConsumed: () -> Unit,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onMessageAvatarClick: (String) -> Unit,
    onMessageDownloadClick: (String) -> Unit,
    onMessageLongClick: (String) -> Unit,
    onMessageResendClick: (String) -> Unit,
    onSimSelectorClick: () -> Unit,
    onUnblockClick: () -> Unit,
) {
    val contentBackdropColor = conversationScreenContentBackdropColor(uiState = uiState)

    val messagesState = uiState.messages
    val isContentLoaded = !shouldShowConversationScreenLoadingContent(uiState = uiState)
    val isBannerRevealed = rememberBlockedBannerRevealState(
        conversationId = conversationId,
        isBlocked = uiState.isBlocked,
        isContentLoaded = isContentLoaded,
    )

    var bannerHeight by remember { mutableStateOf(value = 0.dp) }
    val messagesTopReservation = when {
        isBannerRevealed -> bannerHeight
        else -> 0.dp
    }

    Box(modifier = modifier) {
        when {
            shouldShowConversationScreenLoadingContent(uiState = uiState) -> {
                ConversationScreenLoadingContent(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    contentBackdropColor = contentBackdropColor,
                )
            }

            messagesState is ConversationMessagesUiState.Present -> {
                ConversationScreenPresentContent(
                    modifier = Modifier.fillMaxSize(),
                    conversationId = conversationId,
                    uiState = uiState,
                    messagesState = messagesState,
                    snackbarHostState = snackbarHostState,
                    contentPadding = contentPadding,
                    contentBackdropColor = contentBackdropColor,
                    pendingScrollPosition = pendingScrollPosition,
                    onPendingScrollPositionConsumed = onPendingScrollPositionConsumed,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onMessageClick = onMessageClick,
                    onMessageAvatarClick = onMessageAvatarClick,
                    onMessageDownloadClick = onMessageDownloadClick,
                    onMessageLongClick = onMessageLongClick,
                    onMessageResendClick = onMessageResendClick,
                    onSimSelectorClick = onSimSelectorClick,
                    additionalTopContentPadding = messagesTopReservation,
                )
            }
        }

        ConversationBlockedBannerSlot(
            modifier = Modifier
                .align(alignment = Alignment.TopCenter)
                .padding(top = contentPadding.calculateTopPadding()),
            isRevealed = isBannerRevealed,
            onUnblockClick = onUnblockClick,
            onHeightChanged = { height -> bannerHeight = height },
        )
    }
}

private fun shouldShowConversationScreenLoadingContent(
    uiState: ConversationScreenScaffoldUiState,
): Boolean {
    return uiState.messages is ConversationMessagesUiState.Loading ||
        uiState.composer.simSelector.isLoading
}

@Composable
private fun ConversationScreenLoadingContent(
    modifier: Modifier,
    contentPadding: PaddingValues,
    contentBackdropColor: Color,
) {
    Box(
        modifier = modifier.conversationScreenContentModifier(
            contentPadding = contentPadding,
            backdropColor = contentBackdropColor,
        ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG),
        )
    }
}

@Composable
private fun ConversationScreenPresentContent(
    modifier: Modifier,
    conversationId: String?,
    uiState: ConversationScreenScaffoldUiState,
    messagesState: ConversationMessagesUiState.Present,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    contentBackdropColor: Color,
    pendingScrollPosition: Int?,
    onPendingScrollPositionConsumed: () -> Unit,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onMessageAvatarClick: (String) -> Unit,
    onMessageDownloadClick: (String) -> Unit,
    onMessageLongClick: (String) -> Unit,
    onMessageResendClick: (String) -> Unit,
    onSimSelectorClick: () -> Unit,
    additionalTopContentPadding: Dp,
) {
    val messagesListState = rememberMessagesListState(conversationId = conversationId)
    val showIncomingParticipantIdentity = shouldShowIncomingParticipantIdentity(
        metadata = uiState.metadata,
    )
    val shouldShowSendSimIndicator = uiState.composer.isMessageFieldEnabled &&
        uiState.composer.simSelector.isAvailable

    val currentSendSimDisplayName = when {
        shouldShowSendSimIndicator -> {
            uiState.composer.simSelector.selectedSubscription
                ?.label
                ?.resolveDisplayName()
        }

        else -> null
    }

    AutoScrollToLatestMessage(
        conversationId = conversationId,
        messages = messagesState.messages,
        listState = messagesListState,
        snackbarHostState = snackbarHostState,
    )

    ScrollToTargetMessage(
        conversationId = conversationId,
        pendingScrollPosition = pendingScrollPosition,
        messages = messagesState.messages,
        listState = messagesListState,
        onConsumed = onPendingScrollPositionConsumed,
    )

    ConversationMessages(
        modifier = modifier.conversationScreenContentModifier(
            contentPadding = contentPadding,
            backdropColor = contentBackdropColor,
        ),
        messages = messagesState.messages,
        listState = messagesListState,
        selectedMessageIds = uiState.selection.selectedMessageIds,
        showIncomingParticipantIdentity = showIncomingParticipantIdentity,
        subscriptions = uiState.composer.simSelector.subscriptions,
        currentSendSimDisplayName = currentSendSimDisplayName,
        additionalTopContentPadding = additionalTopContentPadding,
        onAttachmentClick = onAttachmentClick,
        onExternalUriClick = onExternalUriClick,
        onMessageClick = onMessageClick,
        onMessageAvatarClick = onMessageAvatarClick,
        onMessageDownloadClick = onMessageDownloadClick,
        onMessageLongClick = onMessageLongClick,
        onMessageResendClick = onMessageResendClick,
        onSimSelectorClick = onSimSelectorClick,
    )
}

@Composable
private fun Modifier.conversationScreenContentModifier(
    contentPadding: PaddingValues,
    backdropColor: Color,
): Modifier {
    return this
        .padding(paddingValues = contentPadding)
        .background(color = backdropColor)
        .clip(shape = MaterialTheme.contentSurfaceShape)
        .background(color = MaterialTheme.colorScheme.background)
}

@Composable
private fun conversationScreenContentBackdropColor(
    uiState: ConversationScreenScaffoldUiState,
): Color {
    return when {
        uiState.selection.isSelectionMode -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
}

private fun shouldShowIncomingParticipantIdentity(
    metadata: ConversationMetadataUiState,
): Boolean {
    return when (metadata) {
        is ConversationMetadataUiState.Present -> metadata.participantCount > 1

        ConversationMetadataUiState.Loading,
        ConversationMetadataUiState.Unavailable,
        -> false
    }
}

@Composable
private fun AutoScrollToLatestMessage(
    conversationId: String?,
    messages: ImmutableList<ConversationMessageUiModel>,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
) {
    val latestMessage = messages.lastOrNull()
    val latestMessageId = latestMessage?.messageId
    val newMessageText = stringResource(id = R.string.in_conversation_notify_new_message_text)
    val viewActionLabel = stringResource(id = R.string.in_conversation_notify_new_message_action)

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

    val isListDraggedState = listState.interactionSource.collectIsDraggedAsState()
    val isListDragged = isListDraggedState.value

    val autoScrollDecision = evaluateConversationAutoScroll(
        input = ConversationAutoScrollInput(
            previousLatestMessageId = previousLatestMessageId,
            latestMessageId = latestMessageId,
            hasLatestMessage = latestMessage != null,
            isLatestMessageIncoming = latestMessage?.isIncoming ?: false,
            wasScrolledToLatestMessage = wasScrolledToLatestMessage,
        ),
    )

    SideEffect {
        if (autoScrollDecision.shouldScrollToLatestMessage && !isListDragged) {
            listState.requestScrollToItem(index = 0)
        }
    }

    TrackLatestMessageScrollState(
        conversationId = conversationId,
        listState = listState,
        isListDraggedState = isListDraggedState,
        snackbarHostState = snackbarHostState,
        onWasScrolledToLatestMessageChanged = { isScrolledToLatestMessage ->
            wasScrolledToLatestMessage = isScrolledToLatestMessage
        },
    )

    LaunchedEffect(
        conversationId,
        latestMessageId,
    ) {
        previousLatestMessageId = autoScrollDecision.updatedLatestMessageId

        if (autoScrollDecision.shouldShowNewMessageSnackbar) {
            val snackbarResult = snackbarHostState.showSnackbar(
                message = newMessageText,
                actionLabel = viewActionLabel,
                duration = SnackbarDuration.Indefinite,
            )

            if (snackbarResult == SnackbarResult.ActionPerformed) {
                listState.animateScrollToItem(index = 0)
            }
        }
    }
}

@Composable
private fun TrackLatestMessageScrollState(
    conversationId: String?,
    listState: LazyListState,
    isListDraggedState: State<Boolean>,
    snackbarHostState: SnackbarHostState,
    onWasScrolledToLatestMessageChanged: (Boolean) -> Unit,
) {
    LaunchedEffect(
        conversationId,
        listState,
    ) {
        snapshotFlow {
            ConversationLatestScrollSnapshot(
                isScrolledToLatestMessage = isScrolledToLatestMessage(listState = listState),
                isListDragged = isListDraggedState.value,
            )
        }.collect { scrollSnapshot ->
            when {
                scrollSnapshot.isScrolledToLatestMessage -> {
                    onWasScrolledToLatestMessageChanged(true)
                    snackbarHostState.currentSnackbarData?.dismiss()
                }

                scrollSnapshot.isListDragged -> {
                    onWasScrolledToLatestMessageChanged(false)
                }
            }
        }
    }
}

private fun isScrolledToLatestMessage(listState: LazyListState): Boolean {
    return listState.firstVisibleItemIndex == 0 &&
        listState.firstVisibleItemScrollOffset == 0
}

@Composable
private fun ScrollToTargetMessage(
    conversationId: String?,
    pendingScrollPosition: Int?,
    messages: ImmutableList<ConversationMessageUiModel>,
    listState: LazyListState,
    onConsumed: () -> Unit,
) {
    LaunchedEffect(
        conversationId,
        pendingScrollPosition,
        messages.size,
    ) {
        if (pendingScrollPosition == null || messages.isEmpty()) {
            return@LaunchedEffect
        }

        val displayIndex = messagePositionToDisplayIndex(
            position = pendingScrollPosition,
            size = messages.size,
        )

        val firstVisible = listState.firstVisibleItemIndex
        val delta = displayIndex - firstVisible

        val intermediateIndex = when {
            delta > SMOOTH_SCROLL_JUMP_THRESHOLD -> displayIndex - SMOOTH_SCROLL_JUMP_THRESHOLD
            delta < -SMOOTH_SCROLL_JUMP_THRESHOLD -> displayIndex + SMOOTH_SCROLL_JUMP_THRESHOLD
            else -> -1
        }

        if (intermediateIndex != -1) {
            listState.scrollToItem(index = intermediateIndex.coerceIn(0, messages.size - 1))
        }

        listState.animateScrollToItem(index = displayIndex)
        onConsumed()
    }
}

internal fun messagePositionToDisplayIndex(position: Int, size: Int): Int {
    return when {
        size <= 0 -> 0

        else -> {
            val lastIndex = size - 1
            (lastIndex - position).coerceIn(0, lastIndex)
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

@PreviewLightDark
@Composable
private fun ConversationScreenContentLoadingPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentLoadingUiState(),
    )
}

@PreviewLightDark
@Composable
private fun ConversationScreenContentSimLoadingPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentSimLoadingUiState(),
    )
}

@PreviewLightDark
@Composable
private fun ConversationScreenContentEmptyPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentEmptyUiState(),
    )
}

@PreviewLightDark
@Composable
private fun ConversationScreenContentDirectConversationPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentDirectUiState(),
    )
}

@PreviewLightDark
@Composable
private fun ConversationScreenContentPresentPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentPresentUiState(),
    )
}

@PreviewLightDark
@Composable
private fun ConversationScreenContentGroupRichPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentGroupRichUiState(),
    )
}

@PreviewLightDark
@Composable
private fun ConversationScreenContentSelectionPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentSelectionUiState(),
    )
}

@PreviewLightDark
@Composable
private fun ConversationScreenContentDateSeparatedPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentDateSeparatedUiState(),
    )
}

@PreviewLightDark
@Composable
private fun ConversationScreenContentMmsDownloadPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentMmsDownloadUiState(),
    )
}

@PreviewLightDark
@Composable
private fun ConversationScreenContentNoSendSimPreview() {
    ConversationScreenContentPreview(
        uiState = previewConversationScreenContentNoSendSimUiState(),
        conversationId = null,
    )
}
