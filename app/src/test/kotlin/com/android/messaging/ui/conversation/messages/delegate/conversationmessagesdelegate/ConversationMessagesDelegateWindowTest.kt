package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.model.message.ConversationMessagesWindow
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessagesDelegateWindowTest : BaseConversationMessagesDelegateTest() {

    @Test
    fun bind_startsWithTheDefaultWindow() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windowSizes = givenWindowedConversationMessages(loadedMessageCount = 10)
            createBoundDelegate(conversationIdFlow = MutableStateFlow(CONVERSATION_ID))
            runCurrent()

            assertEquals(DEFAULT_WINDOW, windowSizes().first())
        }
    }

    @Test
    fun loadOlderMessages_withOlderMessages_doublesWhatWasLoaded() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windowSizes = givenWindowedConversationMessages(
                loadedMessageCount = DEFAULT_WINDOW,
                hasMore = true,
            )
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            delegate.loadOlderMessages()
            runCurrent()

            assertEquals(DEFAULT_WINDOW * 2, windowSizes().first())
        }
    }

    @Test
    fun loadOlderMessages_calledTwiceBeforeTheLargerWindowArrives_growsOnce() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windowSizes = givenWindowedConversationMessages(
                loadedMessageCount = DEFAULT_WINDOW,
                hasMore = true,
            )
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            delegate.loadOlderMessages()
            delegate.loadOlderMessages()
            runCurrent()

            assertEquals(DEFAULT_WINDOW * 2, windowSizes().first())
        }
    }

    @Test
    fun loadOlderMessages_withoutOlderMessages_doesNotGrowTheWindow() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windowSizes = givenWindowedConversationMessages(loadedMessageCount = 10)
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            delegate.loadOlderMessages()
            runCurrent()

            assertEquals(DEFAULT_WINDOW, windowSizes().first())
        }
    }

    @Test
    fun bind_afterProcessDeath_restoresTheGrownWindowUpToTheCap() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windowSizes = givenConversationMessagesUpTo(messageCount = LARGE_WINDOW)
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()
            // Grow from 500 to 8000, well past the 4000 restore cap, so restore must clamp to
            // the cap rather than retain 8000 or fall back to 500.
            repeat(4) {
                delegate.loadOlderMessages()
                runCurrent()
            }
            assertEquals(LARGE_WINDOW, windowSizes().first())

            // A brand new delegate, handed the state the framework would have saved and restored.
            val restored = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
                savedStateHandle = savedStateHandle.afterProcessDeath(),
            )
            runCurrent()

            assertEquals(DEFAULT_WINDOW * 8, windowSizes().first())
            val messages = (restored.state.value as ConversationMessagesUiState.Present).messages
            assertEquals(DEFAULT_WINDOW * 8, messages.size)
            assertEquals(MessageId(OLDEST_MESSAGE_ID), messages.first().messageId)
        }
    }

    @Test
    fun bind_toAnotherConversation_startsFromTheDefaultWindowAgain() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windowSizes = givenWindowedConversationMessages(
                loadedMessageCount = DEFAULT_WINDOW,
                hasMore = true,
            )
            val conversationIdFlow = MutableStateFlow<ConversationId?>(CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()
            delegate.loadOlderMessages()
            runCurrent()

            conversationIdFlow.value = OTHER_CONVERSATION_ID
            runCurrent()

            assertEquals(DEFAULT_WINDOW, windowSizes().first())
        }
    }

    @Test
    fun newMessages_afterARequestThatFoundEverythingLoaded_growTheWindow() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            // Short enough to fit in the window, so the request from the oldest message is ignored.
            val windows = MutableStateFlow(
                value = messagesWindow(loadedMessageCount = DEFAULT_WINDOW - 1, hasMore = false),
            )
            val windowSizes = givenChangingConversationMessages(windows = windows)
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()
            delegate.loadOlderMessages()
            runCurrent()
            assertEquals(DEFAULT_WINDOW, windowSizes().first())

            // Two more arrive and push the oldest message out of the window, while the user is
            // still sitting at the oldest edge and will not ask for more from there.
            windows.value = messagesWindow(loadedMessageCount = DEFAULT_WINDOW, hasMore = true)
            runCurrent()

            assertEquals(DEFAULT_WINDOW * 2, windowSizes().first())
        }
    }

    /**
     * Answers every conversation with the newest [messageCount] messages at most, as many of them
     * as the window asks for, and hands back the window sizes the delegate asked for.
     */
    private fun givenConversationMessagesUpTo(messageCount: Int): () -> Flow<Int> {
        val message = message(messageId = "message")
        val oldestMessage = message(messageId = OLDEST_MESSAGE_ID)
        val capturedWindowSizes = slot<Flow<Int>>()

        every {
            conversationsRepository.getConversationMessages(
                conversationId = any(),
                windowSizes = capture(capturedWindowSizes),
            )
        } answers {
            capturedWindowSizes.captured.map { windowSize ->
                val loadedMessageCount = minOf(windowSize, messageCount)

                ConversationMessagesWindow(
                    // Oldest first, the way the repository hands the window over.
                    messages = List(loadedMessageCount) { index ->
                        when (index) {
                            0 -> oldestMessage
                            else -> message
                        }
                    },
                    hasMore = loadedMessageCount < messageCount,
                )
            }
        }

        return { capturedWindowSizes.captured }
    }

    /** Answers every conversation with the same fixed window. */
    private fun givenWindowedConversationMessages(
        loadedMessageCount: Int,
        hasMore: Boolean = false,
    ): () -> Flow<Int> {
        return givenChangingConversationMessages(
            windows = flowOf(
                messagesWindow(loadedMessageCount = loadedMessageCount, hasMore = hasMore),
            ),
        )
    }

    /**
     * Answers every conversation with [windows], re-reading it whenever the delegate asks for
     * another size, and hands back the window sizes the delegate asked for.
     */
    private fun givenChangingConversationMessages(
        windows: Flow<ConversationMessagesWindow>,
    ): () -> Flow<Int> {
        val capturedWindowSizes = slot<Flow<Int>>()

        every {
            conversationsRepository.getConversationMessages(
                conversationId = any(),
                windowSizes = capture(capturedWindowSizes),
            )
        } answers {
            combine(capturedWindowSizes.captured, windows) { _, window -> window }
        }

        return { capturedWindowSizes.captured }
    }

    private fun messagesWindow(
        loadedMessageCount: Int,
        hasMore: Boolean,
    ): ConversationMessagesWindow {
        val message = message(messageId = "message")

        return ConversationMessagesWindow(
            messages = List(loadedMessageCount) { message },
            hasMore = hasMore,
        )
    }

    /**
     * A message to repeat across a whole window: only its size and its oldest message matter here,
     * and a relaxed mock per message is enough to exhaust the test heap.
     */
    private fun message(messageId: String): ConversationMessageData {
        return mockk<ConversationMessageData>(relaxed = true).also { data ->
            every { messageUiModelMapper.map(data = data) } returns
                messageUiModel(messageId = messageId)
        }
    }

    private companion object {
        private const val DEFAULT_WINDOW = 500
        private const val LARGE_WINDOW = DEFAULT_WINDOW * 16
        private const val OLDEST_MESSAGE_ID = "message-oldest"
        private val OTHER_CONVERSATION_ID = ConversationId("conversation-other")
    }
}
