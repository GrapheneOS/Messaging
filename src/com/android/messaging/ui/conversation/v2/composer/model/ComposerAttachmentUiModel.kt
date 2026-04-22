package com.android.messaging.ui.conversation.v2.composer.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentUiModel

@Immutable
internal sealed interface ComposerAttachmentUiModel {
    val key: String
    val contentType: String
    val contentUri: String

    @Immutable
    data class Pending(
        override val key: String,
        override val contentType: String,
        override val contentUri: String,
        val displayName: String,
    ) : ComposerAttachmentUiModel

    @Immutable
    sealed interface Resolved : ComposerAttachmentUiModel {

        @Immutable
        sealed interface VisualMedia : Resolved {
            val captionText: String
            val width: Int?
            val height: Int?

            @Immutable
            data class Image(
                override val key: String,
                override val contentType: String,
                override val contentUri: String,
                override val captionText: String,
                override val width: Int?,
                override val height: Int?,
            ) : VisualMedia

            @Immutable
            data class Video(
                override val key: String,
                override val contentType: String,
                override val contentUri: String,
                override val captionText: String,
                override val width: Int?,
                override val height: Int?,
            ) : VisualMedia
        }

        @Immutable
        data class Audio(
            override val key: String,
            override val contentType: String,
            override val contentUri: String,
            val durationMillis: Long,
        ) : Resolved

        @Immutable
        data class File(
            override val key: String,
            override val contentType: String,
            override val contentUri: String,
        ) : Resolved

        @Immutable
        data class VCard(
            override val key: String,
            override val contentType: String,
            override val contentUri: String,
            val vCardUiModel: ConversationVCardAttachmentUiModel,
        ) : Resolved
    }
}
