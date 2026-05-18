package com.android.messaging.ui.conversation.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.android.messaging.ui.conversation.resolveDisplayName
import java.util.TimeZone
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableMap

private val messagesContentPadding = PaddingValues(
    start = 16.dp,
    top = 24.dp,
    end = 16.dp,
    bottom = 24.dp,
)
private val sendSimContentPadding = PaddingValues(
    start = 16.dp,
    top = 24.dp,
    end = 16.dp,
    bottom = 6.dp,
)

private val MESSAGES_CLUSTER_TOP_PADDING = 2.dp
private val MESSAGES_GROUP_TOP_PADDING = 12.dp
private val MESSAGES_SEPARATOR_SPACING = 12.dp
private val messagesSeparatorPadding = PaddingValues(
    horizontal = 14.dp,
    vertical = 6.dp,
)
private val sendSimPadding = PaddingValues(
    horizontal = 16.dp,
    vertical = 4.dp,
)

private enum class ConversationMessagesItemContentType {
    Message,
    MessageWithDateSeparator,
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
        ),
    ) {
        conversationMessageItems(
            displayMessages = displayMessages,
            timeZone = timeZone,
            selectedMessageIds = selectedMessageIds,
            isSelectionMode = isSelectionMode,
            showIncomingParticipantIdentity = showIncomingParticipantIdentity,
            simDisplayNameByParticipantId = simDisplayNameByParticipantId,
            currentSendSimDisplayName = currentSendSimDisplayName,
            shouldShowSendSimIndicator = shouldShowSendSimIndicator,
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
    currentSendSimDisplayName: String?,
    shouldShowSendSimIndicator: Boolean,
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
            message = message,
            messageAbove = messageAboveCurrent(messages = displayMessages, index = index),
            messageBelow = messageBelowCurrent(messages = displayMessages, index = index),
            isSelectionMode = isSelectionMode,
            isSelected = selectedMessageIds.contains(message.messageId),
            showIncomingParticipantIdentity = showIncomingParticipantIdentity,
            simDisplayNameByParticipantId = simDisplayNameByParticipantId,
            currentSendSimDisplayName = when {
                index == 0 && shouldShowSendSimIndicator -> currentSendSimDisplayName
                else -> null
            },
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

private fun conversationMessagesContentPadding(
    shouldShowSendSimIndicator: Boolean,
): PaddingValues {
    return when {
        shouldShowSendSimIndicator -> sendSimContentPadding
        else -> messagesContentPadding
    }
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
    message: ConversationMessageUiModel,
    messageAbove: ConversationMessageUiModel?,
    messageBelow: ConversationMessageUiModel?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    showIncomingParticipantIdentity: Boolean,
    simDisplayNameByParticipantId: ImmutableMap<String, String>,
    currentSendSimDisplayName: String?,
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

        if (!currentSendSimDisplayName.isNullOrBlank()) {
            ConversationSendSimIndicator(
                simDisplayName = currentSendSimDisplayName,
                isSimSelectorEnabled = !isSelectionMode,
                onSimSelectorClick = onSimSelectorClick,
            )
        }
    }
}

@Composable
private fun ConversationSendSimIndicator(
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
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun ColumnWithSeparator(
    showDateSeparator: Boolean,
    dateSeparatorText: String?,
    content: @Composable () -> Unit,
) {
    val verticalSpace = when {
        showDateSeparator -> MESSAGES_SEPARATOR_SPACING
        else -> 0.dp
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(space = verticalSpace),
    ) {
        if (showDateSeparator && dateSeparatorText != null) {
            ConversationDateSeparator(
                text = dateSeparatorText,
            )
        }

        content()
    }
}

@Composable
private fun ConversationDateSeparator(
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(messagesSeparatorPadding),
        )
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
