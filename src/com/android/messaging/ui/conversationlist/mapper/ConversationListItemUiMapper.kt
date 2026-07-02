package com.android.messaging.ui.conversationlist.mapper

import android.content.Context
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.sms.cleanseMmsSubject
import com.android.messaging.ui.conversationlist.model.ConversationListAvatarUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListPreviewUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListSnippetUiModel
import com.android.messaging.util.ContentType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface ConversationListItemUiMapper {
    fun map(item: ConversationListItem, isSelected: Boolean): ConversationListItemUiModel
}

internal class ConversationListItemUiMapperImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val canPlacePhoneCall: CanPlacePhoneCall,
    private val canShowOrAddContact: CanShowOrAddContact,
    private val isContactSaved: IsContactSaved,
    private val resolveAvatarUri: ResolveAvatarUri,
) : ConversationListItemUiMapper {

    override fun map(
        item: ConversationListItem,
        isSelected: Boolean,
    ): ConversationListItemUiModel {
        return item.toItemUiModel(isSelected)
    }

    private fun ConversationListItem.toItemUiModel(
        isSelected: Boolean,
    ): ConversationListItemUiModel {
        val isDraft = draft.isVisible
        val isOutgoing = isDraft || !latestMessage.isIncoming
        val status = toStatus()

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
            status = status,
            mmsDownloadTitleResId = conversationListMmsDownloadTitleResId(status),
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
