package com.android.messaging.ui.classzero

import android.content.ContentValues
import android.os.SystemClock
import android.provider.Telephony.Sms
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.domain.classzero.usecase.SaveClassZeroMessage
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.classzero.model.ClassZeroScreenEffect as Effect
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ClassZeroViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(
        testDispatcher = UnconfinedTestDispatcher(),
    )

    @Before
    fun setUp() {
        SystemClock.setCurrentTimeMillis(START_UPTIME_MILLIS)
    }

    @Test
    fun initialMessageWithBodyDisplaysMessageAndStartsTimer() {
        val viewModel = createViewModel()

        viewModel.onInitialMessageReceived(
            messageValues = messageValues(messageText = FIRST_MESSAGE)
        )

        assertUiState(
            viewModel = viewModel,
            messageText = FIRST_MESSAGE,
        )
        viewModel.onCancelClicked()
    }

    @Test
    fun saveClickedSavesReadMessageAndFinishesAfterLastMessage() {
        runTest {
            val saveClassZeroMessage = mockSaveClassZeroMessage()
            val viewModel = createViewModel(saveClassZeroMessage = saveClassZeroMessage)

            viewModel.effects.test {
                viewModel.onInitialMessageReceived(
                    messageValues = messageValues(messageText = FIRST_MESSAGE),
                )
                viewModel.onSaveClicked()

                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertNull(viewModel.uiState.value)
            val savedMessage = slot<ContentValues>()
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                saveClassZeroMessage(messageValues = capture(savedMessage))
            }
            assertSavedMessage(
                messageValues = savedMessage.captured,
                messageText = FIRST_MESSAGE,
                isRead = true,
            )
        }
    }

    @Test
    fun saveClickedFinishesOnlyAfterSaveCompletes() {
        runTest {
            val saveStarted = CompletableDeferred<Unit>()
            val saveCanComplete = CompletableDeferred<Unit>()
            val saveClassZeroMessage = mockk<SaveClassZeroMessage>()
            every { saveClassZeroMessage(messageValues = any()) } returns flow {
                saveStarted.complete(Unit)
                saveCanComplete.await()
                emit(Unit)
            }
            val viewModel = createViewModel(saveClassZeroMessage = saveClassZeroMessage)

            viewModel.effects.test {
                viewModel.onInitialMessageReceived(
                    messageValues = messageValues(messageText = FIRST_MESSAGE),
                )
                viewModel.onSaveClicked()
                saveStarted.await()

                expectNoEvents()
                assertUiState(
                    viewModel = viewModel,
                    messageText = FIRST_MESSAGE,
                )

                viewModel.onCancelClicked()
                viewModel.onSaveClicked()
                expectNoEvents()

                saveCanComplete.complete(Unit)
                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                saveClassZeroMessage(messageValues = any())
            }
        }
    }

    @Test
    fun cancelClickedDoesNotSaveMessageAndFinishesAfterLastMessage() {
        runTest {
            val saveClassZeroMessage = mockSaveClassZeroMessage()
            val viewModel = createViewModel(saveClassZeroMessage = saveClassZeroMessage)

            viewModel.effects.test {
                viewModel.onInitialMessageReceived(
                    messageValues = messageValues(messageText = FIRST_MESSAGE),
                )
                viewModel.onCancelClicked()

                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertNull(viewModel.uiState.value)
            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                saveClassZeroMessage(messageValues = any())
            }
        }
    }

    @Test
    fun timeoutSavesUnreadMessageAndFinishesAfterLastMessage() {
        runTest {
            val saveClassZeroMessage = mockSaveClassZeroMessage()
            val viewModel = createViewModel(saveClassZeroMessage = saveClassZeroMessage)

            viewModel.effects.test {
                viewModel.onInitialMessageReceived(
                    messageValues = messageValues(messageText = FIRST_MESSAGE),
                )
                advanceTimeoutBy(delayMillis = CLASS_ZERO_DEFAULT_TIMEOUT_MILLIS)

                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertNull(viewModel.uiState.value)
            val savedMessage = slot<ContentValues>()
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                saveClassZeroMessage(messageValues = capture(savedMessage))
            }
            assertSavedMessage(
                messageValues = savedMessage.captured,
                messageText = FIRST_MESSAGE,
                isRead = false,
            )
        }
    }

    @Test
    fun cancelClickedCancelsScheduledTimeout() {
        val saveClassZeroMessage = mockSaveClassZeroMessage()
        val viewModel = createViewModel(saveClassZeroMessage = saveClassZeroMessage)

        viewModel.onInitialMessageReceived(
            messageValues = messageValues(messageText = FIRST_MESSAGE)
        )
        viewModel.onCancelClicked()
        advanceTimeoutBy(delayMillis = CLASS_ZERO_DEFAULT_TIMEOUT_MILLIS)

        assertNull(viewModel.uiState.value)
        verify(exactly = 0) {
            @Suppress("UnusedFlow")
            saveClassZeroMessage(messageValues = any())
        }
    }

    @Test
    fun newMessageWhileDisplayingIsQueuedAndShownAfterCurrentCompletes() {
        runTest {
            val saveClassZeroMessage = mockSaveClassZeroMessage()
            val viewModel = createViewModel(saveClassZeroMessage = saveClassZeroMessage)

            viewModel.effects.test {
                viewModel.onInitialMessageReceived(
                    messageValues = messageValues(messageText = FIRST_MESSAGE),
                )
                viewModel.onNewMessageReceived(
                    messageValues = messageValues(messageText = SECOND_MESSAGE),
                )
                viewModel.onSaveClicked()

                expectNoEvents()
                assertUiState(
                    viewModel = viewModel,
                    messageText = SECOND_MESSAGE,
                )

                viewModel.onCancelClicked()
                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            val savedMessage = slot<ContentValues>()
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                saveClassZeroMessage(messageValues = capture(savedMessage))
            }
            assertSavedMessage(
                messageValues = savedMessage.captured,
                messageText = FIRST_MESSAGE,
                isRead = true,
            )
        }
    }

    @Test
    fun initialMessageIsIgnoredWhenCurrentMessageAlreadyExists() {
        val saveClassZeroMessage = mockSaveClassZeroMessage()
        val viewModel = createViewModel(saveClassZeroMessage = saveClassZeroMessage)

        viewModel.onInitialMessageReceived(
            messageValues = messageValues(messageText = FIRST_MESSAGE)
        )
        viewModel.onInitialMessageReceived(
            messageValues = messageValues(messageText = SECOND_MESSAGE)
        )
        viewModel.onSaveClicked()

        assertNull(viewModel.uiState.value)
        val savedMessage = slot<ContentValues>()
        verify(exactly = 1) {
            @Suppress("UnusedFlow")
            saveClassZeroMessage(messageValues = capture(savedMessage))
        }
        assertSavedMessage(
            messageValues = savedMessage.captured,
            messageText = FIRST_MESSAGE,
            isRead = true,
        )
    }

    @Test
    fun emptyInitialMessageFinishesWhenQueueIsEmpty() {
        runTest {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onInitialMessageReceived(messageValues = null)

                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun blankInitialMessageFinishesWhenQueueIsEmpty() {
        runTest {
            val saveClassZeroMessage = mockSaveClassZeroMessage()
            val viewModel = createViewModel(saveClassZeroMessage = saveClassZeroMessage)

            viewModel.effects.test {
                viewModel.onInitialMessageReceived(
                    messageValues = messageValues(messageText = BLANK_MESSAGE),
                )

                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertNull(viewModel.uiState.value)
            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                saveClassZeroMessage(messageValues = any())
            }
        }
    }

    @Test
    fun restoredTimerIsUsedOnlyForFirstDisplayedMessage() {
        runTest {
            val saveClassZeroMessage = mockSaveClassZeroMessage()
            val savedStateHandle = SavedStateHandle(
                mapOf(CLASS_ZERO_TIMER_FIRE_STATE_KEY to RESTORED_TIMER_FIRE_UPTIME_MILLIS),
            )
            val viewModel = createViewModel(
                saveClassZeroMessage = saveClassZeroMessage,
                savedStateHandle = savedStateHandle,
            )

            viewModel.effects.test {
                viewModel.onInitialMessageReceived(
                    messageValues = messageValues(messageText = FIRST_MESSAGE),
                )
                viewModel.onNewMessageReceived(
                    messageValues = messageValues(messageText = SECOND_MESSAGE),
                )

                assertUiState(
                    viewModel = viewModel,
                    messageText = FIRST_MESSAGE,
                )

                advanceTimeoutBy(
                    delayMillis = RESTORED_TIMER_FIRE_UPTIME_MILLIS - START_UPTIME_MILLIS - 1L,
                )
                expectNoEvents()
                assertUiState(
                    viewModel = viewModel,
                    messageText = FIRST_MESSAGE,
                )

                advanceTimeoutBy(delayMillis = 1L)
                expectNoEvents()
                assertUiState(
                    viewModel = viewModel,
                    messageText = SECOND_MESSAGE,
                )

                val savedMessage = slot<ContentValues>()
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    saveClassZeroMessage(messageValues = capture(savedMessage))
                }
                assertSavedMessage(
                    messageValues = savedMessage.captured,
                    messageText = FIRST_MESSAGE,
                    isRead = false,
                )

                advanceTimeoutBy(delayMillis = CLASS_ZERO_DEFAULT_TIMEOUT_MILLIS - 1L)
                expectNoEvents()
                assertUiState(
                    viewModel = viewModel,
                    messageText = SECOND_MESSAGE,
                )

                viewModel.onCancelClicked()
                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private fun createViewModel(
        saveClassZeroMessage: SaveClassZeroMessage = mockSaveClassZeroMessage(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        defaultDispatcher: CoroutineDispatcher = mainDispatcherRule.testDispatcher,
        mainDispatcher: CoroutineDispatcher = mainDispatcherRule.testDispatcher,
    ): ClassZeroViewModel {
        return ClassZeroViewModel(
            saveClassZeroMessage = saveClassZeroMessage,
            savedStateHandle = savedStateHandle,
            defaultDispatcher = defaultDispatcher,
            mainDispatcher = mainDispatcher,
        )
    }

    private fun messageValues(messageText: String): ContentValues {
        return ContentValues().apply {
            put(Sms.BODY, messageText)
        }
    }

    private fun assertUiState(
        viewModel: ClassZeroViewModel,
        messageText: String,
    ) {
        val uiState = requireNotNull(viewModel.uiState.value)
        assertEquals(messageText, uiState.messageText)
    }

    private fun assertSavedMessage(
        messageValues: ContentValues,
        messageText: String,
        isRead: Boolean,
    ) {
        val readValue = when {
            isRead -> 1
            else -> 0
        }
        assertEquals(messageText, messageValues.getAsString(Sms.BODY))
        assertEquals(readValue, messageValues.getAsInteger(Sms.Inbox.READ))
    }

    private fun mockSaveClassZeroMessage(): SaveClassZeroMessage {
        val saveClassZeroMessage = mockk<SaveClassZeroMessage>()
        every { saveClassZeroMessage(messageValues = any()) } returns flowOf(Unit)

        return saveClassZeroMessage
    }

    private fun advanceTimeoutBy(delayMillis: Long) {
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(delayMillis)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
    }

    private companion object {
        private const val FIRST_MESSAGE = "First class zero message"
        private const val SECOND_MESSAGE = "Second class zero message"
        private const val BLANK_MESSAGE = "   "
        private const val START_UPTIME_MILLIS = 1_000L
        private const val RESTORED_TIMER_FIRE_UPTIME_MILLIS = 42_000L
    }
}
