package com.android.messaging.ui.conversation.v2.messages.model.message

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.android.messaging.util.ContentType

@Immutable
internal data class ConversationMessagePartUiModel(
    val contentType: String,
    val text: String?,
    val contentUri: Uri?,
    val width: Int,
    val height: Int,
) {
    val hasCaptionText: Boolean by lazy(mode = LazyThreadSafetyMode.NONE) {
        !text.isNullOrBlank()
    }

    val hasRenderableContentUri: Boolean by lazy(mode = LazyThreadSafetyMode.NONE) {
        contentUri != null
    }

    val isAudioAttachment: Boolean by lazy(mode = LazyThreadSafetyMode.NONE) {
        ContentType.isAudioType(contentType)
    }

    val isImageAttachment: Boolean by lazy(mode = LazyThreadSafetyMode.NONE) {
        ContentType.isImageType(contentType)
    }

    val isMediaAttachment: Boolean by lazy(mode = LazyThreadSafetyMode.NONE) {
        ContentType.isMediaType(contentType)
    }

    val isSupportedAttachment: Boolean by lazy(mode = LazyThreadSafetyMode.NONE) {
        isImageAttachment ||
            isVideoAttachment ||
            isAudioAttachment ||
            isVCardAttachment
    }

    val isTextPart: Boolean by lazy(mode = LazyThreadSafetyMode.NONE) {
        ContentType.isTextType(contentType)
    }

    val isVCardAttachment: Boolean by lazy(mode = LazyThreadSafetyMode.NONE) {
        ContentType.isVCardType(contentType)
    }

    val isVideoAttachment: Boolean by lazy(mode = LazyThreadSafetyMode.NONE) {
        ContentType.isVideoType(contentType)
    }
}
