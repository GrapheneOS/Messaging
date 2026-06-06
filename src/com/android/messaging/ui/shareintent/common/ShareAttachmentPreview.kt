package com.android.messaging.ui.shareintent.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.ui.common.components.attachment.AttachmentPreviewRow
import com.android.messaging.ui.common.components.attachment.AudioAttachmentCell
import com.android.messaging.ui.common.components.attachment.MediaAttachmentCell
import com.android.messaging.ui.common.components.attachment.VCardAttachmentCell
import com.android.messaging.ui.shareintent.screen.model.ShareAttachmentUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ShareAttachmentPreview(
    attachments: ImmutableList<ShareAttachmentUiModel>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AttachmentPreviewRow(
        attachments = attachments,
        key = { attachment -> attachment.id },
        modifier = modifier.testTag(SHARE_ATTACHMENT_PREVIEW_LIST_TEST_TAG),
    ) { attachment ->
        val itemTestTag = shareAttachmentItemTestTag(id = attachment.id)
        val removeTestTag = shareAttachmentRemoveButtonTestTag(id = attachment.id)

        when (attachment) {
            is ShareAttachmentUiModel.Media -> {
                MediaAttachmentCell(
                    modifier = Modifier.testTag(itemTestTag),
                    contentUri = attachment.id,
                    contentType = attachment.contentType,
                    isVideo = attachment.isVideo,
                    onClick = {},
                    onRemove = { onRemove(attachment.id) },
                    removeButtonTestTag = removeTestTag,
                )
            }

            is ShareAttachmentUiModel.Audio -> {
                AudioAttachmentCell(
                    modifier = Modifier.testTag(itemTestTag),
                    durationMillis = attachment.durationMillis,
                    onClick = {},
                    onRemove = { onRemove(attachment.id) },
                    removeButtonTestTag = removeTestTag,
                )
            }

            is ShareAttachmentUiModel.VCard -> {
                VCardAttachmentCell(
                    modifier = Modifier.testTag(itemTestTag),
                    kind = attachment.kind,
                    avatarUri = null,
                    title = attachment.title
                        ?: stringResource(id = R.string.mediapicker_contact_title),
                    subtitle = null,
                    onClick = {},
                    onRemove = { onRemove(attachment.id) },
                    removeButtonTestTag = removeTestTag,
                )
            }
        }
    }
}
