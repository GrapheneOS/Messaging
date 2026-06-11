package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import android.net.Uri
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.data.conversation.repository.ConversationVCardMetadataRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.domain.photoviewer.usecase.ResolveConversationPhotoViewerInitialOccurrenceIndex
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessagesDelegateImpl
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseConversationMessagesDelegateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected val conversationsRepository = mockk<ConversationsRepository>()
    protected val messageUiModelMapper = mockk<ConversationMessageUiModelMapper>()
    protected val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
    protected val vCardMetadataRepository = mockk<ConversationVCardMetadataRepository>()

    protected fun createDelegate(): ConversationMessagesDelegateImpl {
        return ConversationMessagesDelegateImpl(
            conversationsRepository = conversationsRepository,
            resolveInitialPhotoOccurrenceIndex =
                mockk<ResolveConversationPhotoViewerInitialOccurrenceIndex>(relaxed = true),
            conversationMessageUiModelMapper = messageUiModelMapper,
            conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
            conversationVCardMetadataRepository = vCardMetadataRepository,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    protected fun TestScope.createBoundDelegate(
        conversationIdFlow: StateFlow<String?>,
    ): ConversationMessagesDelegateImpl {
        return createDelegate().also { delegate ->
            delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)
        }
    }

    protected fun givenConversationMessages(
        messages: Flow<List<ConversationMessageData>>,
        conversationId: String = CONVERSATION_ID,
    ) {
        every {
            conversationsRepository.getConversationMessages(conversationId = conversationId)
        } returns messages
    }

    protected fun givenVCardMetadata(
        contentUri: String,
        metadata: Flow<ConversationVCardAttachmentMetadata>,
    ) {
        every {
            vCardMetadataRepository.observeAttachmentMetadata(
                contentUri = contentUri,
                refreshes = any(),
            )
        } returns metadata
    }

    protected fun givenVCardUiModel(
        metadata: ConversationVCardAttachmentMetadata?,
        uiModel: ConversationVCardAttachmentUiModel,
    ) {
        every {
            vCardUiModelMapper.map(metadata = metadata)
        } returns uiModel
    }

    protected fun messagesOf(
        vararg uiModels: ConversationMessageUiModel,
    ): List<ConversationMessageData> {
        return uiModels.map { uiModel ->
            mockk<ConversationMessageData>(relaxed = true).also { data ->
                every { messageUiModelMapper.map(data = data) } returns uiModel
            }
        }
    }

    protected fun messageUiModel(
        messageId: String,
        parts: List<ConversationMessagePartUiModel> = emptyList(),
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = messageId,
            conversationId = CONVERSATION_ID,
            text = "Hello",
            parts = parts.toImmutableList(),
            sentTimestamp = 1L,
            receivedTimestamp = 1L,
            displayTimestamp = 1L,
            status = ConversationMessageUiModel.Status.Outgoing.Complete,
            isIncoming = false,
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

    protected fun textPart(text: String = "body"): ConversationMessagePartUiModel.Text {
        return ConversationMessagePartUiModel.Text(text = text)
    }

    protected fun imagePart(
        contentUri: String = "content://media/image/1",
    ): ConversationMessagePartUiModel.Attachment.Image {
        return ConversationMessagePartUiModel.Attachment.Image(
            text = null,
            contentType = "image/jpeg",
            contentUri = Uri.parse(contentUri),
            width = 640,
            height = 480,
        )
    }

    protected fun audioPart(
        contentUri: String = "content://media/audio/1",
    ): ConversationMessagePartUiModel.Attachment.Audio {
        return ConversationMessagePartUiModel.Attachment.Audio(
            text = null,
            contentType = "audio/mpeg",
            contentUri = Uri.parse(contentUri),
            width = 0,
            height = 0,
        )
    }

    protected fun filePart(
        contentUri: String = "content://media/file/1",
    ): ConversationMessagePartUiModel.Attachment.File {
        return ConversationMessagePartUiModel.Attachment.File(
            text = null,
            contentType = "application/pdf",
            contentUri = Uri.parse(contentUri),
            width = 0,
            height = 0,
        )
    }

    protected fun videoPart(
        contentUri: String = "content://media/video/1",
    ): ConversationMessagePartUiModel.Attachment.Video {
        return ConversationMessagePartUiModel.Attachment.Video(
            text = null,
            contentType = "video/mp4",
            contentUri = Uri.parse(contentUri),
            width = 1280,
            height = 720,
        )
    }

    protected fun vCardPart(
        contentUri: String?,
        vCardUiModel: ConversationVCardAttachmentUiModel = vCardUiModel(titleText = "original"),
    ): ConversationMessagePartUiModel.Attachment.VCard {
        return ConversationMessagePartUiModel.Attachment.VCard(
            text = null,
            contentType = "text/x-vCard",
            contentUri = contentUri?.let(Uri::parse),
            width = 0,
            height = 0,
            vCardUiModel = vCardUiModel,
        )
    }

    protected fun vCardUiModel(titleText: String): ConversationVCardAttachmentUiModel {
        return ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleText = titleText,
        )
    }
}
