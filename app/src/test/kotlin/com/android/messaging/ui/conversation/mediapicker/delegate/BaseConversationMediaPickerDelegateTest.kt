package com.android.messaging.ui.conversation.mediapicker.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.media.model.PhotoPickerDraftAttachmentResult
import com.android.messaging.data.media.repository.ConversationAttachmentsRepository
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.mediapicker.mapper.ConversationDraftAttachmentMapper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseConversationMediaPickerDelegateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected fun createDelegate(
        draftDelegate: ConversationDraftDelegate,
        attachmentMapper: ConversationDraftAttachmentMapper,
        attachmentRepository: ConversationAttachmentsRepository,
        defaultDispatcher: CoroutineDispatcher,
    ): ConversationMediaPickerDelegateImpl {
        return ConversationMediaPickerDelegateImpl(
            conversationDraftDelegate = draftDelegate,
            conversationAttachmentsRepository = attachmentRepository,
            conversationDraftAttachmentMapper = attachmentMapper,
            defaultDispatcher = defaultDispatcher,
        )
    }

    protected fun createMediaPickerDraftDelegateMock(): ConversationDraftDelegate {
        val stateFlow = MutableStateFlow(ConversationDraftState())
        val draftDelegate = mockk<ConversationDraftDelegate>(relaxed = true)
        every { draftDelegate.state } returns stateFlow
        every { draftDelegate.tryStartAddingAttachment() } returns true
        every {
            draftDelegate.addAttachments(attachments = any())
        } answers {
            firstArg<List<ConversationDraftAttachment>>()
        }
        return draftDelegate
    }

    protected fun createDraftAttachmentMapperMock(): ConversationDraftAttachmentMapper {
        return mockk(relaxed = true)
    }

    protected fun createAttachmentRepositoryMock(
        createDraftAttachmentsFromPhotoPickerFlow:
        Flow<PhotoPickerDraftAttachmentResult> = flowOf(),
        createDraftAttachmentFromContactFlow: Flow<ConversationDraftAttachment?> = flowOf(null),
        deleteTemporaryAttachmentFlow: Flow<Unit> = flowOf(Unit),
    ): ConversationAttachmentsRepository {
        val attachmentRepository = mockk<ConversationAttachmentsRepository>()
        every {
            attachmentRepository.createDraftAttachmentsFromPhotoPicker(any())
        } returns createDraftAttachmentsFromPhotoPickerFlow
        every {
            attachmentRepository.createDraftAttachmentFromContact(any())
        } returns createDraftAttachmentFromContactFlow
        every {
            attachmentRepository.deleteTemporaryAttachment(any())
        } returns deleteTemporaryAttachmentFlow
        return attachmentRepository
    }
}
