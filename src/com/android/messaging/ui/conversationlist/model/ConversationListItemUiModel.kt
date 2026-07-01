package com.android.messaging.ui.conversationlist.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus

@Immutable
internal data class ConversationListItemUiModel(
    val conversationId: String,
    val title: String?,
    val avatar: ConversationListAvatarUiModel,
    val snippet: ConversationListSnippetUiModel,
    val subject: String?,
    val timestampMillis: Long,
    val status: ConversationListMessageStatus,
    @param:StringRes
    val mmsDownloadTitleResId: Int?,
    val isOutgoing: Boolean,
    val isUnread: Boolean,
    val isEnterprise: Boolean,
    val isMuted: Boolean,
    val isSnoozed: Boolean,
    val isPinned: Boolean,
    val isSelected: Boolean,
)

@Immutable
internal data class ConversationListAvatarUiModel(
    val uri: String?,
    val contactId: Long,
    val lookupKey: String?,
    val normalizedDestination: String?,
    val isGroup: Boolean,
    val subtitle: String?,
    val canCall: Boolean,
    val canShowContact: Boolean,
    val isContactSaved: Boolean,
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

    @Immutable
    data object Audio : ConversationListPreviewUiModel

    @Immutable
    data object File : ConversationListPreviewUiModel

    @Immutable
    data object VCard : ConversationListPreviewUiModel

    @Immutable
    data class Image(
        val contentUri: String,
        val contentType: String,
    ) : ConversationListPreviewUiModel

    @Immutable
    data class Video(
        val contentUri: String,
        val contentType: String,
    ) : ConversationListPreviewUiModel
}
