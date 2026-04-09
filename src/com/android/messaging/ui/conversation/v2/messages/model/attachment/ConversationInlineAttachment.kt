package com.android.messaging.ui.conversation.v2.messages.model.attachment

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationInlineAttachment(
    val key: String,
    val kind: ConversationInlineAttachmentKind,
    val openAction: ConversationAttachmentOpenAction?,
    val subtitleTextResId: Int?,
    val titleText: String?,
    val titleTextResId: Int?,
)

@Immutable
internal enum class ConversationInlineAttachmentKind {
    AUDIO,
    FILE,
    VCARD,
}
