package com.android.messaging.ui.conversation.audio.delegate

import android.net.Uri
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.media.repository.ConversationAttachmentsRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.testutil.PausableTestDispatcher
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.typeMessageText
import com.android.messaging.ui.conversation.composer.delegate.draft.BaseConversationDraftDelegateTest
import com.android.messaging.ui.mediapicker.LevelTrackingMediaRecorder
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationAudioRecordingDraftLifecycleTest : BaseConversationDraftDelegateTest() {

    private val outputUri = Uri.parse("content://scratch/audio/kept")
    private val conversationAttachmentsRepository = mockk<ConversationAttachmentsRepository> {
        every { deleteTemporaryAttachment(any()) } returns flowOf(Unit)
    }

    @After
    fun tearDown() {
        unmockkConstructor(LevelTrackingMediaRecorder::class)
    }

    @Test
    fun onScreenCleared_whenOnClearedFlushRunsLast_keepsRecordingInSavedDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val defaultDispatcher = PausableTestDispatcher(
                testDispatcher = mainDispatcherRule.testDispatcher,
            )
            val harness = createBoundLoadedDelegateHarness(defaultDispatcher = defaultDispatcher)
            val audioRecordingDelegate = createBoundAudioRecordingDelegate(
                harness = harness,
                defaultDispatcher = defaultDispatcher,
            )

            try {
                harness.delegate.typeMessageText(messageText = "Keep this text")
                audioRecordingDelegate.startLockedRecording(
                    selfParticipantId = ParticipantId("self"),
                )
                runCurrent()
                ShadowSystemClock.advanceBy(Duration.ofMillis(350))

                // ViewModel.clear() closes the screen scope before onCleared() runs, so the
                // typed text was never autosaved
                harness.delegateScope.cancel()
                runCurrent()
                defaultDispatcher.pause()
                audioRecordingDelegate.onScreenCleared()
                harness.delegate.flushDraft()
                assertEquals(2, defaultDispatcher.heldTaskCount)

                // Finalizing queues a flush of the draft with the recording behind the older
                // flush from onCleared(), which default dispatcher threads may still run last
                defaultDispatcher.release(index = 0)
                runCurrent()
                assertEquals(2, defaultDispatcher.heldTaskCount)
                defaultDispatcher.release(index = 1)
                runCurrent()
                defaultDispatcher.release(index = 0)
                runCurrent()

                val savedDrafts = mutableListOf<ConversationDraft>()
                coVerify {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = capture(savedDrafts),
                    )
                }
                assertEquals("Keep this text", savedDrafts.last().messageText)
                assertEquals(
                    listOf(outputUri.toString()),
                    savedDrafts.last().attachments.map { it.contentUri },
                )
                verify(exactly = 0) {
                    @Suppress("UnusedFlow")
                    conversationAttachmentsRepository.deleteTemporaryAttachment(any())
                }
            } finally {
                defaultDispatcher.resume()
                harness.cancel()
            }
        }
    }

    private fun createBoundAudioRecordingDelegate(
        harness: DelegateHarness,
        defaultDispatcher: PausableTestDispatcher,
    ): ConversationAudioRecordingDelegateImpl {
        mockkConstructor(LevelTrackingMediaRecorder::class)
        every {
            anyConstructed<LevelTrackingMediaRecorder>().startRecording(any(), any(), any())
        } returns true
        every {
            anyConstructed<LevelTrackingMediaRecorder>().stopRecording()
        } returns outputUri

        val subscriptionsRepository = mockk<SubscriptionsRepository> {
            every { resolveMaxMessageSize(any()) } returns flowOf(500_000)
        }

        return ConversationAudioRecordingDelegateImpl(
            applicationScope = harness.applicationScope,
            conversationAttachmentsRepository = conversationAttachmentsRepository,
            subscriptionsRepository = subscriptionsRepository,
            conversationDraftDelegate = harness.delegate,
            defaultDispatcher = defaultDispatcher,
        ).also { delegate ->
            delegate.bind(
                scope = harness.delegateScope,
                conversationIdFlow = harness.conversationIdFlow,
            )
        }
    }
}
