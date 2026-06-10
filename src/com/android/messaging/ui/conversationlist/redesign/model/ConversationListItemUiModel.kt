package com.android.messaging.ui.conversationlist.redesign.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
internal data class ConversationListItemUiModel(
    val conversationId: String,
    val title: String?,
    val avatar: ConversationListAvatarUiModel,
    val snippet: ConversationListSnippetUiModel,
    val subject: String?,
    val timestampMillis: Long,
    val status: ConversationListMessageStatus,
    val isOutgoing: Boolean,
    val isUnread: Boolean,
    val isGroup: Boolean,
    val isEnterprise: Boolean,
    val isMuted: Boolean,
    val isArchived: Boolean,
    val isSelected: Boolean,
)

@Immutable
internal data class ConversationListAvatarUiModel(
    val uri: String?,
    val contactId: Long,
    val lookupKey: String?,
    val normalizedDestination: String?,
    val isGroup: Boolean,
)

@Immutable
internal data class ConversationListSnippetUiModel(
    val text: String?,
    val senderName: String?,
    val preview: ConversationListPreviewUiModel?,
    val isDraft: Boolean,
)

@Immutable
internal sealed interface ConversationListPreviewUiModel {
    val contentUri: String
    val contentType: String

    @Immutable
    data class Audio(
        override val contentUri: String,
        override val contentType: String,
    ) : ConversationListPreviewUiModel

    @Immutable
    data class File(
        override val contentUri: String,
        override val contentType: String,
    ) : ConversationListPreviewUiModel

    @Immutable
    data class Image(
        override val contentUri: String,
        override val contentType: String,
    ) : ConversationListPreviewUiModel

    @Immutable
    data class VCard(
        override val contentUri: String,
        override val contentType: String,
    ) : ConversationListPreviewUiModel

    @Immutable
    data class Video(
        override val contentUri: String,
        override val contentType: String,
    ) : ConversationListPreviewUiModel
}

@Stable
internal sealed interface ConversationListMessageStatus {
    data object Unknown : ConversationListMessageStatus
    data object Normal : ConversationListMessageStatus
    data object Sending : ConversationListMessageStatus
    data object Draft : ConversationListMessageStatus

    data class Failed(
        val rawTelephonyStatus: Int,
    ) : ConversationListMessageStatus
}
