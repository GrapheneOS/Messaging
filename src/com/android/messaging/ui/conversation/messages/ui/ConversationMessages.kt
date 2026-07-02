package com.android.messaging.ui.conversation.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.ui.conversation.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageItemTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.ui.message.ConversationMessage
import com.android.messaging.ui.conversation.messages.ui.message.conversationMessageDisplayEpochDay
import com.android.messaging.ui.conversation.messages.ui.message.formatDateSeparatorText
import com.android.messaging.ui.conversation.messages.ui.message.resolveConversationMessageSimDisplayName
import com.android.messaging.ui.conversation.preview.previewMessages
import com.android.messaging.ui.conversation.preview.previewSubscriptions
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.subscription.mapper.resolveDisplayName
import java.util.TimeZone
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableMap

private val MESSAGES_CONTENT_HORIZONTAL_PADDING = 16.dp
private val MESSAGES_CONTENT_TOP_PADDING = 24.dp

private val messagesContentPadding = PaddingValues(
    start = MESSAGES_CONTENT_HORIZONTAL_PADDING,
    top = MESSAGES_CONTENT_TOP_PADDING,
    end = MESSAGES_CONTENT_HORIZONTAL_PADDING,
    bottom = 24.dp,
)
private val sendSimContentPadding = PaddingValues(
    start = MESSAGES_CONTENT_HORIZONTAL_PADDING,
    top = MESSAGES_CONTENT_TOP_PADDING,
    end = MESSAGES_CONTENT_HORIZONTAL_PADDING,
    bottom = 6.dp,
)

private val MESSAGES_CLUSTER_TOP_PADDING = 2.dp
private val MESSAGES_GROUP_TOP_PADDING = 12.dp
private val sendSimPadding = PaddingValues(
    horizontal = 16.dp,
    vertical = 4.dp,
)

private const val SEND_SIM_INDICATOR_ITEM_KEY = "send_sim_indicator"

private enum class ConversationMessagesItemContentType {
    Message,
    MessageWithDateSeparator,
    SendSimIndicator,
}

@Composable
internal fun ConversationMessages(
    modifier: Modifier = Modifier,
    messages: ImmutableList<ConversationMessageUiModel>,
    listState: LazyListState,
    selectedMessageIds: ImmutableSet<String> = persistentSetOf(),
    showIncomingParticipantIdentity: Boolean = true,
    subscriptions: ImmutableList<Subscription> = persistentListOf(),
    currentSendSimDisplayName: String? = null,
    additionalTopContentPadding: Dp = 0.dp,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onMessageAvatarClick: (String) -> Unit,
    onMessageDownloadClick: (String) -> Unit,
    onMessageLongClick: (String) -> Unit,
    onMessageResendClick: (String) -> Unit,
    onSimSelectorClick: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val displayMessages = remember(messages) {
        messages.asReversed()
    }
    val timeZone = remember(configuration) {
        TimeZone.getDefault()
    }
    val simDisplayNameByParticipantId = rememberSimDisplayNameByParticipantId(
        subscriptions = subscriptions,
    )
    val isSelectionMode = selectedMessageIds.isNotEmpty()
    val shouldShowSendSimIndicator = !currentSendSimDisplayName.isNullOrBlank()

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = modifier
            .fillMaxSize()
            .testTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .background(color = MaterialTheme.colorScheme.background),
        contentPadding = conversationMessagesContentPadding(
            shouldShowSendSimIndicator = shouldShowSendSimIndicator,
            additionalTopContentPadding = additionalTopContentPadding,
        ),
    ) {
        conversationSendSimIndicatorItem(
            currentSendSimDisplayName = currentSendSimDisplayName,
            shouldShowSendSimIndicator = shouldShowSendSimIndicator,
            isSelectionMode = isSelectionMode,
            onSimSelectorClick = onSimSelectorClick,
        )

        conversationMessageItems(
            displayMessages = displayMessages,
            timeZone = timeZone,
            selectedMessageIds = selectedMessageIds,
            isSelectionMode = isSelectionMode,
            showIncomingParticipantIdentity = showIncomingParticipantIdentity,
            simDisplayNameByParticipantId = simDisplayNameByParticipantId,
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
}

private fun LazyListScope.conversationMessageItems(
    displayMessages: List<ConversationMessageUiModel>,
    timeZone: TimeZone,
    selectedMessageIds: ImmutableSet<String>,
    isSelectionMode: Boolean,
    showIncomingParticipantIdentity: Boolean,
    simDisplayNameByParticipantId: ImmutableMap<String, String>,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onMessageAvatarClick: (String) -> Unit,
    onMessageDownloadClick: (String) -> Unit,
    onMessageLongClick: (String) -> Unit,
    onMessageResendClick: (String) -> Unit,
    onSimSelectorClick: () -> Unit,
) {
    itemsIndexed(
        items = displayMessages,
        key = { _, message -> message.messageId },
        contentType = { index, _ ->
            conversationMessagesItemContentType(
                messages = displayMessages,
                index = index,
                timeZone = timeZone,
            )
        },
    ) { index, message ->
        ConversationMessagesItem(
            modifier = Modifier
                .animateItem(),
            message = message,
            messageAbove = messageAboveCurrent(messages = displayMessages, index = index),
            messageBelow = messageBelowCurrent(messages = displayMessages, index = index),
            isSelectionMode = isSelectionMode,
            isSelected = selectedMessageIds.contains(message.messageId),
            showIncomingParticipantIdentity = showIncomingParticipantIdentity,
            simDisplayNameByParticipantId = simDisplayNameByParticipantId,
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
}

private fun LazyListScope.conversationSendSimIndicatorItem(
    currentSendSimDisplayName: String?,
    shouldShowSendSimIndicator: Boolean,
    isSelectionMode: Boolean,
    onSimSelectorClick: () -> Unit,
) {
    if (!shouldShowSendSimIndicator || currentSendSimDisplayName.isNullOrBlank()) {
        return
    }

    item(
        key = SEND_SIM_INDICATOR_ITEM_KEY,
        contentType = ConversationMessagesItemContentType.SendSimIndicator,
    ) {
        ConversationSendSimIndicator(
            modifier = Modifier.animateItem(),
            simDisplayName = currentSendSimDisplayName,
            isSimSelectorEnabled = !isSelectionMode,
            onSimSelectorClick = onSimSelectorClick,
        )
    }
}

private fun conversationMessagesContentPadding(
    shouldShowSendSimIndicator: Boolean,
    additionalTopContentPadding: Dp,
): PaddingValues {
    val basePadding = when {
        shouldShowSendSimIndicator -> sendSimContentPadding
        else -> messagesContentPadding
    }

    if (additionalTopContentPadding <= 0.dp) {
        return basePadding
    }

    return PaddingValues(
        start = MESSAGES_CONTENT_HORIZONTAL_PADDING,
        top = MESSAGES_CONTENT_TOP_PADDING + additionalTopContentPadding,
        end = MESSAGES_CONTENT_HORIZONTAL_PADDING,
        bottom = basePadding.calculateBottomPadding(),
    )
}

@Composable
private fun rememberSimDisplayNameByParticipantId(
    subscriptions: ImmutableList<Subscription>,
): ImmutableMap<String, String> {
    val resources = LocalResources.current

    return remember(subscriptions, resources) {
        subscriptions
            .associate { subscription ->
                subscription.selfParticipantId to subscription.label.resolveDisplayName(
                    resources = resources,
                )
            }
            .toImmutableMap()
    }
}

@Immutable
private data class ConversationMessagesItemPresentation(
    val showDateSeparator: Boolean,
    val dateSeparatorText: String?,
    val topPadding: Dp,
)

private fun conversationMessagesItemContentType(
    messages: List<ConversationMessageUiModel>,
    index: Int,
    timeZone: TimeZone,
): ConversationMessagesItemContentType {
    val shouldShowDateSeparator = shouldShowDateSeparator(
        currentMessage = messages[index],
        messageAbove = messageAboveCurrent(
            messages = messages,
            index = index,
        ),
        timeZone = timeZone,
    )

    return when {
        shouldShowDateSeparator -> ConversationMessagesItemContentType.MessageWithDateSeparator
        else -> ConversationMessagesItemContentType.Message
    }
}

private fun messageAboveCurrent(
    messages: List<ConversationMessageUiModel>,
    index: Int,
): ConversationMessageUiModel? {
    return messages.getOrNull(index + 1)
}

private fun messageBelowCurrent(
    messages: List<ConversationMessageUiModel>,
    index: Int,
): ConversationMessageUiModel? {
    return messages.getOrNull(index - 1)
}

@Composable
private fun ConversationMessagesItem(
    modifier: Modifier,
    message: ConversationMessageUiModel,
    messageAbove: ConversationMessageUiModel?,
    messageBelow: ConversationMessageUiModel?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    showIncomingParticipantIdentity: Boolean,
    simDisplayNameByParticipantId: ImmutableMap<String, String>,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onMessageAvatarClick: (String) -> Unit,
    onMessageDownloadClick: (String) -> Unit,
    onMessageLongClick: (String) -> Unit,
    onMessageResendClick: (String) -> Unit,
    onSimSelectorClick: () -> Unit,
) {
    val presentation = rememberConversationMessagesItemPresentation(
        message = message,
        messageAbove = messageAbove,
    )

    val simDisplayName = remember(message, messageBelow, simDisplayNameByParticipantId) {
        resolveConversationMessageSimDisplayName(
            message = message,
            messageBelow = messageBelow,
            simDisplayNameByParticipantId = simDisplayNameByParticipantId,
        )
    }

    ColumnWithSeparator(
        modifier = modifier,
        showDateSeparator = presentation.showDateSeparator,
        dateSeparatorText = presentation.dateSeparatorText,
    ) {
        ConversationMessage(
            modifier = Modifier
                .testTag(conversationMessageItemTestTag(messageId = message.messageId))
                .padding(top = presentation.topPadding),
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            message = message,
            showIncomingParticipantIdentity = showIncomingParticipantIdentity,
            simDisplayName = simDisplayName,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
            onMessageClick = {
                onMessageClick(message.messageId)
            },
            onMessageAvatarClick = {
                onMessageAvatarClick(message.messageId)
            },
            onMessageDownloadClick = {
                onMessageDownloadClick(message.messageId)
            },
            onMessageLongClick = {
                onMessageLongClick(message.messageId)
            },
            onMessageResendClick = {
                onMessageResendClick(message.messageId)
            },
            onSimSelectorClick = onSimSelectorClick,
        )
    }
}

@Composable
private fun ConversationSendSimIndicator(
    modifier: Modifier,
    simDisplayName: String,
    isSimSelectorEnabled: Boolean,
    onSimSelectorClick: () -> Unit,
) {
    val linkColor = MaterialTheme.colorScheme.primary

    val resources = LocalResources.current

    val annotatedText = remember(
        simDisplayName,
        linkColor,
        resources,
        onSimSelectorClick,
        isSimSelectorEnabled,
    ) {
        buildConversationSimLinkAnnotatedString(
            simDisplayName = simDisplayName,
            annotationTemplate = resources.getString(R.string.conversation_send_sim_annotation),
            linkColor = linkColor,
            onSimSelectorClick = onSimSelectorClick,
            isLinkEnabled = isSimSelectorEnabled,
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.padding(paddingValues = sendSimPadding),
            text = annotatedText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun rememberConversationMessagesItemPresentation(
    message: ConversationMessageUiModel,
    messageAbove: ConversationMessageUiModel?,
): ConversationMessagesItemPresentation {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val timeZone = remember(configuration) {
        TimeZone.getDefault()
    }

    val showDateSeparator = remember(
        timeZone,
        message.displayTimestamp,
        messageAbove?.displayTimestamp,
    ) {
        shouldShowDateSeparator(
            currentMessage = message,
            messageAbove = messageAbove,
            timeZone = timeZone,
        )
    }

    val dateSeparatorText = remember(
        context,
        configuration,
        showDateSeparator,
        message.displayTimestamp,
    ) {
        if (!showDateSeparator) {
            null
        } else {
            formatDateSeparatorText(
                context = context,
                message = message,
            )
        }
    }

    val topPadding = remember(
        showDateSeparator,
        messageAbove,
        message.canClusterWithPrevious,
    ) {
        messageItemTopPadding(
            message = message,
            messageAbove = messageAbove,
            showDateSeparator = showDateSeparator,
        )
    }

    return remember(
        showDateSeparator,
        dateSeparatorText,
        topPadding,
    ) {
        ConversationMessagesItemPresentation(
            showDateSeparator = showDateSeparator,
            dateSeparatorText = dateSeparatorText,
            topPadding = topPadding,
        )
    }
}

private fun messageItemTopPadding(
    message: ConversationMessageUiModel,
    messageAbove: ConversationMessageUiModel?,
    showDateSeparator: Boolean,
): Dp {
    return when {
        messageAbove == null || showDateSeparator -> 0.dp
        message.canClusterWithPrevious -> MESSAGES_CLUSTER_TOP_PADDING
        else -> MESSAGES_GROUP_TOP_PADDING
    }
}

private fun shouldShowDateSeparator(
    currentMessage: ConversationMessageUiModel,
    messageAbove: ConversationMessageUiModel?,
    timeZone: TimeZone,
): Boolean {
    return when (messageAbove) {
        null -> true

        else -> {
            shouldShowDateSeparatorBetweenMessages(
                currentMessage = currentMessage,
                messageAbove = messageAbove,
                timeZone = timeZone,
            )
        }
    }
}

private fun shouldShowDateSeparatorBetweenMessages(
    currentMessage: ConversationMessageUiModel,
    messageAbove: ConversationMessageUiModel,
    timeZone: TimeZone,
): Boolean {
    val currentEpochDay = conversationMessageDisplayEpochDay(
        displayTimestamp = currentMessage.displayTimestamp,
        timeZone = timeZone,
    ) ?: return false

    val messageAboveEpochDay = conversationMessageDisplayEpochDay(
        displayTimestamp = messageAbove.displayTimestamp,
        timeZone = timeZone,
    )

    return messageAboveEpochDay != currentEpochDay
}

@PreviewLightDark
@Composable
private fun ConversationMessagesPreview() {
    MessagingPreviewTheme {
        ConversationMessages(
            messages = previewMessages(),
            listState = LazyListState(),
            selectedMessageIds = persistentSetOf("outgoing-delivered"),
            showIncomingParticipantIdentity = true,
            subscriptions = previewSubscriptions(),
            currentSendSimDisplayName = "Personal",
            onAttachmentClick = { _, _ -> },
            onExternalUriClick = {},
            onMessageClick = {},
            onMessageAvatarClick = {},
            onMessageDownloadClick = {},
            onMessageLongClick = {},
            onMessageResendClick = {},
            onSimSelectorClick = {},
        )
    }
}
