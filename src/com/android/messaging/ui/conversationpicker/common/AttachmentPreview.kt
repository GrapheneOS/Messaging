package com.android.messaging.ui.conversationpicker.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.ui.common.components.attachment.AttachmentPreviewRow
import com.android.messaging.ui.common.components.attachment.AudioAttachmentCell
import com.android.messaging.ui.common.components.attachment.MediaAttachmentCell
import com.android.messaging.ui.common.components.attachment.VCardAttachmentCell
import com.android.messaging.ui.conversationpicker.model.AttachmentUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AttachmentPreview(
    attachments: ImmutableList<AttachmentUiModel>,
    onRemove: (String) -> Unit,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AttachmentPreviewRow(
        attachments = attachments,
        key = { attachment -> attachment.id },
        modifier = modifier.testTag(ATTACHMENT_PREVIEW_LIST_TEST_TAG),
    ) { attachment ->
        val itemTestTag = attachmentItemTestTag(id = attachment.id)
        val removeTestTag = attachmentRemoveButtonTestTag(id = attachment.id)

        when (attachment) {
            is AttachmentUiModel.Media -> {
                MediaAttachmentCell(
                    modifier = Modifier.testTag(itemTestTag),
                    contentUri = attachment.id,
                    contentType = attachment.contentType,
                    isVideo = attachment.isVideo,
                    onClick = { onClick(attachment.id) },
                    onRemove = { onRemove(attachment.id) },
                    removeButtonTestTag = removeTestTag,
                )
            }

            is AttachmentUiModel.Audio -> {
                AudioAttachmentCell(
                    modifier = Modifier.testTag(itemTestTag),
                    durationMillis = attachment.durationMillis,
                    onClick = { onClick(attachment.id) },
                    onRemove = { onRemove(attachment.id) },
                    removeButtonTestTag = removeTestTag,
                )
            }

            is AttachmentUiModel.VCard -> {
                VCardAttachmentCell(
                    modifier = Modifier.testTag(itemTestTag),
                    kind = attachment.kind,
                    avatarUri = null,
                    avatarName = attachment.title,
                    title = attachment.title
                        ?: stringResource(id = R.string.mediapicker_contact_title),
                    subtitle = null,
                    onClick = { onClick(attachment.id) },
                    onRemove = { onRemove(attachment.id) },
                    removeButtonTestTag = removeTestTag,
                )
            }
        }
    }
}
