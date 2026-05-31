package com.android.messaging.ui.conversation.messages.delegate.selection

import android.content.ClipboardManager
import android.net.Uri
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.media.repository.ConversationAttachmentsRepository
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.domain.conversation.usecase.forward.CreateForwardedMessage
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessageSelectionDelegateImpl
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseConversationMessageSelectionDelegateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected fun createHarness(
        actionRequirements: CheckConversationActionRequirements = createActionRequirementsMock(),
    ): DelegateHarness {
        val dispatcher = mainDispatcherRule.testDispatcher
        val scope = TestScope(dispatcher)
        val clipboardManager = mockk<ClipboardManager>(relaxed = true)
        val conversationAttachmentsRepository =
            mockk<ConversationAttachmentsRepository>(relaxed = true)
        val conversationMessagesDelegate = mockk<ConversationMessagesDelegate>()
        val createForwardedMessage = mockk<CreateForwardedMessage>()
        val conversationsRepository = mockk<ConversationsRepository>(relaxed = true)
        val messagesStateFlow = MutableStateFlow<ConversationMessagesUiState>(
            value = ConversationMessagesUiState.Loading,
        )
        val conversationIdFlow = MutableStateFlow<String?>(CONVERSATION_ID)

        every { conversationMessagesDelegate.state } returns messagesStateFlow
        coEvery {
            createForwardedMessage.invoke(any(), any())
        } returns null

        val delegate = ConversationMessageSelectionDelegateImpl(
            checkConversationActionRequirements = actionRequirements,
            clipboardManager = clipboardManager,
            conversationAttachmentsRepository = conversationAttachmentsRepository,
            conversationMessagesDelegate = conversationMessagesDelegate,
            createForwardedMessage = createForwardedMessage,
            conversationsRepository = conversationsRepository,
            defaultDispatcher = dispatcher,
        )
        delegate.bind(
            scope = scope,
            conversationIdFlow = conversationIdFlow,
        )

        return DelegateHarness(
            delegate = delegate,
            clipboardManager = clipboardManager,
            conversationAttachmentsRepository = conversationAttachmentsRepository,
            conversationIdFlow = conversationIdFlow,
            conversationsRepository = conversationsRepository,
            createForwardedMessage = createForwardedMessage,
            messagesStateFlow = messagesStateFlow,
            scope = scope,
        )
    }

    protected fun createActionRequirementsMock(
        initialResult: ConversationActionRequirementsResult =
            ConversationActionRequirementsResult.Ready,
    ): CheckConversationActionRequirements {
        return createActionRequirementsMock(results = listOf(initialResult))
    }

    protected fun createActionRequirementsMock(
        results: List<ConversationActionRequirementsResult>,
    ): CheckConversationActionRequirements {
        val mock = mockk<CheckConversationActionRequirements>()
        every { mock.invoke() } returnsMany results
        return mock
    }

    protected fun createAttachmentPart(): ConversationMessagePartUiModel.Attachment {
        return ConversationMessagePartUiModel.Attachment.Image(
            text = null,
            contentType = IMAGE_ATTACHMENT_CONTENT_TYPE,
            contentUri = Uri.parse(IMAGE_ATTACHMENT_CONTENT_URI),
            width = 640,
            height = 480,
        )
    }

    protected fun createMessageUiModel(
        messageId: String,
        text: String? = "Hello",
        parts: ImmutableList<ConversationMessagePartUiModel> = persistentListOf(),
        canCopyMessageToClipboard: Boolean = false,
        canDownloadMessage: Boolean = false,
        canForwardMessage: Boolean = false,
        canResendMessage: Boolean = false,
        canSaveAttachments: Boolean = false,
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = messageId,
            conversationId = CONVERSATION_ID,
            text = text,
            parts = parts,
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
            canCopyMessageToClipboard = canCopyMessageToClipboard,
            canDownloadMessage = canDownloadMessage,
            canForwardMessage = canForwardMessage,
            canResendMessage = canResendMessage,
            canSaveAttachments = canSaveAttachments,
            mmsDownload = null,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.SMS,
        )
    }

    protected fun createMessagesUiState(
        vararg messages: ConversationMessageUiModel,
    ): ConversationMessagesUiState.Present {
        return ConversationMessagesUiState.Present(
            messages = messages.toList().toPersistentList(),
        )
    }

    protected data class DelegateHarness(
        val delegate: ConversationMessageSelectionDelegateImpl,
        val clipboardManager: ClipboardManager,
        val conversationAttachmentsRepository: ConversationAttachmentsRepository,
        val conversationIdFlow: MutableStateFlow<String?>,
        val conversationsRepository: ConversationsRepository,
        val createForwardedMessage: CreateForwardedMessage,
        val messagesStateFlow: MutableStateFlow<ConversationMessagesUiState>,
        val scope: TestScope,
    ) {
        fun cancel() {
            scope.cancel()
        }
    }

    companion object {
        const val IMAGE_ATTACHMENT_CONTENT_TYPE = "image/jpeg"
        const val IMAGE_ATTACHMENT_CONTENT_URI = "content://media/image/1"
    }
}
