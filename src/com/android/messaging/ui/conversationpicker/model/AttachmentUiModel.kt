package com.android.messaging.ui.conversationpicker.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.ui.common.components.attachment.VCardAttachmentKind
import com.android.messaging.ui.common.components.mediapreview.MediaPreviewItem
import com.android.messaging.util.ContentType

@Immutable
internal sealed interface AttachmentUiModel {
    val id: String

    @Immutable
    data class Media(
        override val id: String,
        val contentType: String,
        val isVideo: Boolean,
    ) : AttachmentUiModel

    @Immutable
    data class Audio(
        override val id: String,
        val durationMillis: Long,
    ) : AttachmentUiModel

    @Immutable
    data class VCard(
        override val id: String,
        val title: String?,
        val kind: VCardAttachmentKind,
    ) : AttachmentUiModel
}

internal fun ConversationDraftAttachment.toAttachmentUiModel(): AttachmentUiModel {
    return when {
        ContentType.isVideoType(contentType) -> AttachmentUiModel.Media(
            id = contentUri,
            contentType = contentType,
            isVideo = true,
        )

        ContentType.isAudioType(contentType) -> AttachmentUiModel.Audio(
            id = contentUri,
            durationMillis = durationMillis ?: 0L,
        )

        ContentType.isVCardType(contentType) -> AttachmentUiModel.VCard(
            id = contentUri,
            title = displayName?.substringBeforeLast(delimiter = '.'),
            kind = VCardAttachmentKind.Contact,
        )

        else -> AttachmentUiModel.Media(
            id = contentUri,
            contentType = contentType,
            isVideo = false,
        )
    }
}

internal fun ConversationDraftAttachment.toMediaPreviewItem(): MediaPreviewItem {
    return MediaPreviewItem(
        contentUri = contentUri,
        contentType = contentType,
        isVideo = ContentType.isVideoType(contentType),
    )
}
