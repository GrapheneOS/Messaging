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
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.common.components.contentSurfaceShape
import com.android.messaging.ui.common.components.rememberIsLoadingIndicatorVisible
import com.android.messaging.ui.conversation.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.messages.ui.ConversationMessages
import com.android.messaging.ui.conversation.messages.ui.attachment.OnConversationAttachmentClick
import com.android.messaging.ui.conversation.messages.ui.conversationMessagesLeadingItemCount
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMessageAction
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.subscription.mapper.resolveDisplayName
import kotlinx.coroutines.flow.filter

private const val LOAD_OLDER_MESSAGES_THRESHOLD = 100

private const val LOAD_NEWER_MESSAGES_THRESHOLD = 100

private data class ConversationLatestScrollSnapshot(
    val isScrolledToLatestMessage: Boolean,
    val isAwayFromLatestMessage: Boolean,
)

// Its own type, so scrolling to the latest message dismisses this notice and leaves others alone.
private class NewMessageSnackbarVisuals(
    override val message: String,
    override val actionLabel: String,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}

// Scaffold updates its padding during measure, so padding() must read it there too; a read in
// composition lands a frame late and lets the list overlap a growing bottom bar.
private class VerticalContentPadding(
    private val contentPadding: PaddingValues,
) : PaddingValues {

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp {
        return 0.dp
    }

    override fun calculateTopPadding(): Dp {
        return contentPadding.calculateTopPadding()
    }

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp {
        return 0.dp
    }

    override fun calculateBottomPadding(): Dp {
        return contentPadding.calculateBottomPadding()
    }
}

@Composable
@Suppress("LongMethod") // Long arguments list, not too much logic, so it's ok to suppress
internal fun ConversationScreenContent(
    modifier: Modifier = Modifier,
    conversationId: ConversationId?,
    uiState: ConversationScreenScaffoldUiState,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    pendingScrollMessageId: MessageId?,
    onPendingScrollMessageIdConsumed: () -> Unit,
    onAttachmentClick: OnConversationAttachmentClick,
    onExternalUriClick: (String) -> Unit,
    onLoadOlderMessages: () -> Unit,
    onLoadNewerMessages: () -> Unit,
    onLoadNewestMessages: () -> Unit,
    onLoadMessage: (MessageId) -> Unit,
    onTargetMessageShown: (MessageId) -> Unit,
    onMessageHighlightFinished: (MessageId) -> Unit,
    onMessageClick: (MessageId) -> Unit,
    onMessageAvatarClick: (MessageId) -> Unit,
    onMessageDownloadClick: (MessageId) -> Unit,
    onMessageActionClick: (MessageId, ConversationMessageAction) -> Unit,
    onMessageLongClick: (MessageId) -> Unit,
    onMessageResendClick: (MessageId) -> Unit,
    onSimSelectorClick: () -> Unit,
    onUnblockClick: () -> Unit,
) {
    val contentBackdropColor = conversationScreenContentBackdropColor(uiState = uiState)

    val messagesState = uiState.messages
    val isContentLoaded = !shouldShowConversationScreenLoadingContent(uiState = uiState)
    val isLoadingIndicatorVisible = rememberIsLoadingIndicatorVisible(isLoading = !isContentLoaded)
    // Kept once the messages are in, for a list that is still placing its target to go on showing.
    val wasLoadingIndicatorVisible = remember(conversationId) { mutableStateOf(value = false) }
    SideEffect {
        if (!isContentLoaded) {
            wasLoadingIndicatorVisible.value = isLoadingIndicatorVisible
        }
    }
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
                    isIndicatorVisible = isLoadingIndicatorVisible,
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
                    pendingScrollMessageId = pendingScrollMessageId,
                    onPendingScrollMessageIdConsumed = onPendingScrollMessageIdConsumed,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onLoadOlderMessages = onLoadOlderMessages,
                    onLoadNewerMessages = onLoadNewerMessages,
                    onLoadNewestMessages = onLoadNewestMessages,
                    onLoadMessage = onLoadMessage,
                    onTargetMessageShown = onTargetMessageShown,
                    onMessageHighlightFinished = onMessageHighlightFinished,
                    onMessageClick = onMessageClick,
                    onMessageAvatarClick = onMessageAvatarClick,
                    onMessageDownloadClick = onMessageDownloadClick,
                    onMessageActionClick = onMessageActionClick,
                    onMessageLongClick = onMessageLongClick,
                    onMessageResendClick = onMessageResendClick,
                    onSimSelectorClick = onSimSelectorClick,
                    additionalTopContentPadding = messagesTopReservation,
                    isLoadingIndicatorVisible = wasLoadingIndicatorVisible.value,
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
    isIndicatorVisible: Boolean,
) {
    Box(
        modifier = modifier.conversationScreenContentModifier(
            contentPadding = contentPadding,
            backdropColor = contentBackdropColor,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (isIndicatorVisible) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG),
            )
        }
    }
}

@Composable
@Suppress("LongMethod") // Long arguments list, not too much logic, so it's ok to suppress
private fun ConversationScreenPresentContent(
    modifier: Modifier,
    conversationId: ConversationId?,
    uiState: ConversationScreenScaffoldUiState,
    messagesState: ConversationMessagesUiState.Present,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    contentBackdropColor: Color,
    pendingScrollMessageId: MessageId?,
    onPendingScrollMessageIdConsumed: () -> Unit,
    onAttachmentClick: OnConversationAttachmentClick,
    onExternalUriClick: (String) -> Unit,
    onLoadOlderMessages: () -> Unit,
    onLoadNewerMessages: () -> Unit,
    onLoadNewestMessages: () -> Unit,
    onLoadMessage: (MessageId) -> Unit,
    onTargetMessageShown: (MessageId) -> Unit,
    onMessageHighlightFinished: (MessageId) -> Unit,
    onMessageClick: (MessageId) -> Unit,
    onMessageAvatarClick: (MessageId) -> Unit,
    onMessageDownloadClick: (MessageId) -> Unit,
    onMessageActionClick: (MessageId, ConversationMessageAction) -> Unit,
    onMessageLongClick: (MessageId) -> Unit,
    onMessageResendClick: (MessageId) -> Unit,
    onSimSelectorClick: () -> Unit,
    additionalTopContentPadding: Dp,
    isLoadingIndicatorVisible: Boolean,
) {
    val showIncomingParticipantIdentity = shouldShowIncomingParticipantIdentity(
        metadata = uiState.metadata,
    )
    val shouldShowSendSimIndicator = uiState.composer.isMessageFieldEnabled &&
        uiState.composer.simSelector.isAvailable

    val currentSendSimDisplayName = when {
        // It sits below the newest message, so not below a window that stops short of it.
        shouldShowSendSimIndicator && !messagesState.hasNewerMessages -> {
            uiState.composer.simSelector.selectedSubscription
                ?.label
                ?.resolveDisplayName()
        }

        else -> null
    }
    val leadingItemCount = conversationMessagesLeadingItemCount(
        currentSendSimDisplayName = currentSendSimDisplayName,
    )
    val targetListIndex = targetMessageListIndex(
        messages = messagesState.messages,
        messageId = pendingScrollMessageId,
        leadingItemCount = leadingItemCount,
    )
    // A list that opens at the target, such as one moved to the window around it, stays behind the
    // loading content until the target is centered, so that it shows up with the target in place.
    val messagesListState = rememberMessagesListState(
        conversationId = conversationId,
        windowId = messagesState.windowId,
        initialFirstVisibleItemIndex = { targetListIndex ?: 0 },
    )
    val isPlacingTarget = remember(messagesListState) {
        mutableStateOf(value = targetListIndex != null)
    }

    AutoScrollToLatestMessage(
        conversationId = conversationId,
        messagesState = messagesState,
        listState = messagesListState,
        snackbarHostState = snackbarHostState,
        onLoadNewestMessages = onLoadNewestMessages,
    )

    ScrollToTargetMessage(
        conversationId = conversationId,
        pendingScrollMessageId = pendingScrollMessageId,
        messagesState = messagesState,
        listState = messagesListState,
        leadingItemCount = leadingItemCount,
        isListUnseen = isPlacingTarget.value,
        onLoadMessage = onLoadMessage,
        onTargetPlaced = { isPlacingTarget.value = false },
        onTargetMessageShown = onTargetMessageShown,
        onConsumed = onPendingScrollMessageIdConsumed,
    )

    EndMessageHighlight(
        highlightedMessageId = messagesState.highlightedMessageId,
        onMessageHighlightFinished = onMessageHighlightFinished,
    )

    LoadOlderMessagesOnScrollBack(
        conversationId = conversationId,
        listState = messagesListState,
        onLoadOlderMessages = onLoadOlderMessages,
    )

    LoadNewerMessagesOnScrollForward(
        conversationId = conversationId,
        listState = messagesListState,
        hasNewerMessages = messagesState.hasNewerMessages,
        // The window was just moved around the target, and the scroll to it starts from its end.
        isEnabled = pendingScrollMessageId == null,
        onLoadNewerMessages = onLoadNewerMessages,
    )

    // A window moved elsewhere shares no messages with the one before, so every message leaving
    // and arriving at once would be animated: a fresh list shows it at once.
    key(messagesState.windowId) {
        ConversationMessages(
            modifier = modifier.conversationScreenContentModifier(
                contentPadding = contentPadding,
                backdropColor = contentBackdropColor,
            ),
            messages = messagesState.messages,
            listState = messagesListState,
            selectedMessageIds = uiState.selection.selectedMessageIds,
            highlightedMessageId = messagesState.highlightedMessageId,
            showIncomingParticipantIdentity = showIncomingParticipantIdentity,
            youTubeLinkPreviewsEnabled = messagesState.youTubeLinkPreviewsEnabled,
            subscriptions = uiState.composer.simSelector.subscriptions,
            currentSendSimDisplayName = currentSendSimDisplayName,
            additionalTopContentPadding = additionalTopContentPadding,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
            onMessageClick = onMessageClick,
            onMessageAvatarClick = onMessageAvatarClick,
            onMessageDownloadClick = onMessageDownloadClick,
            onMessageActionClick = onMessageActionClick,
            onMessageLongClick = onMessageLongClick,
            onMessageResendClick = onMessageResendClick,
            onSimSelectorClick = onSimSelectorClick,
        )
    }

    if (isPlacingTarget.value && pendingScrollMessageId != null) {
        ConversationScreenLoadingContent(
            modifier = modifier,
            contentPadding = contentPadding,
            contentBackdropColor = contentBackdropColor,
            isIndicatorVisible = isLoadingIndicatorVisible,
        )
    }
}

@Composable
private fun Modifier.conversationScreenContentModifier(
    contentPadding: PaddingValues,
    backdropColor: Color,
): Modifier {
    val verticalContentPadding = remember(contentPadding) {
        VerticalContentPadding(contentPadding = contentPadding)
    }

    return this
        .padding(paddingValues = verticalContentPadding)
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

/**
 * While the window stops short of the newest message, that message is not in the list to scroll
 * to: the newest messages replace the window instead.
 */
@Composable
private fun AutoScrollToLatestMessage(
    conversationId: ConversationId?,
    messagesState: ConversationMessagesUiState.Present,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onLoadNewestMessages: () -> Unit,
) {
    val hasNewerMessages = messagesState.hasNewerMessages
    val currentHasNewerMessages = rememberUpdatedState(newValue = hasNewerMessages)
    val latestMessage = messagesState.latestMessage()
    val latestMessageId = latestMessage?.messageId
    val newMessageText = stringResource(id = R.string.in_conversation_notify_new_message_text)
    val viewActionLabel = stringResource(id = R.string.in_conversation_notify_new_message_action)

    var previousLatestMessageId by remember(conversationId) {
        mutableStateOf(value = latestMessageId)
    }

    val showLatestMessage = rememberShowLatestMessage(
        hasNewerMessages = hasNewerMessages,
        listState = listState,
        onLoadNewestMessages = onLoadNewestMessages,
    )

    val isListDraggedState = listState.interactionSource.collectIsDraggedAsState()
    val isListDragged = isListDraggedState.value
    val wasScrolledToLatestMessage = rememberWasScrolledToLatestMessage(
        conversationId = conversationId,
        listState = listState,
        isListDraggedState = isListDraggedState,
        hasNewerMessagesState = currentHasNewerMessages,
        snackbarHostState = snackbarHostState,
    )

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
        if (autoScrollDecision.shouldScrollToLatestMessage && !hasNewerMessages && !isListDragged) {
            listState.requestScrollToItem(index = 0)
        }
    }

    LaunchedEffect(
        conversationId,
        latestMessageId,
    ) {
        previousLatestMessageId = autoScrollDecision.updatedLatestMessageId

        if (autoScrollDecision.shouldScrollToLatestMessage && hasNewerMessages) {
            showLatestMessage()
        }

        if (autoScrollDecision.shouldShowNewMessageSnackbar) {
            val snackbarResult = snackbarHostState.showSnackbar(
                visuals = NewMessageSnackbarVisuals(
                    message = newMessageText,
                    actionLabel = viewActionLabel,
                ),
            )

            if (snackbarResult == SnackbarResult.ActionPerformed) {
                showLatestMessage()
            }
        }
    }
}

/** Past a window that stops short of it, the latest message is not one of those loaded. */
private fun ConversationMessagesUiState.Present.latestMessage(): ConversationMessageUiModel? {
    return when {
        hasNewerMessages -> latestUnloadedMessage
        else -> messages.lastOrNull()
    }
}

/**
 * Scrolls to the latest message, or past a window that stops short of it asks for the newest
 * messages in its place, which a fresh list shows from their end.
 */
@Composable
private fun rememberShowLatestMessage(
    hasNewerMessages: Boolean,
    listState: LazyListState,
    onLoadNewestMessages: () -> Unit,
): suspend () -> Unit {
    val currentHasNewerMessages = rememberUpdatedState(newValue = hasNewerMessages)
    val currentOnLoadNewestMessages = rememberUpdatedState(newValue = onLoadNewestMessages)

    return remember(listState) {
        suspend {
            when {
                currentHasNewerMessages.value -> currentOnLoadNewestMessages.value()
                else -> listState.animateScrollToItem(index = 0)
            }
        }
    }
}

/** Whether the list was last at the latest message, or moved away from it since. */
@Composable
private fun rememberWasScrolledToLatestMessage(
    conversationId: ConversationId?,
    listState: LazyListState,
    isListDraggedState: State<Boolean>,
    hasNewerMessagesState: State<Boolean>,
    snackbarHostState: SnackbarHostState,
): Boolean {
    var wasScrolledToLatestMessage by remember(
        conversationId,
        listState,
    ) {
        mutableStateOf(
            value = !hasNewerMessagesState.value &&
                isScrolledToLatestMessage(listState = listState),
        )
    }

    LaunchedEffect(
        conversationId,
        listState,
    ) {
        snapshotFlow {
            val hasNewerMessages = hasNewerMessagesState.value

            ConversationLatestScrollSnapshot(
                isScrolledToLatestMessage = !hasNewerMessages &&
                    isScrolledToLatestMessage(listState = listState),
                // The list keeping up with a new message moves it without a scroll, which is why
                // being off the latest message on its own does not count.
                isAwayFromLatestMessage = hasNewerMessages ||
                    isListDraggedState.value ||
                    listState.isScrollInProgress,
            )
        }.collect { scrollSnapshot ->
            when {
                scrollSnapshot.isScrolledToLatestMessage -> {
                    wasScrolledToLatestMessage = true
                    snackbarHostState.currentSnackbarData
                        ?.takeIf { it.visuals is NewMessageSnackbarVisuals }
                        ?.dismiss()
                }

                scrollSnapshot.isAwayFromLatestMessage -> {
                    wasScrolledToLatestMessage = false
                }
            }
        }
    }

    return wasScrolledToLatestMessage
}

private fun isScrolledToLatestMessage(listState: LazyListState): Boolean {
    return listState.firstVisibleItemIndex == 0 &&
        listState.firstVisibleItemScrollOffset == 0
}

/**
 * The list is reversed, so the last visible item is the oldest loaded message. Getting close to it
 * asks for a larger window; the delegate ignores the request when the whole conversation is loaded.
 */
@Composable
private fun LoadOlderMessagesOnScrollBack(
    conversationId: ConversationId?,
    listState: LazyListState,
    onLoadOlderMessages: () -> Unit,
) {
    LaunchedEffect(
        conversationId,
        listState,
    ) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index

            // Before the first measure pass nothing is visible yet, and asking then would grow the
            // window on every conversation open.
            lastVisibleItemIndex != null &&
                lastVisibleItemIndex >= layoutInfo.totalItemsCount - LOAD_OLDER_MESSAGES_THRESHOLD
        }
            .filter { isNearOldestLoadedMessage -> isNearOldestLoadedMessage }
            .collect { onLoadOlderMessages() }
    }
}

/**
 * The first visible item is the newest loaded message. While the window stops short of the newest
 * message of the conversation, getting close to it asks for the messages after it.
 */
@Composable
private fun LoadNewerMessagesOnScrollForward(
    conversationId: ConversationId?,
    listState: LazyListState,
    hasNewerMessages: Boolean,
    isEnabled: Boolean,
    onLoadNewerMessages: () -> Unit,
) {
    LaunchedEffect(
        conversationId,
        listState,
        hasNewerMessages,
        isEnabled,
    ) {
        if (!hasNewerMessages || !isEnabled) {
            return@LaunchedEffect
        }

        snapshotFlow {
            val firstVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index

            firstVisibleItemIndex != null &&
                firstVisibleItemIndex < LOAD_NEWER_MESSAGES_THRESHOLD
        }
            .filter { isNearNewestLoadedMessage -> isNearNewestLoadedMessage }
            .collect { onLoadNewerMessages() }
    }
}

@Composable
private fun rememberMessagesListState(
    conversationId: ConversationId?,
    windowId: Int,
    initialFirstVisibleItemIndex: () -> Int,
): LazyListState {
    return rememberSaveable(
        conversationId,
        windowId,
        saver = LazyListState.Saver,
    ) {
        LazyListState(
            firstVisibleItemIndex = initialFirstVisibleItemIndex(),
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
