package com.android.messaging.ui.conversationlist.ui.item

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import com.android.messaging.R
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.ui.common.components.TextWithTrailingContent
import com.android.messaging.ui.common.components.attachment.MediaThumbnail
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListPreviewUiModel

private const val PREVIEW_INLINE_CONTENT_ID = "preview"
private const val FAILED_INLINE_CONTENT_ID = "failed"

@Composable
internal fun ConversationListItemSnippet(
    item: ConversationListItemUiModel,
    text: String,
    fontWeight: FontWeight,
    fontStyle: FontStyle?,
    color: Color,
) {
    val hasPreviewThumbnail = item.snippet.preview?.hasThumbnail() ?: false
    val hasFailedIcon = item.status is ConversationListMessageStatus.Error
    val previewPlaceholderSize = with(LocalDensity.current) {
        ItemPreviewThumbnailSize.toSp()
    }

    val annotatedText = buildAnnotatedString {
        if (hasPreviewThumbnail) {
            appendInlineContent(id = PREVIEW_INLINE_CONTENT_ID)
            append(" ")
        }

        append(text)
    }

    val inlineContent = buildMap {
        if (hasPreviewThumbnail) {
            put(
                key = PREVIEW_INLINE_CONTENT_ID,
                value = InlineTextContent(
                    placeholder = Placeholder(
                        width = previewPlaceholderSize,
                        height = previewPlaceholderSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    ConversationListItemPreviewThumbnail(preview = item.snippet.preview)
                },
            )
        }
    }
    val failedIconContent: @Composable () -> Unit = {
        ConversationListItemFailedInlineIcon(
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            color = color,
        )
    }

    TextWithTrailingContent(
        text = annotatedText,
        inlineContent = inlineContent,
        trailingSpacing = ItemBadgeSpacing,
        trailingContent = failedIconContent.takeIf { hasFailedIcon },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ConversationListItemFailedInlineIcon(
    fontWeight: FontWeight,
    fontStyle: FontStyle?,
    color: Color,
) {
    val failedPlaceholderSize = with(LocalDensity.current) {
        ItemBadgeIconSize.toSp()
    }
    val annotatedText = buildAnnotatedString {
        appendInlineContent(id = FAILED_INLINE_CONTENT_ID)
    }
    val inlineContent = mapOf(
        FAILED_INLINE_CONTENT_ID to InlineTextContent(
            placeholder = Placeholder(
                width = failedPlaceholderSize,
                height = failedPlaceholderSize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) {
            ConversationListItemFailedIcon()
        },
    )

    Text(
        text = annotatedText,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ConversationListItemFailedIcon() {
    Icon(
        imageVector = Icons.Default.Error,
        contentDescription = null,
        modifier = Modifier.size(ItemBadgeIconSize),
        tint = MaterialTheme.colorScheme.error,
    )
}

@StringRes
internal fun ConversationListPreviewUiModel.snippetLabelResId(): Int {
    return when (this) {
        is ConversationListPreviewUiModel.Audio -> R.string.conversation_list_snippet_audio_clip
        is ConversationListPreviewUiModel.Image -> R.string.conversation_list_snippet_picture
        is ConversationListPreviewUiModel.Video -> R.string.conversation_list_snippet_video
        is ConversationListPreviewUiModel.VCard -> R.string.conversation_list_snippet_vcard
        is ConversationListPreviewUiModel.File -> R.string.mms_text
    }
}

private fun ConversationListPreviewUiModel.hasThumbnail(): Boolean {
    return when (this) {
        is ConversationListPreviewUiModel.Image -> true
        is ConversationListPreviewUiModel.Video -> true
        is ConversationListPreviewUiModel.Audio -> false
        is ConversationListPreviewUiModel.File -> false
        is ConversationListPreviewUiModel.VCard -> false
    }
}

@Composable
private fun ConversationListItemPreviewThumbnail(preview: ConversationListPreviewUiModel?) {
    val contentUri: String
    val contentType: String

    when (preview) {
        is ConversationListPreviewUiModel.Image -> {
            contentUri = preview.contentUri
            contentType = preview.contentType
        }

        is ConversationListPreviewUiModel.Video -> {
            contentUri = preview.contentUri
            contentType = preview.contentType
        }

        else -> return
    }

    val thumbnailSizePx = with(LocalDensity.current) {
        ItemPreviewThumbnailSize.roundToPx()
    }

    MediaThumbnail(
        modifier = Modifier
            .size(ItemPreviewThumbnailSize)
            .clip(RoundedCornerShape(ItemPreviewThumbnailCornerRadius)),
        contentUri = contentUri,
        contentType = contentType,
        size = IntSize(
            width = thumbnailSizePx,
            height = thumbnailSizePx,
        ),
    )
}
