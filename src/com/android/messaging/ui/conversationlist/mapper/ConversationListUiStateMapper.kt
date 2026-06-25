package com.android.messaging.ui.conversationlist.mapper

import android.content.Context
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanAddContact
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.sms.cleanseMmsSubject
import com.android.messaging.ui.conversationlist.model.ConversationListAvatarUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListPreviewUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListSelectionUiState
import com.android.messaging.ui.conversationlist.model.ConversationListSnippetUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListUiState
import com.android.messaging.ui.conversationlist.model.SelectionActionsUiState
import com.android.messaging.util.ContentType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationListUiStateMapper {
    fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
        isScrollToTopVisible: Boolean,
        isDebugEnabled: Boolean,
    ): ConversationListUiState
}

internal class ConversationListUiStateMapperImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val canAddContact: CanAddContact,
    private val canPlacePhoneCall: CanPlacePhoneCall,
    private val canShowOrAddContact: CanShowOrAddContact,
    private val isContactSaved: IsContactSaved,
    private val resolveAvatarUri: ResolveAvatarUri,
) : ConversationListUiStateMapper {

    override fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
        isScrollToTopVisible: Boolean,
        isDebugEnabled: Boolean,
    ): ConversationListUiState {
        val items = snapshot.items
            .map { item ->
                val isSelected = item.conversationId in selectedConversationIds
                item.toItemUiModel(isSelected)
            }
            .toImmutableList()

        val content = when {
            items.isNotEmpty() -> {
                ConversationListContentUiState.Items(
                    items = items,
                    restoredConversationIds = snapshot.restoredConversationIds,
                )
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
            isScrollToTopVisible = isScrollToTopVisible,
            hasBlockedParticipants = snapshot.blockedDestinations.isNotEmpty(),
            isDebugEnabled = isDebugEnabled,
        )
    }

    private fun ConversationListItem.toItemUiModel(
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
            isUnread = !latestMessage.isRead,
            isEnterprise = participant.isEnterprise,
            isMuted = !notification.isEnabled,
            isSnoozed = notification.isSnoozed,
            isPinned = isPinned,
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

        return ConversationListAvatarUiModel(
            uri = resolveAvatarUri(icon),
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
            normalizedDestination = destination,
            isGroup = participant.isGroup,
            subtitle = destination.takeIf { isOneOnOne },
            canCall = isOneOnOne && canPlacePhoneCall(destination),
            canShowContact = canShowContact,
            isContactSaved = isContactSaved(
                contactId = participant.contactId,
                lookupKey = participant.lookupKey,
            ),
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
        selectedConversationIds: ImmutableList<String>,
        blockedDestinations: ImmutableSet<String>,
    ): ConversationListSelectionUiState {
        val itemsById = items.associateBy(ConversationListItem::conversationId)
        val selectedItems = selectedConversationIds
            .mapNotNull { conversationId ->
                itemsById[conversationId]
            }

        return ConversationListSelectionUiState(
            selectedCount = selectedItems.size,
            actions = mapSelectionActions(
                selectedItems = selectedItems,
                blockedDestinations = blockedDestinations,
            ),
        )
    }

    private fun mapSelectionActions(
        selectedItems: List<ConversationListItem>,
        blockedDestinations: ImmutableSet<String>,
    ): SelectionActionsUiState {
        val singleSelection = selectedItems.singleOrNull()
        val firstSelected = selectedItems.firstOrNull()
        val canAddSelectedContact = singleSelection?.participant?.let { participant ->
            canAddContact(
                isGroup = participant.isGroup,
                lookupKey = participant.lookupKey,
                destination = participant.otherNormalizedDestination,
            )
        }
        val canBlockSelected = singleSelection?.let { item ->
            canBlock(
                destination = item.participant.otherNormalizedDestination,
                blockedDestinations = blockedDestinations,
            )
        }

        return SelectionActionsUiState(
            canAddContact = canAddSelectedContact == true,
            canBlock = canBlockSelected == true,
            firstSelectedIsPinned = firstSelected?.isPinned,
            firstSelectedIsSnoozed = firstSelected?.notification?.isSnoozed,
            firstSelectedIsUnread = firstSelected?.latestMessage?.isRead?.not(),
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
            else -> cleanseMmsSubject(
                resources = context.resources,
                subject = subject,
            )
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
                ConversationListPreviewUiModel.Audio
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
                ConversationListPreviewUiModel.VCard
            }

            else -> {
                ConversationListPreviewUiModel.File
            }
        }
    }
}
