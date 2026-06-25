package com.android.messaging.ui.smsstoragelow

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import com.android.messaging.domain.smsstoragelow.model.SmsStorageRetentionDuration
import com.android.messaging.domain.smsstoragelow.usecase.CancelSmsStorageLowNotification
import com.android.messaging.domain.smsstoragelow.usecase.GetSmsStorageLowWarningActions
import com.android.messaging.domain.smsstoragelow.usecase.ReleaseSmsStorage
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.smsstoragelow.model.SmsStorageLowWarningScreenEffect as Effect
import com.android.messaging.util.LogUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmsStorageLowWarningViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mediaAction = SmsStorageLowWarningAction.DeleteMediaMessages(
        retentionDuration = retentionDuration(),
    )
    private val oldMessagesAction = SmsStorageLowWarningAction.DeleteOldMessages(
        retentionDuration = retentionDuration(),
    )
    private val actions = persistentListOf(mediaAction, oldMessagesAction)

    private val getActions = mockk<GetSmsStorageLowWarningActions>()
    private val releaseStorage = mockk<ReleaseSmsStorage>()
    private val cancelNotification = mockk<CancelSmsStorageLowNotification>()

    @Test
    fun init_loadsStorageActions() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(actions, viewModel.uiState.value.actions)
            assertFalse(viewModel.uiState.value.isLoading)
        }
    }

    @Test
    fun init_whenLoadingActionsFails_buffersFinishUntilEffectsAreCollected() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            mockkStatic(LogUtil::class)
            try {
                every { LogUtil.e(any(), any(), any()) } returns Unit
                val viewModel = createViewModel(
                    actionsFlow = flow {
                        throw IllegalStateException("Failed to load actions")
                    },
                )

                advanceUntilIdle()

                viewModel.effects.test {
                    assertEquals(Effect.Finish, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                unmockkStatic(LogUtil::class)
            }
        }
    }

    @Test
    fun init_whenSelectedActionRestored_restoresConfirmation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val savedStateHandle = SavedStateHandle()
            val initialViewModel = createViewModel(savedStateHandle = savedStateHandle)
            advanceUntilIdle()
            initialViewModel.onActionClicked(oldMessagesAction)

            val restoredViewModel = createViewModel(savedStateHandle = savedStateHandle)
            advanceUntilIdle()

            assertEquals(oldMessagesAction, restoredViewModel.uiState.value.selectedAction)
        }
    }

    @Test
    fun init_whenMediaActionRestored_restoresConfirmation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val savedStateHandle = SavedStateHandle()
            val initialViewModel = createViewModel(savedStateHandle = savedStateHandle)
            advanceUntilIdle()
            initialViewModel.onActionClicked(mediaAction)

            val restoredViewModel = createViewModel(savedStateHandle = savedStateHandle)
            advanceUntilIdle()

            assertEquals(mediaAction, restoredViewModel.uiState.value.selectedAction)
        }
    }

    @Test
    fun actionClicked_selectsActionAndStoresIt() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val savedStateHandle = SavedStateHandle()
            val viewModel = createViewModel(savedStateHandle = savedStateHandle)
            advanceUntilIdle()

            viewModel.onActionClicked(oldMessagesAction)

            assertEquals(oldMessagesAction, viewModel.uiState.value.selectedAction)
            assertNotNull(
                savedStateHandle.get<String>(
                    SMS_STORAGE_LOW_WARNING_SELECTED_ACTION_STATE_KEY,
                ),
            )
        }
    }

    @Test
    fun actionClicked_whenActionIsNotAvailable_ignoresAction() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val savedStateHandle = SavedStateHandle()
            val viewModel = createViewModel(savedStateHandle = savedStateHandle)
            val unavailableAction = SmsStorageLowWarningAction.DeleteOldMessages(
                retentionDuration = SmsStorageRetentionDuration(
                    count = 2,
                    unit = SmsStorageRetentionDuration.DurationUnit.MONTH,
                    millis = 2L * 30L * 24L * 60L * 60L * 1000L,
                ),
            )
            advanceUntilIdle()

            viewModel.onActionClicked(action = unavailableAction)

            assertNull(viewModel.uiState.value.selectedAction)
            assertNull(
                savedStateHandle.get<String>(
                    SMS_STORAGE_LOW_WARNING_SELECTED_ACTION_STATE_KEY,
                ),
            )
        }
    }

    @Test
    fun actionClicked_whenProcessing_ignoresAction() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val releaseCompletion = CompletableDeferred<Unit>()
            val viewModel = createViewModel()
            advanceUntilIdle()
            every { releaseStorage(mediaAction) } returns flow {
                releaseCompletion.await()
                emit(Unit)
            }

            viewModel.onActionClicked(mediaAction)
            viewModel.onCleanupConfirmed()
            advanceUntilIdle()
            viewModel.onActionClicked(oldMessagesAction)

            assertEquals(mediaAction, viewModel.uiState.value.selectedAction)

            releaseCompletion.complete(Unit)
            advanceUntilIdle()
        }
    }

    @Test
    fun confirmationDismissed_clearsSelectedAction() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val savedStateHandle = SavedStateHandle()
            val viewModel = createViewModel(savedStateHandle = savedStateHandle)
            advanceUntilIdle()

            viewModel.onActionClicked(mediaAction)
            viewModel.onConfirmationDismissed()

            assertNull(viewModel.uiState.value.selectedAction)
            assertNull(
                savedStateHandle.get<String>(
                    SMS_STORAGE_LOW_WARNING_SELECTED_ACTION_STATE_KEY,
                ),
            )
        }
    }

    @Test
    fun confirmationDismissed_whenProcessing_ignoresDismiss() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val releaseCompletion = CompletableDeferred<Unit>()
            val viewModel = createViewModel()
            advanceUntilIdle()
            every { releaseStorage(mediaAction) } returns flow {
                releaseCompletion.await()
                emit(Unit)
            }

            viewModel.onActionClicked(mediaAction)
            viewModel.onCleanupConfirmed()
            advanceUntilIdle()
            viewModel.onConfirmationDismissed()

            assertEquals(mediaAction, viewModel.uiState.value.selectedAction)

            releaseCompletion.complete(Unit)
            advanceUntilIdle()
        }
    }

    @Test
    fun ignoreClicked_emitsFinishWithoutCleanup() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onIgnoreClicked()

                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                releaseStorage(any())
            }
            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                cancelNotification()
            }
        }
    }

    @Test
    fun ignoreClicked_whenProcessing_ignoresClick() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val releaseCompletion = CompletableDeferred<Unit>()
            val viewModel = createViewModel()
            advanceUntilIdle()
            every { releaseStorage(mediaAction) } returns flow {
                releaseCompletion.await()
                emit(Unit)
            }

            viewModel.onActionClicked(mediaAction)

            viewModel.effects.test {
                viewModel.onCleanupConfirmed()
                advanceUntilIdle()
                viewModel.onIgnoreClicked()
                advanceUntilIdle()

                expectNoEvents()

                releaseCompletion.complete(Unit)
                advanceUntilIdle()

                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun cleanupConfirmed_whenNoActionSelected_doesNothing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onCleanupConfirmed()
                advanceUntilIdle()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                releaseStorage(any())
            }
            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                cancelNotification()
            }
            assertFalse(viewModel.uiState.value.isProcessing)
        }
    }

    @Test
    fun cleanupConfirmed_releasesStorageCancelsNotificationAndFinishes() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onActionClicked(oldMessagesAction)

            viewModel.effects.test {
                viewModel.onCleanupConfirmed()
                advanceUntilIdle()

                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                releaseStorage(oldMessagesAction)
            }
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                cancelNotification()
            }
            assertFalse(viewModel.uiState.value.isProcessing)
            assertNull(viewModel.uiState.value.selectedAction)
        }
    }

    @Test
    fun cleanupConfirmed_whenReleaseIsPending_keepsProcessingAndDoesNotFinish() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val releaseCompletion = CompletableDeferred<Unit>()
            val viewModel = createViewModel()
            advanceUntilIdle()
            every { releaseStorage(mediaAction) } returns flow {
                releaseCompletion.await()
                emit(Unit)
            }

            viewModel.onActionClicked(mediaAction)
            assertEquals(mediaAction, viewModel.uiState.value.selectedAction)

            viewModel.effects.test {
                viewModel.onCleanupConfirmed()
                advanceUntilIdle()

                assertTrue(viewModel.uiState.value.isProcessing)
                expectNoEvents()

                releaseCompletion.complete(Unit)
                advanceUntilIdle()

                assertEquals(Effect.Finish, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun cleanupConfirmed_whenAlreadyProcessing_ignoresSecondConfirm() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val releaseCompletion = CompletableDeferred<Unit>()
            val viewModel = createViewModel()
            advanceUntilIdle()
            every { releaseStorage(mediaAction) } returns flow {
                releaseCompletion.await()
                emit(Unit)
            }

            viewModel.onActionClicked(mediaAction)
            viewModel.onCleanupConfirmed()
            viewModel.onCleanupConfirmed()
            advanceUntilIdle()

            releaseCompletion.complete(Unit)
            advanceUntilIdle()

            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                releaseStorage(mediaAction)
            }
        }
    }

    @Test
    fun cleanupConfirmed_whenCleanupFails_finishesAndStopsProcessing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            mockkStatic(LogUtil::class)
            try {
                every { LogUtil.e(any(), any(), any()) } returns Unit
                val viewModel = createViewModel()
                advanceUntilIdle()
                every { releaseStorage(any()) } returns flow {
                    throw IllegalStateException("Failed to release storage")
                }

                viewModel.onActionClicked(mediaAction)

                viewModel.effects.test {
                    viewModel.onCleanupConfirmed()
                    advanceUntilIdle()

                    assertEquals(Effect.Finish, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }

                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    releaseStorage(mediaAction)
                }
                verify(exactly = 0) {
                    @Suppress("UnusedFlow")
                    cancelNotification()
                }
                assertFalse(viewModel.uiState.value.isProcessing)
            } finally {
                unmockkStatic(LogUtil::class)
            }
        }
    }

    @Test
    fun cleanupConfirmed_whenCancelNotificationFails_finishesAndStopsProcessing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            mockkStatic(LogUtil::class)
            try {
                every { LogUtil.e(any(), any(), any()) } returns Unit
                val viewModel = createViewModel()
                advanceUntilIdle()
                every { cancelNotification() } returns flow {
                    throw IllegalStateException("Failed to cancel notification")
                }

                viewModel.onActionClicked(mediaAction)

                viewModel.effects.test {
                    viewModel.onCleanupConfirmed()
                    advanceUntilIdle()

                    assertEquals(Effect.Finish, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }

                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    releaseStorage(mediaAction)
                }
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    cancelNotification()
                }
                assertFalse(viewModel.uiState.value.isProcessing)
            } finally {
                unmockkStatic(LogUtil::class)
            }
        }
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        actionsFlow: Flow<ImmutableList<SmsStorageLowWarningAction>> = flowOf(actions),
    ): SmsStorageLowWarningViewModel {
        every { getActions() } returns actionsFlow
        every { releaseStorage(any()) } returns flowOf(Unit)
        every { cancelNotification() } returns flowOf(Unit)

        return SmsStorageLowWarningViewModel(
            savedStateHandle = savedStateHandle,
            getSmsStorageLowWarningActions = getActions,
            releaseSmsStorage = releaseStorage,
            cancelSmsStorageLowNotification = cancelNotification,
            mainDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun retentionDuration(): SmsStorageRetentionDuration {
        return SmsStorageRetentionDuration(
            count = 1,
            unit = SmsStorageRetentionDuration.DurationUnit.MONTH,
            millis = 30L * 24L * 60L * 60L * 1000L,
        )
    }
}
