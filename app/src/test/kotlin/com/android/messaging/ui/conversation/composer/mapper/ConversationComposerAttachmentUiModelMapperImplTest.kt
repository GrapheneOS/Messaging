package com.android.messaging.ui.conversation.composer.mapper

import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachmentKind
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationComposerAttachmentUiModelMapperImplTest {

    @Test
    fun map_returnsTypedUiModelsForResolvedAndPendingAttachments() {
        val vCardUiModel = ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleText = "Sam Rivera",
            subtitleText = "555-000-8901",
        )
        val vCardAttachmentUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
        every {
            vCardAttachmentUiModelMapper.map(
                metadata = ConversationVCardAttachmentMetadata.Loading,
            )
        } returns vCardUiModel
        val mapper = ConversationComposerAttachmentUiModelMapperImpl(
            conversationVCardAttachmentUiModelMapper = vCardAttachmentUiModelMapper,
        )

        val uiModels = mapper.map(
            attachments = listOf(
                ConversationDraftAttachment(
                    contentType = "audio/mpeg",
                    contentUri = "content://attachments/audio/1",
                ),
                ConversationDraftAttachment(
                    contentType = "image/jpeg",
                    contentUri = "content://attachments/image/1",
                    width = 640,
                    height = 480,
                ),
                ConversationDraftAttachment(
                    contentType = "text/x-vCard",
                    contentUri = "content://attachments/vcard/1",
                ),
                ConversationDraftAttachment(
                    contentType = "video/mp4",
                    contentUri = "content://attachments/video/1",
                    width = 1920,
                    height = 1080,
                ),
                ConversationDraftAttachment(
                    contentType = "application/pdf",
                    contentUri = "content://attachments/file/1",
                ),
            ),
            pendingAttachments = listOf(
                ConversationDraftPendingAttachment(
                    pendingAttachmentId = "pending-1",
                    contentType = "image/jpeg",
                    contentUri = "content://pending/1",
                    displayName = "pending.jpg",
                ),
            ),
        )

        assertEquals(
            listOf(
                ComposerAttachmentUiModel.Resolved.Audio(
                    key = "content://attachments/audio/1",
                    contentType = "audio/mpeg",
                    contentUri = "content://attachments/audio/1",
                    durationMillis = 0L,
                ),
                ComposerAttachmentUiModel.Resolved.VisualMedia.Image(
                    key = "content://attachments/image/1",
                    contentType = "image/jpeg",
                    contentUri = "content://attachments/image/1",
                    captionText = "",
                    width = 640,
                    height = 480,
                ),
                ComposerAttachmentUiModel.Resolved.VCard(
                    key = "content://attachments/vcard/1",
                    contentType = "text/x-vCard",
                    contentUri = "content://attachments/vcard/1",
                    vCardUiModel = vCardUiModel,
                ),
                ComposerAttachmentUiModel.Resolved.VisualMedia.Video(
                    key = "content://attachments/video/1",
                    contentType = "video/mp4",
                    contentUri = "content://attachments/video/1",
                    captionText = "",
                    width = 1920,
                    height = 1080,
                ),
                ComposerAttachmentUiModel.Resolved.File(
                    key = "content://attachments/file/1",
                    contentType = "application/pdf",
                    contentUri = "content://attachments/file/1",
                ),
                ComposerAttachmentUiModel.Pending.Generic(
                    key = "pending-1",
                    contentType = "image/jpeg",
                    contentUri = "content://pending/1",
                    displayName = "pending.jpg",
                ),
            ),
            uiModels,
        )
        verify(exactly = 1) {
            vCardAttachmentUiModelMapper.map(
                metadata = ConversationVCardAttachmentMetadata.Loading,
            )
        }
    }

    @Test
    fun map_audioFinalizingKind_mapsToAudioFinalizingPendingUiModel() {
        val mapper = ConversationComposerAttachmentUiModelMapperImpl(
            conversationVCardAttachmentUiModelMapper = mockk(),
        )

        val uiModels = mapper.map(
            attachments = emptyList(),
            pendingAttachments = listOf(
                ConversationDraftPendingAttachment(
                    pendingAttachmentId = "pending-audio-finalizing-1",
                    contentType = "audio/3gpp",
                    contentUri = "pending://audio/pending-audio-finalizing-1",
                    kind = ConversationDraftPendingAttachmentKind.AudioFinalizing,
                ),
            ),
        )

        assertEquals(
            ComposerAttachmentUiModel.Pending.AudioFinalizing(
                key = "pending-audio-finalizing-1",
                contentType = "audio/3gpp",
                contentUri = "pending://audio/pending-audio-finalizing-1",
                displayName = "",
            ),
            uiModels.single(),
        )
    }
}
