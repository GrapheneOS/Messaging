package com.android.messaging.ui.conversation.attachment.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.data.vcard.model.VCardAvatarPhoto

@Immutable
internal data class ConversationVCardAttachmentUiModel(
    val type: ConversationVCardAttachmentType,
    val avatarPhoto: VCardAvatarPhoto? = null,
    val normalizedDestination: String? = null,
    val titleText: String? = null,
    val titleTextResId: Int? = null,
    val subtitleText: String? = null,
    val subtitleTextResId: Int? = null,
)
