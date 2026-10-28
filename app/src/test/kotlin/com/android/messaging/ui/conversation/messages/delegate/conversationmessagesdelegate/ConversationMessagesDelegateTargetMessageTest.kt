package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.model.message.ConversationMessagePosition
import com.android.messaging.data.conversation.model.message.ConversationMessagesWindow
import com.android.messaging.data.conversation.model.message.ConversationMessagesWindowRequest
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessagesDelegateTargetMessageTest :
    BaseConversationMessagesDelegateTest() {

    @Test
    fun loadMessage_targetOutsideTheWindow_movesTheWindowAroundIt() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windows = givenConversationMessages()
            givenTargetPosition(position = TARGET_POSITION)
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            delegate.loadMessage(messageId = TARGET_MESSAGE_ID)
            runCurrent()

            assertEquals(windowAroundTheTarget(), windows().first())
            val messagesState = presentState(delegate = delegate)
            assertEquals(MOVED_WINDOW_ID, messagesState.windowId)
            assertNull(messagesState.missingMessageId)
            assertTrue(messagesState.messages.any { it.messageId == TARGET_MESSAGE_ID })
            assertTrue(messagesState.hasNewerMessages)
            assertEquals(LATEST_MESSAGE_ID, messagesState.latestUnloadedMessage?.messageId)
        }
    }

    @Test
    fun loadMessage_beforeTheFirstWindowIsIn_showsOnlyTheWindowAroundTheTarget() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val loadedWindows = MutableSharedFlow<ConversationMessagesWindow>()
            every {
                conversationsRepository.getConversationMessages(
                    conversationId = any(),
                    windows = any(),
                )
            } returns loadedWindows
            givenTargetPosition(position = TARGET_POSITION)
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()
            delegate.loadMessage(messageId = TARGET_MESSAGE_ID)
            runCurrent()

            // The newest messages it was loading when it moved.
            loadedWindows.emit(
                ConversationMessagesWindow(
                    request = NEWEST_WINDOW,
                    messages = listOf(message(messageId = "message-newest")),
                    hasOlder = true,
                ),
            )
            runCurrent()
            assertEquals(ConversationMessagesUiState.Loading, delegate.state.value)

            loadedWindows.emit(
                ConversationMessagesWindow(
                    request = windowAroundTheTarget(),
                    messages = listOf(message(messageId = TARGET_MESSAGE_ID.value)),
                    hasOlder = true,
                    hasNewer = true,
                ),
            )
            runCurrent()

            val messagesState = presentState(delegate = delegate)
            assertEquals(listOf(TARGET_MESSAGE_ID), messagesState.messages.map { it.messageId })
            assertEquals(MOVED_WINDOW_ID, messagesState.windowId)
        }
    }

    @Test
    fun startAtMessage_beforeTheConversationBinds_onlyEverLoadsTheWindowAroundIt() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val requests = givenRecordedWindowRequests()
            givenTargetPosition(position = TARGET_POSITION)
            val conversationIdFlow = MutableStateFlow<ConversationId?>(null)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()

            delegate.startAtMessage(conversationId = CONVERSATION_ID, messageId = TARGET_MESSAGE_ID)
            conversationIdFlow.value = CONVERSATION_ID
            runCurrent()

            assertEquals(listOf(windowAroundTheTarget()), requests)
            assertTrue(
                presentState(delegate = delegate).messages.any { message ->
                    message.messageId == TARGET_MESSAGE_ID
                },
            )
        }
    }

    @Test
    fun startAtMessage_deletedMessage_reportsItMissingAndLoadsTheNewestMessages() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val requests = givenRecordedWindowRequests()
            givenTargetPosition(position = null)
            val conversationIdFlow = MutableStateFlow<ConversationId?>(null)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()

            delegate.startAtMessage(conversationId = CONVERSATION_ID, messageId = TARGET_MESSAGE_ID)
            conversationIdFlow.value = CONVERSATION_ID
            runCurrent()

            assertEquals(listOf(NEWEST_WINDOW), requests)
            assertEquals(TARGET_MESSAGE_ID, presentState(delegate = delegate).missingMessageId)
        }
    }

    @Test
    fun startAtMessage_ofAnotherConversation_isNotWhereThisOneStarts() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val requests = givenRecordedWindowRequests()
            val conversationIdFlow = MutableStateFlow<ConversationId?>(null)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()

            delegate.startAtMessage(
                conversationId = OTHER_CONVERSATION_ID,
                messageId = TARGET_MESSAGE_ID,
            )
            conversationIdFlow.value = CONVERSATION_ID
            runCurrent()

            assertEquals(listOf(NEWEST_WINDOW), requests)
            coVerify(exactly = 0) {
                conversationsRepository.getMessagePosition(
                    conversationId = any(),
                    messageId = any(),
                )
            }
        }
    }

    @Test
    fun loadMessage_deletedTarget_reportsItMissingWithoutMovingTheWindow() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windows = givenConversationMessages()
            givenTargetPosition(position = null)
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            delegate.loadMessage(messageId = TARGET_MESSAGE_ID)
            runCurrent()

            assertEquals(TARGET_MESSAGE_ID, presentState(delegate = delegate).missingMessageId)
            assertEquals(NEWEST_WINDOW, windows().first())
        }
    }

    @Test
    fun loadMessage_targetAlreadyInTheWindow_neitherLooksItUpNorMoves() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windows = givenConversationMessages(
                newestMessage = message(messageId = TARGET_MESSAGE_ID.value),
            )
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            delegate.loadMessage(messageId = TARGET_MESSAGE_ID)
            runCurrent()

            coVerify(exactly = 0) {
                conversationsRepository.getMessagePosition(
                    conversationId = any(),
                    messageId = any(),
                )
            }
            assertEquals(NEWEST_WINDOW, windows().first())
        }
    }

    @Test
    fun loadMessage_positionArrivingAfterTheConversationChanged_isDropped() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windows = givenConversationMessages()
            val position = CompletableDeferred<ConversationMessagePosition?>()
            coEvery {
                conversationsRepository.getMessagePosition(
                    conversationId = CONVERSATION_ID,
                    messageId = TARGET_MESSAGE_ID,
                )
            } coAnswers { position.await() }
            val conversationIdFlow = MutableStateFlow<ConversationId?>(CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()
            delegate.loadMessage(messageId = TARGET_MESSAGE_ID)
            runCurrent()

            conversationIdFlow.value = OTHER_CONVERSATION_ID
            runCurrent()
            position.complete(null)
            runCurrent()

            assertNull(presentState(delegate = delegate).missingMessageId)
            assertEquals(NEWEST_WINDOW, windows().first())
        }
    }

    @Test
    fun loadMessage_beforeBind_doesNotLookItUp() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = createDelegate()

            delegate.loadMessage(messageId = TARGET_MESSAGE_ID)
            runCurrent()

            coVerify(exactly = 0) {
                conversationsRepository.getMessagePosition(
                    conversationId = any(),
                    messageId = any(),
                )
            }
        }
    }

    @Test
    fun loadNewerMessages_aroundAMessage_doublesTheNewerSideOnly() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windows = givenConversationMessages(hasOlder = true)
            val delegate = createBoundDelegateAroundTheTarget()

            delegate.loadNewerMessages()
            delegate.loadNewerMessages()
            runCurrent()

            assertEquals(windowAroundTheTarget(newerSize = AROUND_SIDE * 2), windows().first())
            assertEquals(MOVED_WINDOW_ID, presentState(delegate = delegate).windowId)
        }
    }

    @Test
    fun loadOlderMessages_aroundAMessage_doublesTheOlderSideOnly() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windows = givenConversationMessages(hasOlder = true)
            val delegate = createBoundDelegateAroundTheTarget()

            delegate.loadOlderMessages()
            runCurrent()

            assertEquals(windowAroundTheTarget(olderSize = AROUND_SIDE * 2), windows().first())
        }
    }

    @Test
    fun loadNewerMessages_reachingTheNewestMessage_turnsIntoTheNewestWindowOfThatSize() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            // Past one doubling of the newer side, nothing newer is left.
            val windows = givenConversationMessages(
                hasNewer = { request -> request.newerSize < AROUND_SIDE * 2 },
            )
            val delegate = createBoundDelegateAroundTheTarget()

            delegate.loadNewerMessages()
            runCurrent()

            assertEquals(
                ConversationMessagesWindowRequest.Newest(size = AROUND_SIDE * 3),
                windows().first(),
            )
            val messagesState = presentState(delegate = delegate)
            assertEquals(MOVED_WINDOW_ID, messagesState.windowId)
            assertFalse(messagesState.hasNewerMessages)
            assertNull(messagesState.latestUnloadedMessage)
        }
    }

    @Test
    fun newMessage_whileTheWindowIsAroundAnOlderMessage_leavesTheWindowWhereItIs() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val latestMessages = MutableStateFlow(
                value = message(messageId = LATEST_MESSAGE_ID.value),
            )
            val windows = givenConversationMessages(latestMessages = latestMessages)
            val delegate = createBoundDelegateAroundTheTarget()
            val messages = presentState(delegate = delegate).messages

            latestMessages.value = message(messageId = "message-arrived")
            runCurrent()

            val messagesState = presentState(delegate = delegate)
            assertEquals(windowAroundTheTarget(), windows().first())
            assertEquals(messages, messagesState.messages)
            assertEquals(
                MessageId("message-arrived"),
                messagesState.latestUnloadedMessage?.messageId,
            )
        }
    }

    @Test
    fun loadNewestMessages_aroundAMessage_swapsInTheNewestWindow() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windows = givenConversationMessages()
            val delegate = createBoundDelegateAroundTheTarget()

            delegate.loadNewestMessages()
            runCurrent()

            assertEquals(NEWEST_WINDOW, windows().first())
            val messagesState = presentState(delegate = delegate)
            assertEquals(MOVED_WINDOW_ID + 1, messagesState.windowId)
            assertFalse(messagesState.hasNewerMessages)
        }
    }

    @Test
    fun loadNewestMessages_onTheNewestWindow_keepsWhatItHasGrownTo() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windows = givenConversationMessages(hasOlder = true)
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()
            delegate.loadOlderMessages()
            runCurrent()

            delegate.loadNewestMessages()
            runCurrent()

            assertEquals(
                ConversationMessagesWindowRequest.Newest(size = NEWEST_WINDOW.size * 2),
                windows().first(),
            )
        }
    }

    @Test
    fun bind_afterProcessDeath_restoresTheWindowAroundTheMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windows = givenConversationMessages(hasOlder = true)
            val delegate = createBoundDelegateAroundTheTarget()
            delegate.loadOlderMessages()
            runCurrent()

            createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
                savedStateHandle = savedStateHandle.afterProcessDeath(),
            )
            runCurrent()

            assertEquals(windowAroundTheTarget(olderSize = AROUND_SIDE * 2), windows().first())
        }
    }

    @Test
    fun highlightMessage_untilClearedForIt_keepsTheMessageHighlighted() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenConversationMessages()
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            delegate.state.test {
                assertNull(presentState(state = awaitItem()).highlightedMessageId)

                delegate.highlightMessage(messageId = TARGET_MESSAGE_ID)
                assertEquals(
                    TARGET_MESSAGE_ID,
                    presentState(state = awaitItem()).highlightedMessageId,
                )

                // A late end of an earlier highlight must not cut this one short.
                delegate.clearMessageHighlight(messageId = MessageId("message-earlier"))
                delegate.clearMessageHighlight(messageId = TARGET_MESSAGE_ID)
                assertNull(presentState(state = awaitItem()).highlightedMessageId)

                expectNoEvents()
            }
        }
    }

    @Test
    fun bind_toAnotherConversation_dropsTheHighlightAndTheMissingMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenConversationMessages()
            givenTargetPosition(position = null)
            val conversationIdFlow = MutableStateFlow<ConversationId?>(CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()
            delegate.highlightMessage(messageId = MessageId("message-highlighted"))
            delegate.loadMessage(messageId = TARGET_MESSAGE_ID)
            runCurrent()

            conversationIdFlow.value = OTHER_CONVERSATION_ID
            runCurrent()

            val messagesState = presentState(delegate = delegate)
            assertNull(messagesState.highlightedMessageId)
            assertNull(messagesState.missingMessageId)
        }
    }

    private fun givenTargetPosition(position: ConversationMessagePosition?) {
        coEvery {
            conversationsRepository.getMessagePosition(
                conversationId = CONVERSATION_ID,
                messageId = TARGET_MESSAGE_ID,
            )
        } returns position
    }

    /** Every window the delegate asks for, each answered as soon as it is asked for. */
    private fun givenRecordedWindowRequests(): List<ConversationMessagesWindowRequest> {
        val requests = mutableListOf<ConversationMessagesWindowRequest>()
        val message = message(messageId = "message")
        val targetMessage = message(messageId = TARGET_MESSAGE_ID.value)

        every {
            conversationsRepository.getConversationMessages(
                conversationId = any(),
                windows = any(),
            )
        } answers {
            secondArg<Flow<ConversationMessagesWindowRequest>>().map { request ->
                requests += request

                ConversationMessagesWindow(
                    request = request,
                    messages = when (request) {
                        is ConversationMessagesWindowRequest.Newest -> listOf(message)
                        is ConversationMessagesWindowRequest.Around -> listOf(targetMessage)
                    },
                    hasOlder = true,
                    hasNewer = request is ConversationMessagesWindowRequest.Around,
                )
            }
        }

        return requests
    }

    private fun TestScope.createBoundDelegateAroundTheTarget(): ConversationMessagesDelegate {
        givenTargetPosition(position = TARGET_POSITION)

        return createBoundDelegate(
            conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
        ).also { delegate ->
            runCurrent()
            delegate.loadMessage(messageId = TARGET_MESSAGE_ID)
            runCurrent()
        }
    }

    /**
     * Answers every conversation with full windows: the newest ones end in [newestMessage], and
     * the ones around a message hold the target, with [latestMessages] past them while [hasNewer].
     * Hands back the windows the delegate asked for.
     */
    private fun givenConversationMessages(
        hasOlder: Boolean = false,
        hasNewer: (ConversationMessagesWindowRequest.Around) -> Boolean = { true },
        newestMessage: ConversationMessageData = message(messageId = "message-newest"),
        latestMessages: Flow<ConversationMessageData> = flowOf(
            message(messageId = LATEST_MESSAGE_ID.value),
        ),
    ): () -> Flow<ConversationMessagesWindowRequest> {
        val message = message(messageId = "message")
        val targetMessage = message(messageId = TARGET_MESSAGE_ID.value)
        val capturedWindows = slot<Flow<ConversationMessagesWindowRequest>>()

        every {
            conversationsRepository.getConversationMessages(
                conversationId = any(),
                windows = capture(capturedWindows),
            )
        } answers {
            combine(capturedWindows.captured, latestMessages) { request, latestMessage ->
                when (request) {
                    is ConversationMessagesWindowRequest.Newest -> ConversationMessagesWindow(
                        request = request,
                        messages = List(request.size - 1) { message } + newestMessage,
                        hasOlder = hasOlder,
                    )

                    is ConversationMessagesWindowRequest.Around -> {
                        val windowHasNewer = hasNewer(request)

                        ConversationMessagesWindow(
                            request = request,
                            messages = List(request.olderSize - 1) { message } +
                                targetMessage +
                                List(request.newerSize) { message },
                            hasOlder = hasOlder,
                            hasNewer = windowHasNewer,
                            latestMessage = latestMessage.takeIf { windowHasNewer },
                        )
                    }
                }
            }
        }

        return { capturedWindows.captured }
    }

    private fun windowAroundTheTarget(
        olderSize: Int = AROUND_SIDE,
        newerSize: Int = AROUND_SIDE,
    ): ConversationMessagesWindowRequest.Around {
        return ConversationMessagesWindowRequest.Around(
            position = TARGET_POSITION,
            olderSize = olderSize,
            newerSize = newerSize,
        )
    }

    private fun message(messageId: String): ConversationMessageData {
        return mockk<ConversationMessageData>(relaxed = true).also { data ->
            every { messageUiModelMapper.map(data = data) } returns
                messageUiModel(messageId = messageId)
        }
    }

    private fun presentState(
        delegate: ConversationMessagesDelegate,
    ): ConversationMessagesUiState.Present {
        return presentState(state = delegate.state.value)
    }

    private fun presentState(
        state: ConversationMessagesUiState,
    ): ConversationMessagesUiState.Present {
        return state as ConversationMessagesUiState.Present
    }

    private companion object {
        private const val AROUND_SIDE = 250
        private const val MOVED_WINDOW_ID = 1
        private val NEWEST_WINDOW = ConversationMessagesWindowRequest.Newest(size = 500)
        private val TARGET_MESSAGE_ID = MessageId("message-target")
        private val TARGET_POSITION = ConversationMessagePosition(
            messageId = TARGET_MESSAGE_ID,
            receivedTimestamp = 1_700_000_000_000L,
        )
        private val LATEST_MESSAGE_ID = MessageId("message-latest")
        private val OTHER_CONVERSATION_ID = ConversationId("conversation-other")
    }
}
