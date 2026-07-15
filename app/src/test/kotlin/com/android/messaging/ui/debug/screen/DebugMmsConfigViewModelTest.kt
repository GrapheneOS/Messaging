package com.android.messaging.ui.debug.screen

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.data.debugmmsconfig.model.DebugSim
import com.android.messaging.data.debugmmsconfig.model.MmsConfigKeyType
import com.android.messaging.data.debugmmsconfig.repository.MmsConfigRepository
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.debug.screen.mapper.DebugMmsConfigUiStateMapper
import com.android.messaging.ui.debug.screen.model.DebugMmsConfigAction as Action
import com.android.messaging.ui.debug.screen.model.DebugMmsConfigUiState as State
import com.android.messaging.ui.debug.screen.model.DebugSimUiState
import com.android.messaging.ui.debug.screen.model.MmsConfigItemUiState
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class DebugMmsConfigViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<MmsConfigRepository>()
    private val mapper = mockk<DebugMmsConfigUiStateMapper>()

    @Test
    fun uiState_selectsFirstSimAndMapsEntries() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            stubLoadedEntries()

            val viewModel = createViewModel()
            viewModel.uiState.test {
                assertEquals(State(), awaitItem())

                val state = awaitItem()
                assertEquals(SIM_ITEMS, state.sims)
                assertEquals(1, state.selectedSubId?.value)
                assertEquals(ITEMS, state.items)
                assertEquals(false, state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun uiState_noSims_isNotLoadingWithoutEntries() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            stubSims(
                sims = persistentListOf(),
                simItems = persistentListOf(),
            )

            val viewModel = createViewModel()
            viewModel.uiState.test {
                assertEquals(State(), awaitItem())

                val state = awaitItem()
                assertEquals(false, state.isLoading)
                assertEquals(null, state.selectedSubId)
                assertEquals(persistentListOf<MmsConfigItemUiState>(), state.items)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 0) { repository.getEntries(any()) }
        }
    }

    @Test
    fun simSelected_switchesSelectionAndLoadsEntries() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            stubLoadedEntries()

            val viewModel = createViewModel()
            viewModel.uiState.test {
                skipItems(2)

                viewModel.onAction(Action.SimSelected(SubId(2)))

                assertEquals(2, awaitItem().selectedSubId?.value)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { repository.getEntries(SubId(2)) }
        }
    }

    @Test
    fun entryToggled_updatesEntryAsBool() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            stubEntryUpdate()

            val viewModel = createLoadedViewModel()
            viewModel.onAction(
                Action.EntryToggled(
                    key = "enabledMMS",
                    checked = true,
                ),
            )
            advanceUntilIdle()

            coVerify {
                repository.updateEntry(
                    subId = SubId(1),
                    key = "enabledMMS",
                    keyType = MmsConfigKeyType.BOOL,
                    value = "true",
                )
            }
        }
    }

    @Test
    fun entryValueSubmitted_numeric_updatesAsInt() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            stubEntryUpdate()

            val viewModel = createLoadedViewModel()
            viewModel.onAction(
                Action.EntryValueSubmitted(
                    key = "maxMessageSize",
                    value = "2048",
                    isNumeric = true,
                ),
            )
            advanceUntilIdle()

            coVerify {
                repository.updateEntry(
                    subId = SubId(1),
                    key = "maxMessageSize",
                    keyType = MmsConfigKeyType.INT,
                    value = "2048",
                )
            }
        }
    }

    @Test
    fun entryValueSubmitted_nonNumeric_updatesAsString() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            stubEntryUpdate()

            val viewModel = createLoadedViewModel()
            viewModel.onAction(
                Action.EntryValueSubmitted(
                    key = "userAgent",
                    value = "Bugle",
                    isNumeric = false,
                ),
            )
            advanceUntilIdle()

            coVerify {
                repository.updateEntry(
                    subId = SubId(1),
                    key = "userAgent",
                    keyType = MmsConfigKeyType.STRING,
                    value = "Bugle",
                )
            }
        }
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): DebugMmsConfigViewModel {
        return DebugMmsConfigViewModel(
            repository = repository,
            mapper = mapper,
            savedStateHandle = savedStateHandle,
        )
    }

    private suspend fun createLoadedViewModel(): DebugMmsConfigViewModel {
        val viewModel = createViewModel()
        viewModel.uiState.first { state -> state.selectedSubId != null }
        return viewModel
    }

    private fun stubLoadedEntries() {
        stubSims(
            sims = SIMS,
            simItems = SIM_ITEMS,
        )
        coEvery { repository.getEntries(any()) } returns persistentListOf()
        every { mapper.toItems(any()) } returns ITEMS
    }

    private fun stubEntryUpdate() {
        stubLoadedEntries()
        coEvery { repository.updateEntry(any(), any(), any(), any()) } just Runs
    }

    private fun stubSims(
        sims: ImmutableList<DebugSim>,
        simItems: ImmutableList<DebugSimUiState>,
    ) {
        coEvery { repository.getActiveSims() } returns sims
        every { mapper.toSims(any()) } returns simItems
    }

    private companion object {
        private val SIMS = persistentListOf(
            DebugSim(subId = SubId(1), mcc = 310, mnc = 260),
            DebugSim(subId = SubId(2), mcc = 208, mnc = 1),
        )

        private val SIM_ITEMS = persistentListOf(
            DebugSimUiState(subId = SubId(1), mccMnc = "(310/260)"),
            DebugSimUiState(subId = SubId(2), mccMnc = "(208/1)"),
        )

        private val ITEMS = persistentListOf<MmsConfigItemUiState>(
            MmsConfigItemUiState.Editable(
                key = "userAgent",
                value = "Bugle",
                isNumeric = false,
            ),
        )
    }
}
