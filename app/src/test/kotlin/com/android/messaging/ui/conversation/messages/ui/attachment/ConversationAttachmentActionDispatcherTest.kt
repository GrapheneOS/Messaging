package com.android.messaging.ui.conversation.messages.ui.attachment

import android.net.Uri
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentOpenAction
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationAttachmentActionDispatcherTest {

    @Test
    fun dispatchOpenContent_callsAttachmentCallback() {
        var openedContentType: String? = null
        var openedContentUri: String? = null
        var externalUri: String? = null

        dispatchConversationAttachmentOpenAction(
            action = ConversationAttachmentOpenAction.OpenContent(
                contentType = "image/jpeg",
                contentUri = CONTENT_URI,
            ),
            onAttachmentClick = { contentType, contentUri, _ ->
                openedContentType = contentType
                openedContentUri = contentUri
            },
            onExternalUriClick = { uri -> externalUri = uri },
        )

        assertEquals("image/jpeg", openedContentType)
        assertEquals(CONTENT_URI, openedContentUri)
        assertNull(externalUri)
    }

    @Test
    fun dispatchOpenExternal_callsExternalUriCallback() {
        var openedContentUri: String? = null
        var externalUri: String? = null

        dispatchConversationAttachmentOpenAction(
            action = ConversationAttachmentOpenAction.OpenExternal(uri = EXTERNAL_URI),
            onAttachmentClick = { _, contentUri, _ -> openedContentUri = contentUri },
            onExternalUriClick = { uri -> externalUri = uri },
        )

        assertNull(openedContentUri)
        assertEquals(EXTERNAL_URI, externalUri)
    }

    @Test
    fun mediaAttachmentOpenAction_opensContent() {
        val action = ConversationMessageAttachment.Media(
            key = "image",
            part = ConversationMessagePartUiModel.Attachment.Image(
                text = null,
                contentType = "image/jpeg",
                contentUri = Uri.parse(CONTENT_URI),
                width = 640,
                height = 480,
            ),
        ).toConversationAttachmentOpenActionOrNull()

        assertEquals(
            ConversationAttachmentOpenAction.OpenContent(
                contentType = "image/jpeg",
                contentUri = CONTENT_URI,
            ),
            action,
        )
    }

    @Test
    fun unsupportedAttachmentWithoutContentUri_hasNoOpenAction() {
        val action = ConversationMessageAttachment.Unsupported(
            key = "unsupported",
            part = ConversationMessagePartUiModel.Attachment.File(
                text = null,
                contentType = "application/octet-stream",
                contentUri = null,
                width = 0,
                height = 0,
            ),
        ).toConversationAttachmentOpenActionOrNull()

        assertNull(action)
    }

    @Test
    fun youTubePreviewOpenAction_opensExternalSourceUrl() {
        val action = ConversationMessageAttachment.YouTubePreview(
            key = "youtube",
            sourceUrl = EXTERNAL_URI,
            thumbnailUrl = "https://img.youtube.com/vi/abc/0.jpg",
        ).toConversationAttachmentOpenActionOrNull()

        assertEquals(
            ConversationAttachmentOpenAction.OpenExternal(uri = EXTERNAL_URI),
            action,
        )
    }

    private companion object {
        private const val CONTENT_URI = "content://mms/part/image-1"
        private const val EXTERNAL_URI = "https://www.youtube.com/watch?v=abc"
    }
}
