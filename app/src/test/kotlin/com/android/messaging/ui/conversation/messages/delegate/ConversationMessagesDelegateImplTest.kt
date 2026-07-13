package com.android.messaging.ui.conversation.messages.delegate

import androidx.core.net.toUri
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.data.conversation.repository.ConversationVCardMetadataRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.domain.photoviewer.usecase.ResolveConversationPhotoViewerInitialOccurrenceIndex
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessagesDelegateImplTest {

    private val resolveInitialPhotoOccurrenceIndex =
        mockk<ResolveConversationPhotoViewerInitialOccurrenceIndex>()
    private val delegate = ConversationMessagesDelegateImpl(
        conversationsRepository = mockk<ConversationsRepository>(),
        appSettingsRepository = mockk<AppSettingsRepository>(),
        resolveInitialPhotoOccurrenceIndex = resolveInitialPhotoOccurrenceIndex,
        conversationMessageUiModelMapper = mockk<ConversationMessageUiModelMapper>(),
        conversationVCardAttachmentUiModelMapper =
            mockk<ConversationVCardAttachmentUiModelMapper>(),
        conversationVCardMetadataRepository = mockk<ConversationVCardMetadataRepository>(),
        defaultDispatcher = StandardTestDispatcher(),
    )

    @Test
    fun resolvePhotoViewerInitialOccurrenceIndex_whenContentTypeIsImage_usesPhotoResolver() {
        every {
            resolveInitialPhotoOccurrenceIndex.invoke(
                partId = PART_ID,
                contentUri = ATTACHMENT_URI.toUri(),
                attachments = any(),
            )
        } returns SECOND_OCCURRENCE_INDEX

        val result = delegate.resolvePhotoViewerInitialOccurrenceIndex(
            contentType = ContentType.IMAGE_JPEG,
            partId = PART_ID,
            contentUri = ATTACHMENT_URI,
        )

        assertEquals(SECOND_OCCURRENCE_INDEX, result)
        verify(exactly = 1) {
            resolveInitialPhotoOccurrenceIndex.invoke(
                partId = PART_ID,
                contentUri = ATTACHMENT_URI.toUri(),
                attachments = any(),
            )
        }
    }

    @Test
    fun resolvePhotoViewerInitialOccurrenceIndex_whenContentTypeIsNotImage_skipsPhotoResolver() {
        val result = delegate.resolvePhotoViewerInitialOccurrenceIndex(
            contentType = ContentType.VIDEO_MP4,
            partId = PART_ID,
            contentUri = ATTACHMENT_URI,
        )

        assertEquals(0, result)
        verify(exactly = 0) {
            resolveInitialPhotoOccurrenceIndex.invoke(
                partId = any(),
                contentUri = any(),
                attachments = any(),
            )
        }
    }

    private companion object {
        private const val ATTACHMENT_URI = "content://example/attachment/1"
        private const val PART_ID = "part-1"
        private const val SECOND_OCCURRENCE_INDEX = 1
    }
}
