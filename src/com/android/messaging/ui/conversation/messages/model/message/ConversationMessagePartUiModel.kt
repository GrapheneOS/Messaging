package com.android.messaging.ui.conversation.messages.model.message

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel

@Immutable
internal sealed interface ConversationMessagePartUiModel {
    val text: String?

    @Immutable
    data class Text(
        override val text: String,
    ) : ConversationMessagePartUiModel

    val hasCaptionText: Boolean
        get() {
            return !text.isNullOrBlank()
        }

    @Immutable
    sealed interface Attachment : ConversationMessagePartUiModel {
        val contentType: String
        val contentUri: Uri?
        val width: Int
        val height: Int

        @Immutable
        data class Audio(
            override val text: String?,
            override val contentType: String,
            override val contentUri: Uri?,
            override val width: Int,
            override val height: Int,
        ) : Attachment

        @Immutable
        data class File(
            override val text: String?,
            override val contentType: String,
            override val contentUri: Uri?,
            override val width: Int,
            override val height: Int,
        ) : Attachment

        @Immutable
        data class Image(
            override val text: String?,
            override val contentType: String,
            override val contentUri: Uri?,
            override val width: Int,
            override val height: Int,
        ) : Attachment

        @Immutable
        data class VCard(
            override val text: String?,
            override val contentType: String,
            override val contentUri: Uri?,
            override val width: Int,
            override val height: Int,
            val vCardUiModel: ConversationVCardAttachmentUiModel,
        ) : Attachment

        @Immutable
        data class Video(
            override val text: String?,
            override val contentType: String,
            override val contentUri: Uri?,
            override val width: Int,
            override val height: Int,
        ) : Attachment
    }
}
