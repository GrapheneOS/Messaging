package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.ui.common.components.attachment.VCardAttachmentKind
import com.android.messaging.util.ContentType

@Immutable
internal sealed interface ShareAttachmentUiModel {
    val id: String

    @Immutable
    data class Media(
        override val id: String,
        val contentType: String,
        val isVideo: Boolean,
    ) : ShareAttachmentUiModel

    @Immutable
    data class Audio(
        override val id: String,
        val durationMillis: Long,
    ) : ShareAttachmentUiModel

    @Immutable
    data class VCard(
        override val id: String,
        val title: String?,
        val kind: VCardAttachmentKind,
    ) : ShareAttachmentUiModel
}

internal fun ConversationDraftAttachment.toShareAttachmentUiModel(): ShareAttachmentUiModel {
    return when {
        ContentType.isVideoType(contentType) -> ShareAttachmentUiModel.Media(
            id = contentUri,
            contentType = contentType,
            isVideo = true,
        )

        ContentType.isAudioType(contentType) -> ShareAttachmentUiModel.Audio(
            id = contentUri,
            durationMillis = durationMillis ?: 0L,
        )

        ContentType.isVCardType(contentType) -> ShareAttachmentUiModel.VCard(
            id = contentUri,
            title = displayName?.substringBeforeLast(delimiter = '.'),
            kind = VCardAttachmentKind.Contact,
        )

        else -> ShareAttachmentUiModel.Media(
            id = contentUri,
            contentType = contentType,
            isVideo = false,
        )
    }
}
