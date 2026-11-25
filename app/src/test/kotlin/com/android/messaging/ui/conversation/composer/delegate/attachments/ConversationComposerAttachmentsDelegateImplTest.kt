package com.android.messaging.ui.conversation.composer.delegate.attachments

import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.repository.ConversationVCardMetadataRepository
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.composer.delegate.ConversationComposerAttachmentsDelegateImpl
import com.android.messaging.ui.conversation.composer.mapper.ConversationComposerAttachmentUiModelMapper
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationComposerAttachmentsDelegateImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun bind_seedsInitialStateFromCurrentDraftState() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val attachmentMapper = mockk<ConversationComposerAttachmentUiModelMapper>()
            val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
            val metadataRepository = mockk<ConversationVCardMetadataRepository>()
            val expectedState = persistentListOf<ComposerAttachmentUiModel>(
                ComposerAttachmentUiModel.Resolved.File(
                    key = "content://attachments/file/1",
                    contentType = "application/pdf",
                    contentUri = "content://attachments/file/1",
                ),
            )
            every {
                attachmentMapper.map(any(), any())
            } returns expectedState
            val delegate = ConversationComposerAttachmentsDelegateImpl(
                conversationComposerAttachmentUiModelMapper = attachmentMapper,
                conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
                conversationVCardMetadataRepository = metadataRepository,
                defaultDispatcher = dispatcher,
            )
            val boundScope = CoroutineScope(dispatcher + SupervisorJob())

            try {
                delegate.bind(
                    scope = boundScope,
                    draftStateFlow = MutableStateFlow(
                        ConversationDraftState(
                            draft = ConversationDraft(
                                attachments = persistentListOf(
                                    ConversationDraftAttachment(
                                        contentType = "application/pdf",
                                        contentUri = "content://attachments/file/1",
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
                assertEquals(expectedState, delegate.state.value)

                advanceUntilIdle()

                assertEquals(expectedState, delegate.state.value)
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun bind_updatesVCardAttachmentWhenMetadataArrives() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val attachmentMapper = mockk<ConversationComposerAttachmentUiModelMapper>()
            val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
            val metadataRepository = mockk<ConversationVCardMetadataRepository>()
            val loadingUiModel = ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleTextResId = 1,
                subtitleTextResId = 2,
            )
            val loadedMetadata = ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                entryCount = 1,
                singleDisplayName = "Sam Rivera",
                normalizedDestination = "555-000-8901",
                locationAddress = null,
            )
            val loadedUiModel = ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = "Sam Rivera",
                subtitleText = "555-000-8901",
            )
            val initialAttachment = ComposerAttachmentUiModel.Resolved.VCard(
                key = "content://attachments/vcard/1",
                contentType = "text/x-vCard",
                contentUri = "content://attachments/vcard/1",
                vCardUiModel = loadingUiModel,
            )
            every {
                attachmentMapper.map(any(), any())
            } returns persistentListOf(initialAttachment)
            every {
                metadataRepository.observeAttachmentMetadata(
                    contentUri = "content://attachments/vcard/1",
                    refreshes = any(),
                )
            } returns flowOf(loadedMetadata)
            every {
                vCardUiModelMapper.map(metadata = loadedMetadata)
            } returns loadedUiModel
            val delegate = ConversationComposerAttachmentsDelegateImpl(
                conversationComposerAttachmentUiModelMapper = attachmentMapper,
                conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
                conversationVCardMetadataRepository = metadataRepository,
                defaultDispatcher = dispatcher,
            )
            val boundScope = CoroutineScope(dispatcher + SupervisorJob())

            try {
                delegate.bind(
                    scope = boundScope,
                    draftStateFlow = MutableStateFlow(
                        ConversationDraftState(
                            draft = ConversationDraft(
                                attachments = persistentListOf(
                                    ConversationDraftAttachment(
                                        contentType = "text/x-vCard",
                                        contentUri = "content://attachments/vcard/1",
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
                advanceUntilIdle()

                assertEquals(
                    persistentListOf(
                        initialAttachment.copy(vCardUiModel = loadedUiModel),
                    ),
                    delegate.state.value,
                )
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun bind_doesNotRestartObservationWhenOnlyDraftTextChanges() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val attachmentMapper = mockk<ConversationComposerAttachmentUiModelMapper>()
            val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
            val metadataRepository = mockk<ConversationVCardMetadataRepository>()
            val draftStateFlow = MutableStateFlow(
                ConversationDraftState(
                    draft = ConversationDraft(
                        messageText = "hello",
                        attachments = persistentListOf(
                            ConversationDraftAttachment(
                                contentType = "application/pdf",
                                contentUri = "content://attachments/file/1",
                            ),
                        ),
                    ),
                ),
            )
            every {
                attachmentMapper.map(any(), any())
            } returns persistentListOf(
                ComposerAttachmentUiModel.Resolved.File(
                    key = "content://attachments/file/1",
                    contentType = "application/pdf",
                    contentUri = "content://attachments/file/1",
                ),
            )
            val delegate = ConversationComposerAttachmentsDelegateImpl(
                conversationComposerAttachmentUiModelMapper = attachmentMapper,
                conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
                conversationVCardMetadataRepository = metadataRepository,
                defaultDispatcher = dispatcher,
            )
            val boundScope = CoroutineScope(dispatcher + SupervisorJob())

            try {
                delegate.bind(
                    scope = boundScope,
                    draftStateFlow = draftStateFlow,
                )
                advanceUntilIdle()

                verify(exactly = 2) {
                    attachmentMapper.map(any(), any())
                }

                draftStateFlow.value = draftStateFlow.value.copy(
                    draft = draftStateFlow.value.draft.copy(
                        messageText = "updated text",
                    ),
                )
                advanceUntilIdle()

                verify(exactly = 2) {
                    attachmentMapper.map(any(), any())
                }
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun bind_observesDuplicateVCardUrisOnlyOnce() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val attachmentMapper = mockk<ConversationComposerAttachmentUiModelMapper>()
            val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
            val metadataRepository = mockk<ConversationVCardMetadataRepository>()
            every {
                attachmentMapper.map(any(), any())
            } returns persistentListOf(
                createVCardAttachment(),
                createVCardAttachment(),
            )
            every {
                metadataRepository.observeAttachmentMetadata(
                    contentUri = "content://attachments/vcard/1",
                    refreshes = any(),
                )
            } returns flowOf(ConversationVCardAttachmentMetadata.Loading)
            every {
                vCardUiModelMapper.map(metadata = ConversationVCardAttachmentMetadata.Loading)
            } returns ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleTextResId = 1,
                subtitleTextResId = 2,
            )
            val delegate = ConversationComposerAttachmentsDelegateImpl(
                conversationComposerAttachmentUiModelMapper = attachmentMapper,
                conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
                conversationVCardMetadataRepository = metadataRepository,
                defaultDispatcher = dispatcher,
            )
            val boundScope = CoroutineScope(dispatcher + SupervisorJob())

            try {
                delegate.bind(
                    scope = boundScope,
                    draftStateFlow = MutableStateFlow(
                        ConversationDraftState(
                            draft = ConversationDraft(
                                attachments = persistentListOf(
                                    ConversationDraftAttachment(
                                        contentType = "text/x-vCard",
                                        contentUri = "content://attachments/vcard/1",
                                    ),
                                    ConversationDraftAttachment(
                                        contentType = "text/x-vCard",
                                        contentUri = "content://attachments/vcard/1",
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    metadataRepository.observeAttachmentMetadata(
                        contentUri = "content://attachments/vcard/1",
                        refreshes = any(),
                    )
                }
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun bind_keepsLoadedVCardWhenAnotherAttachmentIsRemoved() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val attachmentMapper = mockk<ConversationComposerAttachmentUiModelMapper>()
            val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
            val metadataRepository = mockk<ConversationVCardMetadataRepository>()
            val firstDraftAttachment = ConversationDraftAttachment(
                contentType = "text/x-vCard",
                contentUri = "content://attachments/vcard/1",
            )
            val secondDraftAttachment = ConversationDraftAttachment(
                contentType = "text/x-vCard",
                contentUri = "content://attachments/vcard/2",
            )
            every {
                attachmentMapper.map(any(), any())
            } answers {
                firstArg<List<ConversationDraftAttachment>>()
                    .map { attachment ->
                        createVCardAttachment(contentUri = attachment.contentUri)
                    }
                    .toImmutableList()
            }
            every {
                metadataRepository.observeAttachmentMetadata(
                    contentUri = any(),
                    refreshes = any(),
                )
            } answers {
                val contentUri = firstArg<String?>()
                flow {
                    emit(ConversationVCardAttachmentMetadata.Loading)
                    delay(timeMillis = 100)
                    emit(createLoadedMetadata(displayName = contentUri))
                }
            }
            every {
                vCardUiModelMapper.map(metadata = any())
            } answers {
                when (val metadata = firstArg<ConversationVCardAttachmentMetadata?>()) {
                    is ConversationVCardAttachmentMetadata.Loaded -> {
                        createLoadedUiModel(displayName = metadata.singleDisplayName)
                    }

                    else -> createVCardAttachment().vCardUiModel
                }
            }
            val draftStateFlow = MutableStateFlow(
                ConversationDraftState(
                    draft = ConversationDraft(
                        attachments = persistentListOf(
                            firstDraftAttachment,
                            secondDraftAttachment,
                        ),
                    ),
                ),
            )
            val delegate = ConversationComposerAttachmentsDelegateImpl(
                conversationComposerAttachmentUiModelMapper = attachmentMapper,
                conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
                conversationVCardMetadataRepository = metadataRepository,
                defaultDispatcher = dispatcher,
            )
            val boundScope = CoroutineScope(dispatcher + SupervisorJob())
            val loadedSecondAttachment = createVCardAttachment(
                contentUri = "content://attachments/vcard/2",
            ).copy(
                vCardUiModel = createLoadedUiModel(
                    displayName = "content://attachments/vcard/2",
                ),
            )

            try {
                delegate.bind(
                    scope = boundScope,
                    draftStateFlow = draftStateFlow,
                )
                advanceUntilIdle()

                draftStateFlow.value = draftStateFlow.value.copy(
                    draft = draftStateFlow.value.draft.copy(
                        attachments = persistentListOf(secondDraftAttachment),
                    ),
                )
                runCurrent()

                assertEquals(persistentListOf(loadedSecondAttachment), delegate.state.value)

                advanceUntilIdle()

                assertEquals(persistentListOf(loadedSecondAttachment), delegate.state.value)
            } finally {
                boundScope.cancel()
            }
        }
    }

    private fun createLoadedMetadata(
        displayName: String?,
    ): ConversationVCardAttachmentMetadata.Loaded {
        return ConversationVCardAttachmentMetadata.Loaded(
            type = ConversationVCardAttachmentType.CONTACT,
            avatarPhoto = null,
            entryCount = 1,
            singleDisplayName = displayName,
            normalizedDestination = null,
            locationAddress = null,
        )
    }

    private fun createLoadedUiModel(
        displayName: String?,
    ): ConversationVCardAttachmentUiModel {
        return ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleText = displayName,
        )
    }

    private fun createVCardAttachment(
        contentUri: String = "content://attachments/vcard/1",
    ): ComposerAttachmentUiModel.Resolved.VCard {
        return ComposerAttachmentUiModel.Resolved.VCard(
            key = contentUri,
            contentType = "text/x-vCard",
            contentUri = contentUri,
            vCardUiModel = ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleTextResId = 1,
                subtitleTextResId = 2,
            ),
        )
    }
}
