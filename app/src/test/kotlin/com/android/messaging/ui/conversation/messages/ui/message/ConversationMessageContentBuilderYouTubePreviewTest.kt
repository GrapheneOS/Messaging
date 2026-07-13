package com.android.messaging.ui.conversation.messages.ui.message

import androidx.core.net.toUri
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageContentBuilderYouTubePreviewTest {

    @Test
    fun buildContentSkipsYouTubePreviewWhenDisabled() {
        val content = buildConversationMessageContent(
            message = message(
                text = MESSAGE_TEXT_WITH_YOUTUBE_LINK,
            ),
            subjectText = null,
            youTubeLinkPreviewsEnabled = false,
        )

        assertTrue(content.attachments.isEmpty())
    }

    @Test
    fun buildContentAppendsYouTubePreviewWhenEnabled() {
        val content = buildConversationMessageContent(
            message = message(
                text = MESSAGE_TEXT_WITH_YOUTUBE_LINK,
            ),
            subjectText = null,
            youTubeLinkPreviewsEnabled = true,
        )

        val preview = content.attachments.single()
            as ConversationMessageAttachment.YouTubePreview
        assertEquals(YOUTUBE_VIDEO_URL, preview.sourceUrl)
    }

    @Test
    fun buildContentSkipsYouTubePreviewWhenMessageHasImageAttachment() {
        val content = buildConversationMessageContent(
            message = message(
                text = MESSAGE_TEXT_WITH_YOUTUBE_LINK,
                parts = persistentListOf(
                    ConversationMessagePartUiModel.Attachment.Image(
                        text = null,
                        contentType = "image/jpeg",
                        contentUri = "content://example.test/image/1".toUri(),
                        width = 320,
                        height = 240,
                        partId = "part-1",
                    ),
                ),
            ),
            subjectText = null,
            youTubeLinkPreviewsEnabled = true,
        )

        assertTrue(
            content.attachments.none { attachment ->
                attachment is ConversationMessageAttachment.YouTubePreview
            },
        )
    }

    private fun message(
        text: String,
        parts: ImmutableList<ConversationMessagePartUiModel> = persistentListOf(),
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = "message-1",
            conversationId = "conversation-1",
            text = text,
            parts = parts,
            sentTimestamp = 0L,
            receivedTimestamp = 0L,
            displayTimestamp = 0L,
            status = ConversationMessageUiModel.Status.Incoming.Complete,
            isIncoming = true,
            senderDisplayName = null,
            senderAvatarUri = null,
            senderContactId = 0L,
            senderContactLookupKey = null,
            senderNormalizedDestination = null,
            senderParticipantId = null,
            selfParticipantId = null,
            canClusterWithPrevious = false,
            canClusterWithNext = false,
            canCopyMessageToClipboard = false,
            canDownloadMessage = false,
            canForwardMessage = false,
            canResendMessage = false,
            canSaveAttachments = false,
            mmsDownload = null,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.SMS,
        )
    }

    private companion object {
        private const val YOUTUBE_VIDEO_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        private const val MESSAGE_TEXT_WITH_YOUTUBE_LINK = "Watch this: $YOUTUBE_VIDEO_URL"
    }
}
