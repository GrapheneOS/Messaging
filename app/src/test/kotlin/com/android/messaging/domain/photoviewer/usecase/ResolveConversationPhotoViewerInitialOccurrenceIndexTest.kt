package com.android.messaging.domain.photoviewer.usecase

import androidx.core.net.toUri
import com.android.messaging.domain.photoviewer.model.ConversationPhotoViewerAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ResolveConversationPhotoViewerInitialOccurrenceIndexTest {

    private val resolveIndex = ResolveConversationPhotoViewerInitialOccurrenceIndexImpl(
        normalizePhotoViewerUri = NormalizePhotoViewerUriImpl(),
    )

    @Test
    fun invoke_whenClickedUriHasDuplicates_returnsClickedOccurrenceIndex() {
        val result = resolveIndex(
            partId = "part-3",
            contentUri = DUPLICATE_URI.toUri(),
            attachments = sequenceOf(
                photoAttachment(partId = "part-1", contentUri = DUPLICATE_URI),
                photoAttachment(partId = "part-2", contentUri = DUPLICATE_URI),
                photoAttachment(partId = "part-3", contentUri = DUPLICATE_URI),
            ),
        )

        assertEquals(2, result)
    }

    @Test
    fun invoke_whenClickedUriIsDistinct_returnsZero() {
        val result = resolveIndex(
            partId = "part-2",
            contentUri = "content://example/content/2".toUri(),
            attachments = sequenceOf(
                photoAttachment(partId = "part-1", contentUri = "content://example/content/1"),
                photoAttachment(partId = "part-2", contentUri = "content://example/content/2"),
                photoAttachment(partId = "part-3", contentUri = "content://example/content/3"),
            ),
        )

        assertEquals(0, result)
    }

    @Test
    fun invoke_whenUrisOnlyDifferByQueryOrFragment_treatsUrisAsDuplicates() {
        val result = resolveIndex(
            partId = "part-3",
            contentUri = "$DUPLICATE_URI#preview".toUri(),
            attachments = sequenceOf(
                photoAttachment(partId = "part-1", contentUri = DUPLICATE_URI),
                photoAttachment(partId = "part-2", contentUri = "$DUPLICATE_URI?version=2"),
                photoAttachment(partId = "part-3", contentUri = "$DUPLICATE_URI#preview"),
            ),
        )

        assertEquals(2, result)
    }

    @Test
    fun invoke_whenPartIdIsBlank_returnsZero() {
        val result = resolveIndex(
            partId = "",
            contentUri = DUPLICATE_URI.toUri(),
            attachments = sequenceOf(
                photoAttachment(partId = "part-1", contentUri = DUPLICATE_URI),
                photoAttachment(partId = "part-2", contentUri = DUPLICATE_URI),
            ),
        )

        assertEquals(0, result)
    }

    @Test
    fun invoke_whenEarlierPartIdIsBlank_countsEarlierOccurrence() {
        val result = resolveIndex(
            partId = "part-2",
            contentUri = DUPLICATE_URI.toUri(),
            attachments = sequenceOf(
                photoAttachment(partId = "", contentUri = DUPLICATE_URI),
                photoAttachment(partId = "part-2", contentUri = DUPLICATE_URI),
            ),
        )

        assertEquals(1, result)
    }

    @Test
    fun invoke_whenClickedPartIsReached_doesNotConsumeLaterAttachments() {
        var laterAttachmentConsumed = false

        val result = resolveIndex(
            partId = "part-2",
            contentUri = DUPLICATE_URI.toUri(),
            attachments = sequence {
                yield(photoAttachment(partId = "part-1", contentUri = DUPLICATE_URI))
                yield(photoAttachment(partId = "part-2", contentUri = DUPLICATE_URI))
                laterAttachmentConsumed = true
                yield(photoAttachment(partId = "part-3", contentUri = DUPLICATE_URI))
            },
        )

        assertEquals(1, result)
        assertFalse(laterAttachmentConsumed)
    }

    private fun photoAttachment(
        partId: String,
        contentUri: String,
    ): ConversationPhotoViewerAttachment {
        return ConversationPhotoViewerAttachment(
            partId = partId,
            contentUri = contentUri.toUri(),
        )
    }

    private companion object {
        private const val DUPLICATE_URI = "content://example/content/shared"
    }
}
