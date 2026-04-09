package com.android.messaging.ui.conversation.v2.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.v2.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.v2.conversationMessageItemTestTag
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.ui.message.ConversationMessage
import com.android.messaging.ui.conversation.v2.messages.ui.message.conversationMessageDisplayEpochDay
import com.android.messaging.ui.conversation.v2.messages.ui.message.formatDateSeparatorText
import java.util.TimeZone
import kotlinx.collections.immutable.ImmutableList

private val CONVERSATION_MESSAGES_CONTENT_PADDING = PaddingValues(
    start = 16.dp,
    top = 24.dp,
    end = 16.dp,
    bottom = 24.dp,
)

private val CONVERSATION_MESSAGES_CLUSTER_TOP_PADDING = 2.dp
private val CONVERSATION_MESSAGES_GROUP_TOP_PADDING = 12.dp
private val CONVERSATION_MESSAGES_SEPARATOR_SPACING = 12.dp
private val CONVERSATION_MESSAGES_SEPARATOR_PADDING = PaddingValues(
    horizontal = 14.dp,
    vertical = 6.dp,
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
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val displayMessages = remember(messages) {
        messages.asReversed()
    }
    val timeZone = remember(configuration) {
        TimeZone.getDefault()
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = modifier
            .fillMaxSize()
            .testTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .background(color = MaterialTheme.colorScheme.background),
        contentPadding = CONVERSATION_MESSAGES_CONTENT_PADDING,
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
                messageAbove = messageAboveCurrent(
                    messages = displayMessages,
                    index = index,
                ),
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
            )
        }
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

@Composable
private fun ConversationMessagesItem(
    message: ConversationMessageUiModel,
    messageAbove: ConversationMessageUiModel?,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
) {
    val presentation = rememberConversationMessagesItemPresentation(
        message = message,
        messageAbove = messageAbove,
    )

    ColumnWithSeparator(
        showDateSeparator = presentation.showDateSeparator,
        dateSeparatorText = presentation.dateSeparatorText,
    ) {
        ConversationMessage(
            modifier = Modifier
                .testTag(conversationMessageItemTestTag(messageId = message.messageId))
                .padding(top = presentation.topPadding),
            message = message,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
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
        message.canClusterWithPrevious -> CONVERSATION_MESSAGES_CLUSTER_TOP_PADDING
        else -> CONVERSATION_MESSAGES_GROUP_TOP_PADDING
    }
}

@Composable
private fun ColumnWithSeparator(
    showDateSeparator: Boolean,
    dateSeparatorText: String?,
    content: @Composable () -> Unit,
) {
    val verticalSpace = when {
        showDateSeparator -> CONVERSATION_MESSAGES_SEPARATOR_SPACING
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
            modifier = Modifier.padding(CONVERSATION_MESSAGES_SEPARATOR_PADDING),
        )
    }
}

private fun shouldShowDateSeparator(
    currentMessage: ConversationMessageUiModel,
    messageAbove: ConversationMessageUiModel?,
    timeZone: TimeZone,
): Boolean {
    if (messageAbove == null) {
        return true
    }

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
