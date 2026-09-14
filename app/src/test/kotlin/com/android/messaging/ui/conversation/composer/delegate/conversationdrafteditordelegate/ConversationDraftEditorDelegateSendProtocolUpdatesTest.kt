package com.android.messaging.ui.conversation.composer.delegate.conversationdrafteditordelegate

import app.cash.turbine.test
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import io.mockk.coEvery
import io.mockk.coVerify
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationDraftEditorDelegateSendProtocolUpdatesTest :
    BaseConversationDraftEditorDelegateTest() {

    @Test
    fun applySendProtocol_whenDraftHasContent_appliesGivenProtocol() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")

        delegate.applySendProtocol(sendProtocol = ConversationDraftSendProtocol.MMS)

        assertEquals(ConversationDraftSendProtocol.MMS, delegate.state.value.sendProtocol)
    }

    @Test
    fun applySendProtocol_whenDraftIsEmpty_forcesSms() {
        val delegate = loadedDelegate()

        delegate.applySendProtocol(sendProtocol = ConversationDraftSendProtocol.MMS)

        assertEquals(ConversationDraftSendProtocol.SMS, delegate.state.value.sendProtocol)
    }

    @Test
    fun sendProtocolUpdates_resolvesProtocolWithConversationAndDraftAfterDebounce() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenResolvedSendProtocol(protocol = ConversationDraftSendProtocol.MMS)
            val delegate = loadedDelegate()

            delegate.sendProtocolUpdates.test {
                delegate.onMessageTextChanged(messageText = "hi")
                advanceTimeBy(249.milliseconds)
                expectNoEvents()
                coVerify(exactly = 0) {
                    resolveConversationDraftSendProtocol(conversationId = any(), draft = any())
                }

                advanceUntilIdle()

                assertEquals(ConversationDraftSendProtocol.MMS, awaitItem())
                coVerify(exactly = 1) {
                    resolveConversationDraftSendProtocol(
                        conversationId = CONVERSATION_ID,
                        draft = draft(messageText = "hi"),
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun sendProtocolUpdates_ignoresChangesThatDoNotAffectConversationOrDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenResolvedSendProtocol(protocol = ConversationDraftSendProtocol.MMS)
            val delegate = loadedDelegate()

            delegate.sendProtocolUpdates.test {
                delegate.onMessageTextChanged(messageText = "hi")
                advanceUntilIdle()
                assertEquals(ConversationDraftSendProtocol.MMS, awaitItem())

                delegate.addPendingAttachment(
                    pendingAttachment = pendingAttachment(pendingAttachmentId = "p1"),
                )
                advanceUntilIdle()

                expectNoEvents()
                coVerify(exactly = 1) {
                    resolveConversationDraftSendProtocol(conversationId = any(), draft = any())
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun sendProtocolUpdates_reResolvesAndReEmitsWhenPendingAttachmentIsResolvedIntoDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenResolvedSendProtocol(protocol = ConversationDraftSendProtocol.SMS)
            coEvery {
                resolveConversationDraftSendProtocol(
                    conversationId = any(),
                    draft = match { draft -> draft.attachments.isNotEmpty() },
                )
            } returns ConversationDraftSendProtocol.MMS
            val delegate = loadedDelegate()

            delegate.sendProtocolUpdates.test {
                delegate.onMessageTextChanged(messageText = "hi")
                advanceUntilIdle()
                assertEquals(ConversationDraftSendProtocol.SMS, awaitItem())

                delegate.addPendingAttachment(
                    pendingAttachment = pendingAttachment(pendingAttachmentId = "p1"),
                )
                val resolved = attachment(contentUri = "content://resolved")
                givenAttachmentLimitResult(
                    attachmentsToAdd = listOf(resolved),
                    didDropAttachments = false,
                )
                delegate.resolvePendingAttachment(pendingAttachmentId = "p1", attachment = resolved)
                advanceUntilIdle()

                assertEquals(ConversationDraftSendProtocol.MMS, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun sendProtocolUpdates_doesNotReEmitWhenResolvedProtocolIsUnchanged() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenResolvedSendProtocol(protocol = ConversationDraftSendProtocol.SMS)
            val delegate = loadedDelegate()

            delegate.sendProtocolUpdates.test {
                delegate.onMessageTextChanged(messageText = "hi")
                advanceUntilIdle()
                assertEquals(ConversationDraftSendProtocol.SMS, awaitItem())

                delegate.onMessageTextChanged(messageText = "hello")
                advanceUntilIdle()

                expectNoEvents()
                coVerify(exactly = 2) {
                    resolveConversationDraftSendProtocol(conversationId = any(), draft = any())
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun sendProtocolUpdates_cancelsInFlightResolutionWhenDraftChangesAgain() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = loadedDelegate()
            coEvery {
                resolveConversationDraftSendProtocol(
                    conversationId = any(),
                    draft = draft(messageText = "slow"),
                )
            } coAnswers {
                delay(timeMillis = 1_000)
                ConversationDraftSendProtocol.MMS
            }
            coEvery {
                resolveConversationDraftSendProtocol(
                    conversationId = any(),
                    draft = draft(messageText = "fast"),
                )
            } returns ConversationDraftSendProtocol.SMS

            delegate.sendProtocolUpdates.test {
                delegate.onMessageTextChanged(messageText = "slow")
                advanceTimeBy(300.milliseconds)
                delegate.onMessageTextChanged(messageText = "fast")
                advanceUntilIdle()

                assertEquals(ConversationDraftSendProtocol.SMS, awaitItem())
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
