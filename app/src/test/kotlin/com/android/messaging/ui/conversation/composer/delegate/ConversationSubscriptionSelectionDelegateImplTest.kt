package com.android.messaging.ui.conversation.composer.delegate

import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.satellite.model.SatelliteConnection
import com.android.messaging.data.satellite.repository.SatelliteConnectionsRepository
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationSubscriptionSelectionDelegateImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // A foreground scope: advanceUntilIdle() does not run work launched in backgroundScope.
    private val delegateScope = CoroutineScope(mainDispatcherRule.testDispatcher + Job())

    private val subscriptionsFlow = MutableStateFlow<ImmutableList<Subscription>>(
        persistentListOf(subscription(subId = FIRST_SUB_ID)),
    )
    private val defaultSmsSubIdFlow = MutableStateFlow(SubId(FIRST_SUB_ID))
    private val satelliteSubIdsFlow = MutableStateFlow<Set<SubId>>(emptySet())
    private val observedSubIds = mutableListOf<Set<SubId>>()

    private val subscriptionsRepository = mockk<SubscriptionsRepository> {
        every { getDefaultSmsSubscriptionId() } returns SubId(FIRST_SUB_ID)
        every { observeActiveSubscriptions() } returns subscriptionsFlow
        every { observeDefaultSmsSubscriptionId() } returns defaultSmsSubIdFlow
    }

    private val satelliteConnectionsRepository = object : SatelliteConnectionsRepository {
        override fun observeSatelliteConnections(
            subIds: Collection<SubId>,
        ): Flow<ImmutableMap<SubId, SatelliteConnection>> {
            val requested = subIds.toSet()
            observedSubIds += requested
            return satelliteSubIdsFlow.map { satelliteSubIds ->
                requested
                    .filter { subId -> subId in satelliteSubIds }
                    .associateWith { CONNECTION }
                    .toImmutableMap()
            }
        }
    }

    @After
    fun tearDown() {
        delegateScope.cancel()
    }

    @Test
    fun bind_addsTheSatelliteConnectionsOfTheActiveSubscriptions() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = createDelegate()
            delegate.bind(scope = delegateScope)
            advanceUntilIdle()

            assertEquals(
                persistentMapOf<SubId, SatelliteConnection>(),
                delegate.state.value.satelliteConnections,
            )

            satelliteSubIdsFlow.value = setOf(SubId(FIRST_SUB_ID))
            advanceUntilIdle()

            assertEquals(
                mapOf(SubId(FIRST_SUB_ID) to CONNECTION),
                delegate.state.value.satelliteConnections,
            )
            assertTrue(delegate.state.value.areSubscriptionsLoaded)
        }
    }

    @Test
    fun bind_observesANewlyInsertedSimForSatelliteConnections() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = createDelegate()
            delegate.bind(scope = delegateScope)
            satelliteSubIdsFlow.value = setOf(SubId(SECOND_SUB_ID))
            advanceUntilIdle()

            assertEquals(
                persistentMapOf<SubId, SatelliteConnection>(),
                delegate.state.value.satelliteConnections,
            )

            subscriptionsFlow.value = persistentListOf(
                subscription(subId = FIRST_SUB_ID),
                subscription(subId = SECOND_SUB_ID),
            )
            advanceUntilIdle()

            assertEquals(setOf(SubId(FIRST_SUB_ID), SubId(SECOND_SUB_ID)), observedSubIds.last())
            assertEquals(
                mapOf(SubId(SECOND_SUB_ID) to CONNECTION),
                delegate.state.value.satelliteConnections,
            )
        }
    }

    @Test
    fun bind_keepsListeningWhenTheSubscriptionsChangeWithoutChangingSims() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = createDelegate()
            delegate.bind(scope = delegateScope)
            advanceUntilIdle()

            subscriptionsFlow.value = persistentListOf(
                subscription(subId = FIRST_SUB_ID).copy(displayDestination = "+1 555 0100"),
            )
            advanceUntilIdle()

            assertEquals(listOf(setOf(SubId(FIRST_SUB_ID))), observedSubIds)
            assertEquals(
                "+1 555 0100",
                delegate.state.value.subscriptions.single().displayDestination,
            )
        }
    }

    private fun createDelegate(): ConversationSubscriptionSelectionDelegateImpl {
        return ConversationSubscriptionSelectionDelegateImpl(
            subscriptionsRepository = subscriptionsRepository,
            satelliteConnectionsRepository = satelliteConnectionsRepository,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun subscription(subId: Int): Subscription {
        return Subscription(
            selfParticipantId = ParticipantId("self-$subId"),
            subId = SubId(subId),
            label = ConversationSubscriptionLabel.Slot(slotId = subId),
            displayDestination = null,
            displaySlotId = subId,
            color = 0,
        )
    }

    private companion object {
        private const val FIRST_SUB_ID = 1
        private const val SECOND_SUB_ID = 2
        private val CONNECTION = SatelliteConnection(
            isSmsAvailable = true,
            isMmsAvailable = false,
        )
    }
}
