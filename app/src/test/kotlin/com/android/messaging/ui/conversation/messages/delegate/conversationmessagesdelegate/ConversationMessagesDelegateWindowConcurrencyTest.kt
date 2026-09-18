package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversation.model.message.ConversationMessagesWindow
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessagesDelegateWindowConcurrencyTest :
    BaseConversationMessagesDelegateTest() {

    @Test
    fun growingTheWindow_neverTouchesTheSavedStateFromTheLoadingDispatcher() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val loadingDispatcher = LoadingDispatcher(mainDispatcherRule.testDispatcher)
            val calls = mutableListOf<Pair<String, String>>()
            val hasOlderMessages = MutableStateFlow(value = false)
            val windowSizes = givenConversationMessagesFillingTheWindow(
                hasOlderMessages = hasOlderMessages,
            )
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
                savedStateHandle = recordingItsCalls(calls, loadingDispatcher),
                defaultDispatcher = loadingDispatcher,
            )
            runCurrent()
            delegate.loadOlderMessages()
            runCurrent()

            // The request found everything loaded, so the window is grown by the observer, on the
            // loading dispatcher, once older messages turn up.
            hasOlderMessages.value = true
            runCurrent()

            assertEquals(DEFAULT_WINDOW * 2, windowSizes().first())
            assertFalse(calls.toString(), calls.any { (_, dispatcher) -> dispatcher == LOADING })
        }
    }

    @Test
    fun savingTheState_leavesTheGrownWindowAlone() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val windowSizes = givenConversationMessagesFillingTheWindow()
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            // What saving reads, taken before the window grows underneath it.
            val readWhileSaving = savedStateHandle.savedStateProvider().saveState()
            delegate.loadOlderMessages()
            runCurrent()
            assertEquals(DEFAULT_WINDOW * 2, windowSizes().first())

            writeBackWhatSavingRead(readWhileSaving)
            runCurrent()

            assertEquals(DEFAULT_WINDOW * 2, windowSizes().first())
        }
    }

    private fun writeBackWhatSavingRead(readWhileSaving: Bundle) {
        val readValues = SavedStateHandle.createHandle(readWhileSaving, null)

        readValues.keys().forEach { key ->
            savedStateHandle[key] = readValues.get<Any?>(key)
        }
    }

    @Test
    fun loadOlderMessages_whileAWindowArrives_growsTheWindowOnce() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val newMessages = MutableStateFlow(value = 0)
            val windowSizes = givenConversationMessagesFillingTheWindow(
                newMessages = newMessages,
            )
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            val askedForOlderMessages = CountDownLatch(1)
            val letTheRequestFinish = CountDownLatch(1)
            pauseTheRequestBeforeItGrowsTheWindow(
                delegate = delegate,
                paused = askedForOlderMessages,
                resumed = letTheRequestFinish,
            )
            val request = Thread({ delegate.loadOlderMessages() }, REQUEST_THREAD_NAME)
            request.start()

            try {
                assertTrue(askedForOlderMessages.await(AWAIT_SECONDS, TimeUnit.SECONDS))

                // A message arrives while the request is pending, and the window observer finds it.
                newMessages.value += 1
                runCurrent()
                assertEquals(DEFAULT_WINDOW * 2, windowSizes().first())
            } finally {
                letTheRequestFinish.countDown()
                request.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
            }
            runCurrent()

            assertEquals(DEFAULT_WINDOW * 2, windowSizes().first())
        }
    }

    @OptIn(ExperimentalForInheritanceCoroutinesApi::class)
    private fun pauseTheRequestBeforeItGrowsTheWindow(
        delegate: Any,
        paused: CountDownLatch,
        resumed: CountDownLatch,
    ) {
        val hasOlderMessages = ReflectionHelpers
            .getField<MutableStateFlow<Boolean>>(delegate, HAS_OLDER_MESSAGES)

        val pauseOnce = AtomicBoolean(true)
        val pausingRead = object : MutableStateFlow<Boolean> by hasOlderMessages {
            override var value: Boolean
                get() {
                    val currentValue = hasOlderMessages.value

                    if (Thread.currentThread().name == REQUEST_THREAD_NAME &&
                        pauseOnce.compareAndSet(true, false)
                    ) {
                        paused.countDown()
                        check(resumed.await(AWAIT_SECONDS, TimeUnit.SECONDS))
                    }

                    return currentValue
                }
                set(value) {
                    hasOlderMessages.value = value
                }
        }

        ReflectionHelpers.setField(delegate, HAS_OLDER_MESSAGES, pausingRead)
    }

    private fun recordingItsCalls(
        calls: MutableList<Pair<String, String>>,
        loadingDispatcher: LoadingDispatcher? = null,
    ): SavedStateHandle {
        fun called(call: String) {
            calls += call to when (loadingDispatcher?.isRunning) {
                true -> LOADING
                else -> MAIN
            }
        }

        return spyk(SavedStateHandle()) {
            every { get<Any?>(any()) } answers {
                called("get")
                callOriginal()
            }
            every { set<Any?>(any(), any()) } answers {
                called("set")
                callOriginal()
            }
            every { getMutableStateFlow<Any?>(any(), any()) } answers {
                called(GET_MUTABLE_STATE_FLOW)
                callOriginal()
            }
            every { remove<Any?>(any()) } answers {
                called("remove")
                callOriginal()
            }
            every { setSavedStateProvider(any(), any()) } answers {
                called("setSavedStateProvider")
                callOriginal()
            }
        }
    }

    private fun givenConversationMessagesFillingTheWindow(
        newMessages: Flow<Int> = MutableStateFlow(value = 0),
        hasOlderMessages: Flow<Boolean> = MutableStateFlow(value = true),
    ): () -> Flow<Int> {
        val message = mockk<ConversationMessageData>(relaxed = true)
        every { messageUiModelMapper.map(data = message) } returns
            messageUiModel(messageId = "message")
        val capturedWindowSizes = slot<Flow<Int>>()

        every {
            conversationsRepository.getConversationMessages(
                conversationId = any(),
                windowSizes = capture(capturedWindowSizes),
            )
        } answers {
            combine(
                capturedWindowSizes.captured,
                newMessages,
                hasOlderMessages,
            ) { windowSize, _, hasMore ->
                val loadedMessageCount = minOf(windowSize, MESSAGE_COUNT)

                ConversationMessagesWindow(
                    messages = List(loadedMessageCount) { message },
                    hasMore = hasMore && loadedMessageCount < MESSAGE_COUNT,
                )
            }
        }

        return { capturedWindowSizes.captured }
    }

    private class LoadingDispatcher(
        private val testDispatcher: CoroutineDispatcher,
    ) : CoroutineDispatcher() {

        var isRunning = false
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            testDispatcher.dispatch(context) {
                isRunning = true

                try {
                    block.run()
                } finally {
                    isRunning = false
                }
            }
        }
    }

    private companion object {
        private const val DEFAULT_WINDOW = 500
        private const val MESSAGE_COUNT = DEFAULT_WINDOW * 4
        private const val AWAIT_SECONDS = 5L
        private const val MAIN = "main"
        private const val LOADING = "loading dispatcher"
        private const val GET_MUTABLE_STATE_FLOW = "getMutableStateFlow"
        private const val HAS_OLDER_MESSAGES = "hasOlderMessages"
        private const val REQUEST_THREAD_NAME = "test-load-older-messages"
    }
}
