package com.android.messaging.ui.conversationlist.redesign.mapper

import androidx.core.net.toUri
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAvatarUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListPreviewUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListSelectionUiState
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListSnippetUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListUiState
import com.android.messaging.ui.conversationlist.redesign.model.SelectedConversationUiModel
import com.android.messaging.ui.conversationlist.redesign.model.SelectionActionsUiState
import com.android.messaging.util.AvatarUriUtil
import com.android.messaging.util.ContentType
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationListUiStateMapper {
    fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableSet<String>,
        isScrollUpVisible: Boolean,
    ): ConversationListUiState
}

internal class ConversationListUiStateMapperImpl @Inject constructor() :
    ConversationListUiStateMapper {

    override fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableSet<String>,
        isScrollUpVisible: Boolean,
    ): ConversationListUiState {
        val items = snapshot.items
            .map { item ->
                val isSelected = item.conversationId in selectedConversationIds

                mapItem(
                    item = item,
                    isSelected = isSelected,
                )
            }
            .toImmutableList()

        val content = when {
            items.isNotEmpty() -> {
                ConversationListContentUiState.Items(items)
            }

            !snapshot.hasFirstSyncCompleted -> {
                ConversationListContentUiState.WaitingForSync
            }

            else -> {
                ConversationListContentUiState.Empty
            }
        }

        val selection = mapSelectionState(
            items = snapshot.items,
            selectedConversationIds = selectedConversationIds,
            blockedDestinations = snapshot.blockedDestinations,
        )

        return ConversationListUiState(
            content = content,
            selection = selection,
            isScrollUpVisible = isScrollUpVisible,
            hasBlockedParticipants = snapshot.blockedDestinations.isNotEmpty(),
        )
    }

    private fun mapItem(
        item: ConversationListItem,
        isSelected: Boolean,
    ): ConversationListItemUiModel {
        val preview = item.activePreview()
        val isDraft = item.draft.isVisible
        val isOutgoing = isDraft || !item.latestMessage.isIncoming

        return ConversationListItemUiModel(
            conversationId = item.conversationId,
            title = item.title,
            avatar = ConversationListAvatarUiModel(
                uri = resolveAvatarUri(item.icon),
                contactId = item.participant.contactId,
                lookupKey = item.participant.lookupKey,
                normalizedDestination = item.participant.otherNormalizedDestination,
                isGroup = item.participant.isGroup,
            ),
            snippet = ConversationListSnippetUiModel(
                text = item.activeSnippetText(),
                senderName = item.latestMessage.senderName,
                preview = preview,
                isDraft = isDraft,
            ),
            subject = item.activeSubject(),
            timestampMillis = item.latestMessage.timestamp,
            status = mapStatus(item),
            isOutgoing = isOutgoing,
            isUnread = !item.latestMessage.isRead && !isDraft,
            isGroup = item.participant.isGroup,
            isEnterprise = item.participant.isEnterprise,
            isMuted = !item.notification.isEnabled,
            isArchived = item.isArchived,
            isSelected = isSelected,
        )
    }

    private fun mapSelectionState(
        items: List<ConversationListItem>,
        selectedConversationIds: ImmutableSet<String>,
        blockedDestinations: ImmutableSet<String>,
    ): ConversationListSelectionUiState {
        val selectedConversations = items
            .asSequence()
            .filter { item ->
                item.conversationId in selectedConversationIds
            }
            .map(::toSelectedConversation)
            .toList()
            .toImmutableList()

        return ConversationListSelectionUiState(
            selectedConversations = selectedConversations,
            actions = mapSelectionActions(
                selectedConversations = selectedConversations,
                blockedDestinations = blockedDestinations,
            ),
            isActive = selectedConversations.isNotEmpty(),
        )
    }

    private fun toSelectedConversation(
        item: ConversationListItem,
    ): SelectedConversationUiModel {
        return SelectedConversationUiModel(
            conversationId = item.conversationId,
            normalizedDestination = item.participant.otherNormalizedDestination,
            participantLookupKey = item.participant.lookupKey,
            isGroup = item.participant.isGroup,
            isArchived = item.isArchived,
        )
    }

    private fun mapSelectionActions(
        selectedConversations: List<SelectedConversationUiModel>,
        blockedDestinations: ImmutableSet<String>,
    ): SelectionActionsUiState {
        val singleSelection = selectedConversations.singleOrNull()

        return SelectionActionsUiState(
            canArchive = selectedConversations.any { !it.isArchived },
            canUnarchive = selectedConversations.any { it.isArchived },
            canDelete = selectedConversations.isNotEmpty(),
            canAddContact = singleSelection?.canAddContact() == true,
            canBlock = singleSelection?.canBlock(blockedDestinations) == true,
        )
    }

    private fun SelectedConversationUiModel.canAddContact(): Boolean {
        return !isGroup && participantLookupKey.isNullOrBlank()
    }

    private fun SelectedConversationUiModel.canBlock(
        blockedDestinations: ImmutableSet<String>,
    ): Boolean {
        val destination = normalizedDestination?.takeIf(String::isNotBlank) ?: return false

        return destination !in blockedDestinations
    }

    private fun ConversationListItem.activeSnippetText(): String? {
        return when {
            draft.isVisible -> draft.snippetText
            else -> latestMessage.snippetText
        }
    }

    private fun ConversationListItem.activeSubject(): String? {
        return when {
            draft.isVisible -> draft.subject
            else -> subject
        }?.takeIf(String::isNotBlank)
    }

    private fun ConversationListItem.activePreview(): ConversationListPreviewUiModel? {
        val previewUri = when {
            draft.isVisible -> draft.previewUri
            else -> latestMessage.previewUri
        }?.takeIf(String::isNotBlank)

        val previewContentType = when {
            draft.isVisible -> draft.previewContentType
            else -> latestMessage.previewContentType
        }?.takeIf(String::isNotBlank)

        return when {
            previewUri != null && previewContentType != null -> {
                mapPreview(
                    contentUri = previewUri,
                    contentType = previewContentType,
                )
            }

            else -> null
        }
    }

    private fun mapPreview(
        contentUri: String,
        contentType: String,
    ): ConversationListPreviewUiModel {
        return when {
            ContentType.isAudioType(contentType) -> {
                ConversationListPreviewUiModel.Audio(
                    contentUri = contentUri,
                    contentType = contentType,
                )
            }

            ContentType.isImageType(contentType) -> {
                ConversationListPreviewUiModel.Image(
                    contentUri = contentUri,
                    contentType = contentType,
                )
            }

            ContentType.isVideoType(contentType) -> {
                ConversationListPreviewUiModel.Video(
                    contentUri = contentUri,
                    contentType = contentType,
                )
            }

            ContentType.isVCardType(contentType) -> {
                ConversationListPreviewUiModel.VCard(
                    contentUri = contentUri,
                    contentType = contentType,
                )
            }

            else -> {
                ConversationListPreviewUiModel.File(
                    contentUri = contentUri,
                    contentType = contentType,
                )
            }
        }
    }

    private fun mapStatus(
        item: ConversationListItem,
    ): ConversationListMessageStatus {
        return when {
            item.draft.isVisible -> ConversationListMessageStatus.Draft
            else -> item.latestMessage.status
        }
    }

    private fun resolveAvatarUri(
        icon: String?,
    ): String? {
        val iconUriString = icon?.takeIf(String::isNotBlank) ?: return null
        val iconUri = iconUriString.toUri()

        return when {
            AvatarUriUtil.isAvatarUri(iconUri) -> AvatarUriUtil.getPrimaryUri(iconUri)?.toString()
            else -> iconUriString
        }
    }
}
