package com.android.messaging.ui.conversation.entry.newchat

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.data.ParticipantData
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class NewChatViewModelSimSelectionTest : BaseNewChatViewModelTest() {

    @Test
    fun activeSubscriptions_withoutMatchingDefault_selectsFirstSubscription() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val first = subscription(selfParticipantId = "self-1", subId = 11, slotId = 1)
            val second = subscription(selfParticipantId = "self-2", subId = 12, slotId = 2)
            val repository = createSubscriptionsRepositoryMock(
                subscriptions = persistentListOf(first, second),
                defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
            )
            val viewModel = createViewModel(subscriptionsRepository = repository)

            viewModel.uiState.test {
                advanceUntilIdle()
                val simState = expectMostRecentItem().simSelectorState
                assertEquals(persistentListOf(first, second), simState.subscriptions)
                assertEquals(first, simState.selectedSubscription)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun activeSubscriptions_withMatchingDefaultSubId_selectsThatSubscription() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val first = subscription(selfParticipantId = "self-1", subId = 11, slotId = 1)
            val second = subscription(selfParticipantId = "self-2", subId = 12, slotId = 2)
            val repository = createSubscriptionsRepositoryMock(
                subscriptions = persistentListOf(first, second),
                defaultSmsSubscriptionId = 12,
            )
            val viewModel = createViewModel(subscriptionsRepository = repository)

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(second, expectMostRecentItem().simSelectorState.selectedSubscription)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun activeSubscriptions_withUnmatchedDefaultSubId_fallsBackToFirstSubscription() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val first = subscription(selfParticipantId = "self-1", subId = 11, slotId = 1)
            val second = subscription(selfParticipantId = "self-2", subId = 12, slotId = 2)
            val repository = createSubscriptionsRepositoryMock(
                subscriptions = persistentListOf(first, second),
                defaultSmsSubscriptionId = 99,
            )
            val viewModel = createViewModel(subscriptionsRepository = repository)

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(first, expectMostRecentItem().simSelectorState.selectedSubscription)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun activeSubscriptions_whenEmpty_selectsNoSubscription() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repository = createSubscriptionsRepositoryMock(subscriptions = persistentListOf())
            val viewModel = createViewModel(subscriptionsRepository = repository)

            viewModel.uiState.test {
                advanceUntilIdle()
                val simState = expectMostRecentItem().simSelectorState
                assertEquals(persistentListOf<Subscription>(), simState.subscriptions)
                assertNull(simState.selectedSubscription)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onSimSelected_withKnownSelfParticipantId_updatesSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val first = subscription(selfParticipantId = "self-1", subId = 11, slotId = 1)
            val second = subscription(selfParticipantId = "self-2", subId = 12, slotId = 2)
            val repository = createSubscriptionsRepositoryMock(
                subscriptions = persistentListOf(first, second),
            )
            val viewModel = createViewModel(subscriptionsRepository = repository)

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(first, expectMostRecentItem().simSelectorState.selectedSubscription)

                viewModel.onSimSelected(selfParticipantId = "self-2")
                advanceUntilIdle()
                assertEquals(second, expectMostRecentItem().simSelectorState.selectedSubscription)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onSimSelected_withUnknownSelfParticipantId_keepsCurrentSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val first = subscription(selfParticipantId = "self-1", subId = 11, slotId = 1)
            val second = subscription(selfParticipantId = "self-2", subId = 12, slotId = 2)
            val repository = createSubscriptionsRepositoryMock(
                subscriptions = persistentListOf(first, second),
            )
            val viewModel = createViewModel(subscriptionsRepository = repository)

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(first, expectMostRecentItem().simSelectorState.selectedSubscription)

                viewModel.onSimSelected(selfParticipantId = "unknown-self")
                advanceUntilIdle()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(first, viewModel.uiState.value.simSelectorState.selectedSubscription)
        }
    }

    @Test
    fun simSelection_isPersistedAndRestoredOverridingDefaultOnRecreation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val first = subscription(selfParticipantId = "self-1", subId = 11, slotId = 1)
            val second = subscription(selfParticipantId = "self-2", subId = 12, slotId = 2)
            val savedStateHandle = SavedStateHandle()

            val firstViewModel = createViewModel(
                subscriptionsRepository = createSubscriptionsRepositoryMock(
                    subscriptions = persistentListOf(first, second),
                ),
                savedStateHandle = savedStateHandle,
            )
            advanceUntilIdle()
            firstViewModel.onSimSelected(selfParticipantId = "self-2")
            advanceUntilIdle()

            val recreatedViewModel = createViewModel(
                subscriptionsRepository = createSubscriptionsRepositoryMock(
                    subscriptions = persistentListOf(first, second),
                    defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
                ),
                savedStateHandle = savedStateHandle,
            )

            recreatedViewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(second, expectMostRecentItem().simSelectorState.selectedSubscription)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
    @Test
    fun simSelection_withStalePersistedSelection_fallsBackToDefaultSmsSubscription() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val first = subscription(selfParticipantId = "self-1", subId = 11, slotId = 1)
            val second = subscription(selfParticipantId = "self-2", subId = 12, slotId = 2)
            val repository = createSubscriptionsRepositoryMock(
                subscriptions = persistentListOf(first, second),
                defaultSmsSubscriptionId = 12,
            )
            val viewModel = createViewModel(
                subscriptionsRepository = repository,
                savedStateHandle = SavedStateHandle(
                    mapOf("sim_selected_self_participant_id" to "self-gone"),
                ),
            )

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(second, expectMostRecentItem().simSelectorState.selectedSubscription)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
