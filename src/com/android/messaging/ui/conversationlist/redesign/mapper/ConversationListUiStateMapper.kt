package com.android.messaging.ui.conversationlist.redesign.mapper

import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanAddContact
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAvatarUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListPreviewUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListSelectionUiState
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListSnippetUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListUiState
import com.android.messaging.ui.conversationlist.redesign.model.SelectedConversationUiModel
import com.android.messaging.ui.conversationlist.redesign.model.SelectionActionsUiState
import com.android.messaging.util.ContentType
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationListUiStateMapper {
    fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableSet<String>,
        isScrollUpVisible: Boolean,
        isDebugEnabled: Boolean,
    ): ConversationListUiState
}

internal class ConversationListUiStateMapperImpl @Inject constructor(
    private val canAddContact: CanAddContact,
    private val canPlacePhoneCall: CanPlacePhoneCall,
    private val canShowOrAddContact: CanShowOrAddContact,
    private val isContactSavedUseCase: IsContactSaved,
    private val resolveAvatarUri: ResolveAvatarUri,
) : ConversationListUiStateMapper {

    override fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableSet<String>,
        isScrollUpVisible: Boolean,
        isDebugEnabled: Boolean,
    ): ConversationListUiState {
        val items = snapshot.items
            .map { item ->
                val isSelected = item.conversationId in selectedConversationIds
                item.toConversationListUiState(isSelected)
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
            isDebugEnabled = isDebugEnabled,
        )
    }

    private fun ConversationListItem.toConversationListUiState(
        isSelected: Boolean,
    ): ConversationListItemUiModel {
        val isDraft = draft.isVisible
        val isOutgoing = isDraft || !latestMessage.isIncoming

        return ConversationListItemUiModel(
            conversationId = conversationId,
            title = title,
            avatar = toAvatar(),
            snippet = ConversationListSnippetUiModel(
                text = activeSnippetText(),
                senderName = latestMessage.senderName,
                preview = activePreview(),
                isDraft = isDraft,
            ),
            subject = activeSubject(),
            timestampMillis = latestMessage.timestamp,
            status = toStatus(),
            isOutgoing = isOutgoing,
            isUnread = !latestMessage.isRead && !isDraft,
            isGroup = participant.isGroup,
            isEnterprise = participant.isEnterprise,
            isMuted = !notification.isEnabled,
            isArchived = isArchived,
            isSelected = isSelected,
        )
    }

    private fun ConversationListItem.toAvatar(): ConversationListAvatarUiModel {
        val isOneOnOne = !participant.isGroup
        val destination = participant.otherNormalizedDestination?.takeIf(String::isNotBlank)
        val canShowContact = canShowOrAddContact(
            isGroup = participant.isGroup,
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
            destination = destination,
        )
        val isContactSaved = isContactSavedUseCase(
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
        )

        return ConversationListAvatarUiModel(
            uri = resolveAvatarUri(icon),
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
            normalizedDestination = destination,
            isGroup = participant.isGroup,
            details = destination.takeIf { isOneOnOne },
            canCall = isOneOnOne && canPlacePhoneCall(destination),
            canShowContact = canShowContact,
            isContactSaved = isContactSaved,
        )
    }

    private fun ConversationListItem.toStatus(): ConversationListMessageStatus {
        return when {
            draft.isVisible -> ConversationListMessageStatus.Draft
            else -> latestMessage.status
        }
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
        val canAddContact = singleSelection?.let { conversation ->
            canAddContact(
                isGroup = conversation.isGroup,
                lookupKey = conversation.participantLookupKey,
                destination = conversation.normalizedDestination,
            )
        }
        val canBlock = singleSelection?.let { conversation ->
            canBlock(
                destination = conversation.normalizedDestination,
                blockedDestinations = blockedDestinations,
            )
        }

        return SelectionActionsUiState(
            canArchive = selectedConversations.any { !it.isArchived },
            canUnarchive = selectedConversations.any { it.isArchived },
            canDelete = selectedConversations.isNotEmpty(),
            canAddContact = canAddContact == true,
            canBlock = canBlock == true,
        )
    }

    private fun canBlock(
        destination: String?,
        blockedDestinations: ImmutableSet<String>,
    ): Boolean {
        val resolvedDestination = destination?.takeIf(String::isNotBlank) ?: return false

        return resolvedDestination !in blockedDestinations
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
}
