package com.android.messaging.ui.conversation.audio.delegate

import android.net.Uri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachmentKind
import com.android.messaging.data.media.repository.ConversationAttachmentsRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.mediapicker.LevelTrackingMediaRecorder
import com.android.messaging.util.ContentType
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationAudioRecordingDelegateImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var conversationAttachmentsRepository: ConversationAttachmentsRepository
    private lateinit var subscriptionsRepository: SubscriptionsRepository
    private lateinit var conversationDraftDelegate: ConversationDraftDelegate

    @Before
    fun setUp() {
        conversationAttachmentsRepository = mockk()
        subscriptionsRepository = mockk()
        conversationDraftDelegate = mockk()

        every {
            subscriptionsRepository.resolveMaxMessageSize(any())
        } returns flowOf(500_000)
        every {
            conversationAttachmentsRepository.deleteTemporaryAttachment(any())
        } returns flowOf(Unit)
        every {
            conversationDraftDelegate.addAttachments(any())
        } answers {
            firstArg<Collection<ConversationDraftAttachment>>().toList()
        }
        every {
            conversationDraftDelegate.addPendingAttachment(any())
        } just runs
        every {
            conversationDraftDelegate.removePendingAttachment(any())
        } just runs
        every {
            conversationDraftDelegate.resolvePendingAttachment(any(), any())
        } returns true
    }

    @After
    fun tearDown() {
        unmockkConstructor(LevelTrackingMediaRecorder::class)
    }

    @Test
    fun startRecording_startsRecorderAndPublishesRecordingState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            mockSuccessfulRecorderStart()

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()

            assertEquals(
                ConversationAudioRecordingPhase.Recording,
                delegate.state.value.phase,
            )
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                subscriptionsRepository.resolveMaxMessageSize(
                    selfParticipantId = "self-1",
                )
            }
            verify(exactly = 1) {
                anyConstructed<LevelTrackingMediaRecorder>().startRecording(
                    any(),
                    any(),
                    500_000,
                )
            }
        }
    }

    @Test
    fun startLockedRecording_startsRecorderAndPublishesLockedRecordingState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            mockSuccessfulRecorderStart()

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startLockedRecording(selfParticipantId = "self-1")
            runCurrent()

            assertEquals(
                ConversationAudioRecordingPhase.Recording,
                delegate.state.value.phase,
            )
            assertTrue(delegate.state.value.isLocked)
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                subscriptionsRepository.resolveMaxMessageSize(
                    selfParticipantId = "self-1",
                )
            }
            verify(exactly = 1) {
                anyConstructed<LevelTrackingMediaRecorder>().startRecording(
                    any(),
                    any(),
                    500_000,
                )
            }
        }
    }

    @Test
    fun lockRecording_whileStarting_locksWhenRecorderStarts() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val maxMessageSize = CompletableDeferred<Int>()
            every {
                subscriptionsRepository.resolveMaxMessageSize(any())
            } returns flow {
                emit(maxMessageSize.await())
            }
            mockSuccessfulRecorderStart()

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()

            assertTrue(delegate.lockRecording())
            maxMessageSize.complete(500_000)
            runCurrent()

            assertEquals(
                ConversationAudioRecordingPhase.Recording,
                delegate.state.value.phase,
            )
            assertTrue(delegate.state.value.isLocked)
        }
    }

    @Test
    fun cancelRecording_whileStarting_stopsAndDeletesWhenRecorderStarts() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val outputUri = Uri.parse("content://scratch/audio/starting")
            val maxMessageSize = CompletableDeferred<Int>()
            every {
                subscriptionsRepository.resolveMaxMessageSize(any())
            } returns flow {
                emit(maxMessageSize.await())
            }
            mockSuccessfulRecorderStart(outputUri = outputUri)

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()
            delegate.cancelRecording()
            maxMessageSize.complete(500_000)
            runCurrent()

            assertEquals(
                ConversationAudioRecordingPhase.Idle,
                delegate.state.value.phase,
            )
            verify(exactly = 1) {
                anyConstructed<LevelTrackingMediaRecorder>().stopRecording()
            }
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                conversationAttachmentsRepository.deleteTemporaryAttachment(
                    contentUri = outputUri.toString(),
                )
            }
        }
    }

    @Test
    fun finishRecording_whileStarting_stopsAndDeletesWhenRecorderStarts() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val outputUri = Uri.parse("content://scratch/audio/starting-finish")
            val maxMessageSize = CompletableDeferred<Int>()
            every {
                subscriptionsRepository.resolveMaxMessageSize(any())
            } returns flow {
                emit(maxMessageSize.await())
            }
            mockSuccessfulRecorderStart(outputUri = outputUri)

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()

            assertEquals(
                ConversationAudioRecordingPhase.Recording,
                delegate.state.value.phase,
            )

            delegate.finishRecording()
            maxMessageSize.complete(500_000)
            runCurrent()

            assertEquals(
                ConversationAudioRecordingPhase.Idle,
                delegate.state.value.phase,
            )
            verify(exactly = 1) {
                anyConstructed<LevelTrackingMediaRecorder>().stopRecording()
            }
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                conversationAttachmentsRepository.deleteTemporaryAttachment(
                    contentUri = outputUri.toString(),
                )
            }
            verify(exactly = 0) {
                conversationDraftDelegate.addPendingAttachment(any())
            }
        }
    }

    @Test
    fun finishRecording_afterMinimumDuration_resolvesPendingAudioAttachment() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val outputUri = Uri.parse("content://scratch/audio/1")
            val pendingAttachment = slot<ConversationDraftPendingAttachment>()
            mockSuccessfulRecorderStart(outputUri = outputUri)
            every {
                conversationDraftDelegate.addPendingAttachment(capture(pendingAttachment))
            } just runs

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()
            ShadowSystemClock.advanceBy(Duration.ofMillis(350))

            delegate.finishRecording()
            runCurrent()

            assertEquals(
                ConversationAudioRecordingPhase.Finalizing,
                delegate.state.value.phase,
            )
            verifyPendingAudioAttachmentAdded(pendingAttachment = pendingAttachment)

            advanceTimeBy(delayTimeMillis = 500L)
            runCurrent()

            verify(exactly = 1) {
                conversationDraftDelegate.resolvePendingAttachment(
                    pendingAttachmentId = pendingAttachment.captured.pendingAttachmentId,
                    attachment = ConversationDraftAttachment(
                        contentType = ContentType.AUDIO_3GPP,
                        contentUri = outputUri.toString(),
                    ),
                )
            }
            verify(exactly = 0) {
                conversationDraftDelegate.addAttachments(any())
            }
            assertEquals(
                ConversationAudioRecordingPhase.Idle,
                delegate.state.value.phase,
            )
        }
    }

    @Test
    fun cancelRecording_whileRecording_deletesTemporaryAttachmentAndResetsState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val outputUri = Uri.parse("content://scratch/audio/2")
            mockSuccessfulRecorderStart(outputUri = outputUri)

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()
            delegate.cancelRecording()
            runCurrent()

            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                conversationAttachmentsRepository.deleteTemporaryAttachment(
                    contentUri = outputUri.toString(),
                )
            }
            verify(exactly = 0) {
                conversationDraftDelegate.addAttachments(any())
            }
            assertEquals(
                ConversationAudioRecordingPhase.Idle,
                delegate.state.value.phase,
            )
        }
    }

    @Test
    fun cancelRecording_whileFinalizing_removesPendingAttachmentAndDeletesTemporaryAttachment() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val outputUri = Uri.parse("content://scratch/audio/finalizing")
            val pendingAttachment = slot<ConversationDraftPendingAttachment>()
            mockSuccessfulRecorderStart(outputUri = outputUri)
            every {
                conversationDraftDelegate.addPendingAttachment(capture(pendingAttachment))
            } just runs

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()
            ShadowSystemClock.advanceBy(Duration.ofMillis(350))
            delegate.finishRecording()
            runCurrent()

            delegate.cancelRecording()
            runCurrent()

            verify(exactly = 1) {
                conversationDraftDelegate.removePendingAttachment(
                    pendingAttachmentId = pendingAttachment.captured.pendingAttachmentId,
                )
            }
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                conversationAttachmentsRepository.deleteTemporaryAttachment(
                    contentUri = outputUri.toString(),
                )
            }
            verify(exactly = 0) {
                conversationDraftDelegate.resolvePendingAttachment(any(), any())
            }
            assertEquals(
                ConversationAudioRecordingPhase.Idle,
                delegate.state.value.phase,
            )
        }
    }

    @Test
    fun finishRecording_calledTwice_resolvesPendingAttachmentOnce() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val outputUri = Uri.parse("content://scratch/audio/double-finish")
            mockSuccessfulRecorderStart(outputUri = outputUri)

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()
            ShadowSystemClock.advanceBy(Duration.ofMillis(350))

            delegate.finishRecording()
            delegate.finishRecording()
            advanceTimeBy(delayTimeMillis = 500L)
            runCurrent()

            verify(exactly = 1) {
                conversationDraftDelegate.addPendingAttachment(any())
            }
            verify(exactly = 1) {
                conversationDraftDelegate.resolvePendingAttachment(any(), any())
            }
            verify(exactly = 1) {
                anyConstructed<LevelTrackingMediaRecorder>().stopRecording()
            }
        }
    }

    @Test
    fun lockRecording_thenDurationTick_preservesLockedState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            mockSuccessfulRecorderStart()

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()

            assertTrue(delegate.lockRecording())
            ShadowSystemClock.advanceBy(Duration.ofMillis(250))
            advanceTimeBy(delayTimeMillis = 200L)
            runCurrent()

            assertTrue(delegate.state.value.isLocked)
            assertTrue(delegate.state.value.durationMillis >= 250L)
        }
    }

    @Test
    fun finishRecording_beforeMinimumDuration_deletesTemporaryAttachmentWithoutAttaching() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val outputUri = Uri.parse("content://scratch/audio/short")
            mockSuccessfulRecorderStart(outputUri = outputUri)

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()
            ShadowSystemClock.advanceBy(Duration.ofMillis(250))
            delegate.finishRecording()
            runCurrent()

            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                conversationAttachmentsRepository.deleteTemporaryAttachment(
                    contentUri = outputUri.toString(),
                )
            }
            verify(exactly = 0) {
                conversationDraftDelegate.addPendingAttachment(any())
            }
            verify(exactly = 0) {
                conversationDraftDelegate.resolvePendingAttachment(any(), any())
            }
            assertEquals(
                ConversationAudioRecordingPhase.Idle,
                delegate.state.value.phase,
            )
        }
    }

    private fun mockSuccessfulRecorderStart(
        outputUri: Uri = Uri.parse("content://scratch/audio/default"),
    ) {
        mockkConstructor(LevelTrackingMediaRecorder::class)
        every {
            anyConstructed<LevelTrackingMediaRecorder>().startRecording(any(), any(), 500_000)
        } returns true
        every {
            anyConstructed<LevelTrackingMediaRecorder>().stopRecording()
        } returns outputUri
    }

    private fun verifyPendingAudioAttachmentAdded(
        pendingAttachment: CapturingSlot<ConversationDraftPendingAttachment>,
    ) {
        assertTrue(pendingAttachment.isCaptured)
        assertTrue(
            pendingAttachment.captured.pendingAttachmentId.startsWith(
                prefix = "pending-audio-",
            ),
        )
        assertEquals(
            "pending://audio/${pendingAttachment.captured.pendingAttachmentId}",
            pendingAttachment.captured.contentUri,
        )
        assertEquals(ContentType.AUDIO_3GPP, pendingAttachment.captured.contentType)
        assertEquals(
            ConversationDraftPendingAttachmentKind.AudioFinalizing,
            pendingAttachment.captured.kind,
        )
    }

    private fun createBoundDelegate(scope: CoroutineScope): ConversationAudioRecordingDelegateImpl {
        return ConversationAudioRecordingDelegateImpl(
            conversationAttachmentsRepository = conversationAttachmentsRepository,
            subscriptionsRepository = subscriptionsRepository,
            conversationDraftDelegate = conversationDraftDelegate,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        ).also { delegate ->
            delegate.bind(
                scope = scope,
                conversationIdFlow = MutableStateFlow(value = CONVERSATION_ID),
            )
        }
    }
}
