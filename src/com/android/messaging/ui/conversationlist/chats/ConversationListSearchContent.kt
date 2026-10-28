package com.android.messaging.ui.conversationlist.chats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.common.components.horizontalSafeDrawingInsets
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.common.item.ConversationListItemRow
import com.android.messaging.ui.conversationlist.common.item.SearchResultsContentPadding
import com.android.messaging.ui.conversationlist.common.item.SearchResultsItemSpacing
import com.android.messaging.ui.conversationlist.common.item.SearchSectionHeaderHorizontalPadding
import com.android.messaging.ui.conversationlist.common.item.SearchSectionHeaderVerticalPadding
import com.android.messaging.ui.conversationlist.common.list.conversationRowHorizontalPadding
import com.android.messaging.ui.conversationlist.common.status.ConversationListStatusMessage
import com.android.messaging.ui.conversationlist.common.support.CONVERSATION_LIST_SEARCH_RESULTS_TEST_TAG
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItems
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel as Model
import com.android.messaging.ui.conversationlist.model.ConversationListSearchContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListSearchMessageUiModel
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val CONVERSATIONS_HEADER_KEY = "conversations_header"
private const val MESSAGES_HEADER_KEY = "messages_header"
private const val MESSAGE_KEY_PREFIX = "message:"
private const val SECTION_HEADER_CONTENT_TYPE = "section_header"
private const val CONVERSATION_ROW_CONTENT_TYPE = "conversation_row"

@Composable
internal fun ConversationListSearchContent(
    content: ConversationListSearchContentUiState,
    scaffoldContentPadding: PaddingValues,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (content) {
            ConversationListSearchContentUiState.Idle -> Unit

            ConversationListSearchContentUiState.NoResults -> {
                ConversationListStatusMessage(
                    text = stringResource(R.string.conversation_list_search_no_results),
                )
            }

            is ConversationListSearchContentUiState.Results -> {
                ConversationListSearchResults(
                    results = content,
                    scaffoldContentPadding = scaffoldContentPadding,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun ConversationListSearchResults(
    results: ConversationListSearchContentUiState.Results,
    scaffoldContentPadding: PaddingValues,
    onAction: (Action) -> Unit,
) {
    val horizontalInsets = horizontalSafeDrawingInsets()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(CONVERSATION_LIST_SEARCH_RESULTS_TEST_TAG),
        contentPadding = PaddingValues(
            top = SearchResultsContentPadding,
            bottom = scaffoldContentPadding.calculateBottomPadding() +
                SearchResultsContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(SearchResultsItemSpacing),
    ) {
        if (results.conversations.isNotEmpty()) {
            conversationsSection(
                conversations = results.conversations,
                horizontalInsets = horizontalInsets,
                onAction = onAction,
            )
        }

        if (results.messages.isNotEmpty()) {
            messagesSection(
                messages = results.messages,
                horizontalInsets = horizontalInsets,
                onAction = onAction,
            )
        }
    }
}

private fun LazyListScope.conversationsSection(
    conversations: ImmutableList<Model>,
    horizontalInsets: PaddingValues,
    onAction: (Action) -> Unit,
) {
    item(
        key = CONVERSATIONS_HEADER_KEY,
        contentType = SECTION_HEADER_CONTENT_TYPE,
    ) {
        ConversationListSearchSectionHeader(
            text = stringResource(R.string.conversation_list_search_conversations_header),
            horizontalInsets = horizontalInsets,
        )
    }

    items(
        items = conversations,
        key = { item -> item.conversationId.value },
        contentType = { CONVERSATION_ROW_CONTENT_TYPE },
    ) { item ->
        ConversationListSearchResultRow(
            item = item,
            horizontalInsets = horizontalInsets,
            onOpen = {
                onAction(Action.SearchConversationClicked(item.conversationId))
            },
            onAction = onAction,
        )
    }
}

private fun LazyListScope.messagesSection(
    messages: ImmutableList<ConversationListSearchMessageUiModel>,
    horizontalInsets: PaddingValues,
    onAction: (Action) -> Unit,
) {
    item(
        key = MESSAGES_HEADER_KEY,
        contentType = SECTION_HEADER_CONTENT_TYPE,
    ) {
        ConversationListSearchSectionHeader(
            text = stringResource(R.string.conversation_list_search_messages_header),
            horizontalInsets = horizontalInsets,
        )
    }

    items(
        items = messages,
        key = { message -> MESSAGE_KEY_PREFIX + message.messageId.value },
        contentType = { CONVERSATION_ROW_CONTENT_TYPE },
    ) { message ->
        ConversationListSearchResultRow(
            item = message.item,
            horizontalInsets = horizontalInsets,
            onOpen = {
                onAction(
                    Action.SearchMessageClicked(
                        conversationId = message.item.conversationId,
                        messageId = message.messageId,
                    ),
                )
            },
            onAction = onAction,
        )
    }
}

@Composable
private fun ConversationListSearchSectionHeader(
    text: String,
    horizontalInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .padding(horizontalInsets)
            .padding(
                horizontal = SearchSectionHeaderHorizontalPadding,
                vertical = SearchSectionHeaderVerticalPadding,
            )
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ConversationListSearchResultRow(
    item: Model,
    horizontalInsets: PaddingValues,
    onOpen: () -> Unit,
    onAction: (Action) -> Unit,
) {
    val destination = item.avatar.normalizedDestination

    ConversationListItemRow(
        item = item,
        modifier = Modifier.conversationRowHorizontalPadding(horizontalInsets),
        onClick = onOpen,
        onLongClick = {},
        onAvatarMessageClick = onOpen,
        onAvatarCallClick = {
            if (destination != null) {
                onAction(Action.AvatarCallClicked(destination))
            }
        }.takeIf { item.avatar.canCall },
        onAvatarContactClick = {
            onAction(Action.AvatarContactClicked(item.avatar))
        }.takeIf { item.avatar.canShowContact },
        onAvatarInfoClick = {
            onAction(Action.AvatarInfoClicked(item.conversationId))
        },
    )
}

@PreviewLightDark
@Composable
private fun ConversationListSearchContentResultsPreview() {
    MessagingPreviewTheme {
        ConversationListSearchContent(
            content = ConversationListSearchContentUiState.Results(
                conversations = previewConversationListItems()
                    .map { item ->
                        item.copy(
                            titleMatchRanges = persistentListOf(0..1),
                            isArchivedBadgeVisible = item.isPinned,
                        )
                    }
                    .toImmutableList(),
                messages = previewConversationListItems()
                    .take(2)
                    .mapIndexed { index, item ->
                        ConversationListSearchMessageUiModel(
                            messageId = MessageId(index.toString()),
                            item = item.copy(
                                snippet = item.snippet.copy(text = "Are we still on for lunch?"),
                                snippetMatchRanges = persistentListOf(20..24),
                            ),
                        )
                    }
                    .toImmutableList(),
            ),
            scaffoldContentPadding = PaddingValues(),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationListSearchContentNoResultsPreview() {
    MessagingPreviewTheme {
        ConversationListSearchContent(
            content = ConversationListSearchContentUiState.NoResults,
            scaffoldContentPadding = PaddingValues(),
            onAction = {},
        )
    }
}
