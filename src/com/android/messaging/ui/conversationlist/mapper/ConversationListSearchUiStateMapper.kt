package com.android.messaging.ui.conversationlist.mapper

import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationSearchMatch
import com.android.messaging.data.conversationlist.model.MessageSearchMatch
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListSearchContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListSearchMessageUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListSnippetUiModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationListSearchUiStateMapper {
    fun map(
        query: String,
        conversationMatches: ImmutableList<ConversationSearchMatch>,
        messageMatches: ImmutableList<MessageSearchMatch>,
    ): ConversationListSearchContentUiState
}

internal class ConversationListSearchUiStateMapperImpl @Inject constructor(
    private val itemUiMapper: ConversationListItemUiMapper,
) : ConversationListSearchUiStateMapper {

    override fun map(
        query: String,
        conversationMatches: ImmutableList<ConversationSearchMatch>,
        messageMatches: ImmutableList<MessageSearchMatch>,
    ): ConversationListSearchContentUiState {
        return when {
            query.isBlank() -> ConversationListSearchContentUiState.Idle

            conversationMatches.isEmpty() && messageMatches.isEmpty() -> {
                ConversationListSearchContentUiState.NoResults
            }

            else -> {
                ConversationListSearchContentUiState.Results(
                    conversations = conversationMatches
                        .map(::mapConversation)
                        .toImmutableList(),
                    messages = messageMatches
                        .map(::mapMessage)
                        .toImmutableList(),
                )
            }
        }
    }

    private fun mapConversation(match: ConversationSearchMatch): ConversationListItemUiModel {
        return itemUiMapper
            .map(
                item = match.item,
                isSelected = false,
                isOpened = false,
            )
            .copy(
                titleMatchRanges = match.titleMatchRanges,
                isArchivedBadgeVisible = match.item.isArchived,
            )
    }

    private fun mapMessage(match: MessageSearchMatch): ConversationListSearchMessageUiModel {
        val message = match.message
        val snippet = snippetAroundFirstMatch(
            text = message.text,
            matchRanges = match.textMatchRanges,
        )
        val item = itemUiMapper
            .map(
                item = match.item,
                isSelected = false,
                isOpened = false,
            )
            .copy(
                snippet = ConversationListSnippetUiModel(
                    text = snippet.text,
                    senderName = message.senderName,
                    preview = null,
                    isDraft = false,
                ),
                subject = null,
                timestampMillis = message.receivedTimestamp,
                status = ConversationListMessageStatus.Normal,
                mmsDownloadTitleResId = null,
                isOutgoing = !message.isIncoming,
                isUnread = false,
                snippetMatchRanges = snippet.matchRanges,
                isArchivedBadgeVisible = match.item.isArchived,
            )

        return ConversationListSearchMessageUiModel(
            messageId = message.messageId,
            item = item,
        )
    }

    /**
     * The row shows a single line of the message, so a match further in starts the line a few
     * words before it, and line breaks become spaces.
     */
    private fun snippetAroundFirstMatch(
        text: String,
        matchRanges: ImmutableList<IntRange>,
    ): MessageSnippet {
        val singleLineText = text
            .replace('\n', ' ')
            .replace('\r', ' ')
        val firstMatchStart = matchRanges.firstOrNull()?.first ?: 0

        if (firstMatchStart <= SNIPPET_LEADING_CHARS) {
            return MessageSnippet(
                text = singleLineText,
                matchRanges = matchRanges,
            )
        }

        val windowStart = firstMatchStart - SNIPPET_LEADING_CHARS
        val wordStart = singleLineText.indexOf(' ', startIndex = windowStart) + 1
        val start = when {
            wordStart in (windowStart + 1)..firstMatchStart -> wordStart
            Character.isLowSurrogate(singleLineText[windowStart]) -> windowStart + 1
            else -> windowStart
        }
        val offset = SNIPPET_ELLIPSIS.length - start

        return MessageSnippet(
            text = SNIPPET_ELLIPSIS + singleLineText.substring(start),
            matchRanges = matchRanges
                .map { range -> (range.first + offset)..(range.last + offset) }
                .toImmutableList(),
        )
    }

    private class MessageSnippet(
        val text: String,
        val matchRanges: ImmutableList<IntRange>,
    )

    private companion object {
        private const val SNIPPET_LEADING_CHARS = 16
        private const val SNIPPET_ELLIPSIS = "…"
    }
}
