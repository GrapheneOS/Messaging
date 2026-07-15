package com.android.messaging.ui.conversation.messages.ui.attachment.rendering

import android.net.Uri
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentOpenAction
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentSections
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import kotlinx.collections.immutable.persistentListOf

internal const val MESSAGE_ATTACHMENTS_TAG = "message-attachments-under-test"
internal const val INLINE_ROW_TAG = "inline-row-under-test"
internal const val STANDALONE_VISUAL_TAG = "standalone-visual-under-test"
internal const val IMAGE_KEY = "image-1"
internal const val IMAGE_CONTENT_URI = "content://mms/part/image-1"
internal const val IMAGE_CONTENT_TYPE = "image/jpeg"
internal const val SECOND_IMAGE_KEY = "image-2"
internal const val SECOND_IMAGE_CONTENT_URI = "content://mms/part/image-2"
internal const val THIRD_IMAGE_KEY = "image-3"
internal const val THIRD_IMAGE_CONTENT_URI = "content://mms/part/image-3"
internal const val VIDEO_KEY = "video-1"
internal const val VIDEO_CONTENT_URI = "content://mms/part/video-1"
internal const val VIDEO_CONTENT_TYPE = "video/mp4"
internal const val AUDIO_KEY = "audio-1"
internal const val AUDIO_CONTENT_URI = "content://mms/part/audio-1"
internal const val AUDIO_CONTENT_TYPE = "audio/x-wav"
internal const val AUDIO_TITLE = "Voice message"
internal const val AUDIO_DURATION = "00:18"
internal const val FILE_KEY = "file-1"
internal const val FILE_CONTENT_URI = "content://mms/part/file-1"
internal const val FILE_CONTENT_TYPE = "application/pdf"
internal const val FILE_TITLE = "application/pdf"
internal const val UNSUPPORTED_KEY = "unsupported-1"
internal const val UNSUPPORTED_CONTENT_URI = "content://mms/part/unsupported-1"
internal const val UNSUPPORTED_CONTENT_TYPE = "application/octet-stream"
internal const val VCARD_KEY = "vcard-1"
internal const val VCARD_CONTENT_URI = "content://mms/part/vcard-1"
internal const val VCARD_CONTENT_TYPE = "text/x-vCard"
internal const val VCARD_TITLE = "Sam Rivera"
internal const val VCARD_SUBTITLE = "sam@example.com"
internal const val YOUTUBE_KEY = "youtube-1"
internal const val YOUTUBE_SOURCE_URI = "https://www.youtube.com/watch?v=abc"
internal const val YOUTUBE_THUMBNAIL_URI = "https://img.youtube.com/vi/abc/0.jpg"
internal val ATTACHMENT_WIDTH = 240.dp

internal fun emptySections(): ConversationAttachmentSections {
    return ConversationAttachmentSections(
        galleryVisualAttachments = persistentListOf(),
        trailingItems = persistentListOf(),
    )
}

internal fun gallerySections(
    vararg attachments: ConversationMessageAttachment,
): ConversationAttachmentSections {
    return ConversationAttachmentSections(
        galleryVisualAttachments = persistentListOf(*attachments),
        trailingItems = persistentListOf(),
    )
}

internal fun trailingSections(
    vararg items: ConversationAttachmentItem,
): ConversationAttachmentSections {
    return ConversationAttachmentSections(
        galleryVisualAttachments = persistentListOf(),
        trailingItems = persistentListOf(*items),
    )
}

internal fun imageAttachment(
    key: String = IMAGE_KEY,
    contentUri: String = IMAGE_CONTENT_URI,
    width: Int = 640,
    height: Int = 480,
): ConversationMessageAttachment.Media {
    return ConversationMessageAttachment.Media(
        key = key,
        part = ConversationMessagePartUiModel.Attachment.Image(
            text = null,
            contentType = IMAGE_CONTENT_TYPE,
            contentUri = Uri.parse(contentUri),
            width = width,
            height = height,
        ),
    )
}

internal fun videoAttachment(
    key: String = VIDEO_KEY,
    contentUri: String = VIDEO_CONTENT_URI,
    width: Int = 1280,
    height: Int = 720,
): ConversationMessageAttachment.Media {
    return ConversationMessageAttachment.Media(
        key = key,
        part = ConversationMessagePartUiModel.Attachment.Video(
            text = null,
            contentType = VIDEO_CONTENT_TYPE,
            contentUri = Uri.parse(contentUri),
            width = width,
            height = height,
        ),
    )
}

internal fun audioAttachment(
    key: String = AUDIO_KEY,
    contentUri: String = AUDIO_CONTENT_URI,
): ConversationMessageAttachment.Media {
    return ConversationMessageAttachment.Media(
        key = key,
        part = ConversationMessagePartUiModel.Attachment.Audio(
            text = null,
            contentType = AUDIO_CONTENT_TYPE,
            contentUri = Uri.parse(contentUri),
            width = 0,
            height = 0,
        ),
    )
}

internal fun fileAttachment(
    key: String = FILE_KEY,
    contentUri: String? = FILE_CONTENT_URI,
    contentType: String = FILE_CONTENT_TYPE,
): ConversationMessageAttachment.Media {
    return ConversationMessageAttachment.Media(
        key = key,
        part = filePart(
            contentUri = contentUri,
            contentType = contentType,
        ),
    )
}

internal fun unsupportedAttachment(
    key: String = UNSUPPORTED_KEY,
    contentUri: String? = UNSUPPORTED_CONTENT_URI,
    contentType: String = UNSUPPORTED_CONTENT_TYPE,
): ConversationMessageAttachment.Unsupported {
    return ConversationMessageAttachment.Unsupported(
        key = key,
        part = filePart(
            contentUri = contentUri,
            contentType = contentType,
        ),
    )
}

internal fun youTubeAttachment(
    key: String = YOUTUBE_KEY,
    sourceUrl: String = YOUTUBE_SOURCE_URI,
    thumbnailUrl: String = YOUTUBE_THUMBNAIL_URI,
): ConversationMessageAttachment.YouTubePreview {
    return ConversationMessageAttachment.YouTubePreview(
        key = key,
        sourceUrl = sourceUrl,
        thumbnailUrl = thumbnailUrl,
    )
}

internal fun standaloneVisualItem(
    attachment: ConversationMessageAttachment,
): ConversationAttachmentItem.StandaloneVisual {
    return ConversationAttachmentItem.StandaloneVisual(
        key = attachment.key,
        attachment = attachment,
    )
}

internal fun inlineItem(
    attachment: ConversationInlineAttachment,
): ConversationAttachmentItem.Inline {
    return ConversationAttachmentItem.Inline(
        key = attachment.key,
        attachment = attachment,
    )
}

internal fun audioInlineAttachment(
    key: String = AUDIO_KEY,
    contentUri: String = AUDIO_CONTENT_URI,
    titleText: String? = AUDIO_TITLE,
): ConversationInlineAttachment.Audio {
    return ConversationInlineAttachment.Audio(
        key = key,
        contentUri = contentUri,
        openAction = ConversationAttachmentOpenAction.OpenContent(
            contentType = AUDIO_CONTENT_TYPE,
            contentUri = contentUri,
        ),
        titleText = titleText,
        titleTextResId = R.string.audio_attachment_content_description,
    )
}

internal fun fileInlineAttachment(
    key: String = FILE_KEY,
    openAction: ConversationAttachmentOpenAction? = ConversationAttachmentOpenAction.OpenContent(
        contentType = FILE_CONTENT_TYPE,
        contentUri = FILE_CONTENT_URI,
    ),
    subtitleTextResId: Int? = null,
    titleText: String? = FILE_TITLE,
    titleTextResId: Int? = R.string.notification_file,
): ConversationInlineAttachment.File {
    return ConversationInlineAttachment.File(
        key = key,
        openAction = openAction,
        subtitleTextResId = subtitleTextResId,
        titleText = titleText,
        titleTextResId = titleTextResId,
    )
}

internal fun vCardInlineAttachment(
    key: String = VCARD_KEY,
    openAction: ConversationAttachmentOpenAction? = ConversationAttachmentOpenAction.OpenContent(
        contentType = VCARD_CONTENT_TYPE,
        contentUri = VCARD_CONTENT_URI,
    ),
    titleText: String? = VCARD_TITLE,
    subtitleText: String? = VCARD_SUBTITLE,
): ConversationInlineAttachment.VCard {
    return ConversationInlineAttachment.VCard(
        key = key,
        contentUri = VCARD_CONTENT_URI,
        openAction = openAction,
        type = ConversationVCardAttachmentType.CONTACT,
        avatarPhoto = null,
        normalizedDestination = null,
        titleText = titleText,
        titleTextResId = R.string.notification_vcard,
        subtitleText = subtitleText,
        subtitleTextResId = R.string.vcard_tap_hint,
    )
}

internal fun vCardMediaAttachment(
    key: String = VCARD_KEY,
): ConversationMessageAttachment.Media {
    return ConversationMessageAttachment.Media(
        key = key,
        part = ConversationMessagePartUiModel.Attachment.VCard(
            text = null,
            contentType = VCARD_CONTENT_TYPE,
            contentUri = Uri.parse(VCARD_CONTENT_URI),
            width = 0,
            height = 0,
            vCardUiModel = ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = VCARD_TITLE,
                subtitleText = VCARD_SUBTITLE,
            ),
        ),
    )
}

private fun filePart(
    contentUri: String?,
    contentType: String,
): ConversationMessagePartUiModel.Attachment.File {
    return ConversationMessagePartUiModel.Attachment.File(
        text = null,
        contentType = contentType,
        contentUri = contentUri?.let(Uri::parse),
        width = 0,
        height = 0,
    )
}
