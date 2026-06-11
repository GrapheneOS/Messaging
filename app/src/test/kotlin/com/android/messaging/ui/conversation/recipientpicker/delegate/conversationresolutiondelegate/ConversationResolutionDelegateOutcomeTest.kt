package com.android.messaging.ui.conversation.recipientpicker.delegate.conversationresolutiondelegate

import app.cash.turbine.test
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionOutcome
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionState
import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationResolutionDelegateOutcomeTest :
    BaseConversationResolutionDelegateTest() {

    @Test
    fun resolve_whenUseCaseResolves_emitsResolvedOutcomeCarryingConversationId() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            coEvery {
                resolveConversationId(destinations = listOf("+15550100"))
            } returns ResolveConversationIdResult.Resolved(conversationId = "conversation-42")
            val delegate = createBoundDelegate()

            delegate.outcomes.test {
                delegate.resolve(destinations = listOf("+15550100"))
                runCurrent()

                assertEquals(
                    ConversationResolutionOutcome.Resolved(conversationId = "conversation-42"),
                    awaitItem(),
                )
                expectNoEvents()
            }
        }
    }

    @Test
    fun resolve_whenUseCaseReportsNotResolved_emitsFailedOutcome() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            coEvery {
                resolveConversationId(destinations = any())
            } returns ResolveConversationIdResult.NotResolved
            val delegate = createBoundDelegate()

            delegate.outcomes.test {
                delegate.resolve(destinations = listOf("+15550100"))
                runCurrent()

                assertEquals(ConversationResolutionOutcome.Failed, awaitItem())
                expectNoEvents()
            }
        }
    }

    @Test
    fun resolve_whenUseCaseReportsEmptyDestinations_emitsFailedOutcome() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            coEvery {
                resolveConversationId(destinations = any())
            } returns ResolveConversationIdResult.EmptyDestinations
            val delegate = createBoundDelegate()

            delegate.outcomes.test {
                delegate.resolve(destinations = listOf("   "))
                runCurrent()

                assertEquals(ConversationResolutionOutcome.Failed, awaitItem())
                expectNoEvents()
            }
        }
    }

    @Test
    fun resolve_whenUseCaseThrows_emitsFailedOutcomeAndReturnsToIdle() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            coEvery {
                resolveConversationId(destinations = any())
            } throws IllegalStateException("boom")
            val delegate = createBoundDelegate()

            delegate.outcomes.test {
                delegate.resolve(destinations = listOf("+15550100"))
                runCurrent()

                assertEquals(ConversationResolutionOutcome.Failed, awaitItem())
                expectNoEvents()
            }
            assertEquals(ConversationResolutionState.Idle, delegate.state.value)
        }
    }

    @Test
    fun resolve_forwardsDestinationsToUseCaseWithoutTrimmingOrFiltering() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val captured = slot<List<String>>()
            coEvery {
                resolveConversationId(destinations = capture(captured))
            } returns ResolveConversationIdResult.Resolved(conversationId = "conversation-42")
            val delegate = createBoundDelegate()

            delegate.resolve(destinations = listOf("  +15550100  ", "alice@example.com", ""))
            runCurrent()

            assertEquals(
                listOf("  +15550100  ", "alice@example.com", ""),
                captured.captured,
            )
        }
    }
}
