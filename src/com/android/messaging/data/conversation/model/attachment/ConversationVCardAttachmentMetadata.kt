package com.android.messaging.data.conversation.model.attachment

import androidx.compose.runtime.Immutable
import com.android.messaging.data.vcard.model.VCardAvatarPhoto

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
        val avatarPhoto: VCardAvatarPhoto?,
        val entryCount: Int,
        val singleDisplayName: String?,
        val normalizedDestination: String?,
        val locationAddress: String?,
    ) : ConversationVCardAttachmentMetadata
}
