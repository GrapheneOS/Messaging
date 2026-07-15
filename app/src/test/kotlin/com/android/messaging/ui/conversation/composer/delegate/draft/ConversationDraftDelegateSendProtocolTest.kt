package com.android.messaging.ui.conversation.composer.delegate.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import io.mockk.coVerify
import io.mockk.verify
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationDraftDelegateSendProtocolTest : BaseConversationDraftDelegateTest() {

    @Test
    fun onMessageTextChanged_resolvesDraftSendProtocolAfterDebounce() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val conversationsRepository = createConversationsRepositoryMock()
            val getDraftSendProtocol = createGetDraftSendProtocolMock(
                protocol = ConversationDraftSendProtocol.MMS,
            )
            val harness = createBoundLoadedDelegateHarness(
                conversationsRepository = conversationsRepository,
                getDraftSendProtocol = getDraftSendProtocol,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                advanceTimeBy(249.milliseconds)

                coVerify(exactly = 0) {
                    conversationsRepository.getConversationSendData(
                        conversationId = any(),
                        requestedSelfParticipantId = any(),
                    )
                }
                assertEquals(
                    ConversationDraftSendProtocol.SMS,
                    harness.delegate.state.value.sendProtocol,
                )

                advanceTimeBy(1.milliseconds)
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    conversationsRepository.getConversationSendData(
                        conversationId = CONVERSATION_ID,
                        requestedSelfParticipantId = "",
                    )
                }
                verify(exactly = 1) {
                    getDraftSendProtocol.invoke(
                        draft = match { draft -> draft.messageText == "Hello" },
                        sendData = any(),
                    )
                }
                assertEquals(
                    ConversationDraftSendProtocol.MMS,
                    harness.delegate.state.value.sendProtocol,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageTextChanged_debouncesDraftSendProtocolUntilTypingSettles() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val conversationsRepository = createConversationsRepositoryMock()
            val getDraftSendProtocol = createGetDraftSendProtocolMock(
                protocol = ConversationDraftSendProtocol.MMS,
            )
            val harness = createBoundLoadedDelegateHarness(
                conversationsRepository = conversationsRepository,
                getDraftSendProtocol = getDraftSendProtocol,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "H")
                advanceTimeBy(100.milliseconds)
                harness.delegate.onMessageTextChanged(messageText = "He")
                advanceTimeBy(100.milliseconds)
                harness.delegate.onMessageTextChanged(messageText = "Hel")
                advanceTimeBy(249.milliseconds)

                coVerify(exactly = 0) {
                    conversationsRepository.getConversationSendData(
                        conversationId = any(),
                        requestedSelfParticipantId = any(),
                    )
                }
                verify(exactly = 0) {
                    getDraftSendProtocol.invoke(draft = any(), sendData = any())
                }

                advanceTimeBy(1.milliseconds)
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    conversationsRepository.getConversationSendData(
                        conversationId = any(),
                        requestedSelfParticipantId = any(),
                    )
                }
                verify(exactly = 1) {
                    getDraftSendProtocol.invoke(
                        draft = match { draft -> draft.messageText == "Hel" },
                        sendData = any(),
                    )
                }
                assertEquals(
                    ConversationDraftSendProtocol.MMS,
                    harness.delegate.state.value.sendProtocol,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageTextChanged_whenDraftBecomesEmpty_resetsDraftSendProtocolToSms() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val conversationsRepository = createConversationsRepositoryMock()
            val getDraftSendProtocol = createGetDraftSendProtocolMock(
                protocol = ConversationDraftSendProtocol.MMS,
            )
            val harness = createBoundLoadedDelegateHarness(
                conversationsRepository = conversationsRepository,
                getDraftSendProtocol = getDraftSendProtocol,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                advanceTimeBy(250.milliseconds)
                advanceUntilIdle()

                assertEquals(
                    ConversationDraftSendProtocol.MMS,
                    harness.delegate.state.value.sendProtocol,
                )

                harness.delegate.onMessageTextChanged(messageText = "")

                assertEquals(
                    ConversationDraftSendProtocol.SMS,
                    harness.delegate.state.value.sendProtocol,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun addAttachments_whenSendDataIsUnavailable_fallsBackToMmsDraftProtocol() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val conversationsRepository = createConversationsRepositoryMock(
                sendData = null,
            )
            val getDraftSendProtocol = createGetDraftSendProtocolMock(
                protocol = ConversationDraftSendProtocol.SMS,
            )
            val harness = createBoundLoadedDelegateHarness(
                conversationsRepository = conversationsRepository,
                getDraftSendProtocol = getDraftSendProtocol,
            )

            try {
                harness.delegate.addAttachments(
                    attachments = listOf(
                        ConversationDraftAttachment(
                            contentType = "image/jpeg",
                            contentUri = "content://images/1",
                        ),
                    ),
                )
                advanceTimeBy(250.milliseconds)
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    conversationsRepository.getConversationSendData(
                        conversationId = any(),
                        requestedSelfParticipantId = any(),
                    )
                }
                verify(exactly = 0) {
                    getDraftSendProtocol.invoke(draft = any(), sendData = any())
                }
                assertEquals(
                    ConversationDraftSendProtocol.MMS,
                    harness.delegate.state.value.sendProtocol,
                )
            } finally {
                harness.cancel()
            }
        }
    }
}
