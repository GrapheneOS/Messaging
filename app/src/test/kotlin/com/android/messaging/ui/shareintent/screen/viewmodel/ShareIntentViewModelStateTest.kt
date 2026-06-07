package com.android.messaging.ui.shareintent.screen.viewmodel

import app.cash.turbine.test
import com.android.messaging.ui.shareintent.screen.model.ShareAttachmentUiModel
import com.android.messaging.ui.shareintent.screen.model.ShareDraftUiState
import com.android.messaging.ui.shareintent.screen.model.ShareTargetsUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ShareIntentViewModelStateTest : BaseShareIntentViewModelTest() {

    @Test
    fun init_bindsBothDelegates() {
        createViewModel()

        verify(exactly = 1) { targetsDelegate.bind(any()) }
        verify(exactly = 1) { draftDelegate.bind(any(), any()) }
    }

    @Test
    fun uiState_combinesTargetsAndDraftState() = runTest(mainDispatcherRule.testDispatcher) {
        targetsState.value = ShareTargetsUiState(isLoading = false)
        draftState.value = ShareDraftUiState(isLoading = false, text = "shared")

        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertTrue(awaitItem().isLoading)
            val state = awaitItem()
            assertEquals(targetsState.value, state.targets)
            assertEquals(draftState.value, state.draft)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_isLoading_whenTargetsLoading() = runTest(mainDispatcherRule.testDispatcher) {
        targetsState.value = ShareTargetsUiState(isLoading = true)
        draftState.value = ShareDraftUiState(isLoading = false)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertTrue(awaitItem().isLoading)
            assertTrue(awaitItem().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_isLoading_whenDraftLoading() = runTest(mainDispatcherRule.testDispatcher) {
        targetsState.value = ShareTargetsUiState(isLoading = false)
        draftState.value = ShareDraftUiState(isLoading = true)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertTrue(awaitItem().isLoading)
            assertTrue(awaitItem().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isSendEnabled_isTrue_whenDraftHasTextAndSelectionExists() =
        runTest(mainDispatcherRule.testDispatcher) {
            draftState.value = ShareDraftUiState(isLoading = false, text = "hi")
            setSelection(listOf(conversationTarget(conversationId = "1")))

            val viewModel = createViewModel()
            viewModel.uiState.test {
                assertFalse(awaitItem().isSendEnabled)
                assertTrue(awaitItem().isSendEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun isSendEnabled_isTrue_whenDraftHasOnlyAttachmentsAndSelectionExists() =
        runTest(mainDispatcherRule.testDispatcher) {
            draftState.value = ShareDraftUiState(
                isLoading = false,
                attachments = persistentListOf(
                    ShareAttachmentUiModel.Media(
                        id = "content://a",
                        contentType = "image/jpeg",
                        isVideo = false,
                    ),
                ),
            )
            setSelection(listOf(conversationTarget(conversationId = "1")))

            val viewModel = createViewModel()
            viewModel.uiState.test {
                assertFalse(awaitItem().isSendEnabled)
                assertTrue(awaitItem().isSendEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun isSendEnabled_isFalse_whenDraftBlank() = runTest(mainDispatcherRule.testDispatcher) {
        draftState.value = ShareDraftUiState(isLoading = false, text = "")
        setSelection(listOf(conversationTarget(conversationId = "1")))

        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertFalse(awaitItem().isSendEnabled)
            assertFalse(awaitItem().isSendEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isSendEnabled_isFalse_whenSelectionEmpty() = runTest(mainDispatcherRule.testDispatcher) {
        draftState.value = ShareDraftUiState(isLoading = false, text = "hi")

        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertFalse(awaitItem().isSendEnabled)
            assertFalse(awaitItem().isSendEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
