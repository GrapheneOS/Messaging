package com.android.messaging.ui.conversation.composer.delegate.conversationdrafteditordelegate

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachmentKind
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.domain.conversation.usecase.draft.ResolveConversationDraftSendProtocol
import com.android.messaging.domain.conversation.usecase.draft.ResolveDraftAttachmentsWithinLimit
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.domain.conversation.usecase.draft.model.DraftAttachmentLimitResult
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftEditorDelegateImpl
import com.android.messaging.ui.conversation.composer.delegate.PersistedDraftUpdate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseConversationDraftEditorDelegateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected val subscriptionsRepository = mockk<SubscriptionsRepository>()
    protected val resolveConversationDraftSendProtocol =
        mockk<ResolveConversationDraftSendProtocol>()
    protected val resolveDraftAttachmentsWithinLimit = mockk<ResolveDraftAttachmentsWithinLimit>()

    @Before
    fun setUpDefaultCollaboratorStubs() {
        every { subscriptionsRepository.resolveAttachmentLimit() } returns DEFAULT_ATTACHMENT_LIMIT
        coEvery {
            resolveConversationDraftSendProtocol(conversationId = any(), draft = any())
        } returns ConversationDraftSendProtocol.SMS
    }

    protected fun createDelegate(): ConversationDraftEditorDelegateImpl {
        return ConversationDraftEditorDelegateImpl(
            subscriptionsRepository = subscriptionsRepository,
            resolveConversationDraftSendProtocol = resolveConversationDraftSendProtocol,
            resolveDraftAttachmentsWithinLimit = resolveDraftAttachmentsWithinLimit,
        )
    }

    protected fun loadedDelegate(
        conversationId: String = CONVERSATION_ID,
        persistedDraft: ConversationDraft = draft(),
    ): ConversationDraftEditorDelegateImpl {
        return createDelegate().also { delegate ->
            delegate.reset(conversationId = conversationId)
            delegate.applyPersistedDraftUpdate(
                persistedDraftUpdate = PersistedDraftUpdate(
                    conversationId = conversationId,
                    persistedDraft = persistedDraft,
                ),
            )
        }
    }

    protected fun persistedDraftUpdate(
        conversationId: String = CONVERSATION_ID,
        persistedDraft: ConversationDraft = draft(),
    ): PersistedDraftUpdate {
        return PersistedDraftUpdate(
            conversationId = conversationId,
            persistedDraft = persistedDraft,
        )
    }

    @Suppress("SameParameterValue")
    protected fun givenAttachmentLimit(limit: Int) {
        every { subscriptionsRepository.resolveAttachmentLimit() } returns limit
    }

    protected fun givenResolvedSendProtocol(protocol: ConversationDraftSendProtocol) {
        coEvery {
            resolveConversationDraftSendProtocol(conversationId = any(), draft = any())
        } returns protocol
    }

    protected fun givenAttachmentLimitResult(
        attachmentsToAdd: List<ConversationDraftAttachment>,
        didDropAttachments: Boolean,
    ) {
        every {
            resolveDraftAttachmentsWithinLimit(
                currentAttachments = any(),
                attachmentsToAdd = any(),
            )
        } returns DraftAttachmentLimitResult(
            attachmentsToAdd = attachmentsToAdd,
            didDropAttachments = didDropAttachments,
        )
    }

    protected fun draft(
        messageText: String = "",
        subjectText: String = "",
        selfParticipantId: String = "",
        attachments: List<ConversationDraftAttachment> = emptyList(),
    ): ConversationDraft {
        return ConversationDraft(
            messageText = messageText,
            subjectText = subjectText,
            selfParticipantId = selfParticipantId,
            attachments = attachments.toImmutableList(),
        )
    }

    protected fun attachment(
        contentUri: String,
        contentType: String = "image/jpeg",
        captionText: String = "",
    ): ConversationDraftAttachment {
        return ConversationDraftAttachment(
            contentType = contentType,
            contentUri = contentUri,
            captionText = captionText,
        )
    }

    protected fun pendingAttachment(
        pendingAttachmentId: String,
        contentUri: String = "content://pending/$pendingAttachmentId",
        contentType: String = "image/jpeg",
        kind: ConversationDraftPendingAttachmentKind =
            ConversationDraftPendingAttachmentKind.Generic,
    ): ConversationDraftPendingAttachment {
        return ConversationDraftPendingAttachment(
            pendingAttachmentId = pendingAttachmentId,
            contentUri = contentUri,
            contentType = contentType,
            kind = kind,
        )
    }

    protected companion object {
        const val DEFAULT_ATTACHMENT_LIMIT = 10
    }
}
