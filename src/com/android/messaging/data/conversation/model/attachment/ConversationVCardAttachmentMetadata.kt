package com.android.messaging.data.conversation.model.attachment

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ConversationVCardAttachmentMetadata {

    @Immutable
    data object Missing : ConversationVCardAttachmentMetadata

    @Immutable
    data object Loading : ConversationVCardAttachmentMetadata

    @Immutable
    data object Failed : ConversationVCardAttachmentMetadata

    @Immutable
    data class Loaded(
        val type: ConversationVCardAttachmentType,
        val avatarUri: String?,
        val displayName: String?,
        val details: String?,
        val locationAddress: String?,
    ) : ConversationVCardAttachmentMetadata
}
