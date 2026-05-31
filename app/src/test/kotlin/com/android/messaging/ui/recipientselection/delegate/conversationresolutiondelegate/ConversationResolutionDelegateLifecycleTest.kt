package com.android.messaging.ui.recipientselection.delegate.conversationresolutiondelegate

import app.cash.turbine.test
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.ui.recipientselection.model.picker.ConversationResolutionOutcome
import com.android.messaging.ui.recipientselection.model.picker.ConversationResolutionState
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationResolutionDelegateLifecycleTest :
    BaseConversationResolutionDelegateTest() {

    @Test
    fun resolve_beforeBind_isInertAndNeverInvokesUseCase() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = createDelegate()

            delegate.outcomes.test {
                delegate.resolve(destinations = listOf("+15550100"))
                runCurrent()

                expectNoEvents()
            }
            assertEquals(ConversationResolutionState.Idle, delegate.state.value)
            coVerify(exactly = 0) {
                resolveConversationId(destinations = any())
            }
        }
    }

    @Test
    fun bind_isIdempotentAndRetainsTheFirstScope() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = createDelegate()
            delegate.bind(scope = backgroundScope)
            val replacementScope = CoroutineScope(context = mainDispatcherRule.testDispatcher)
            replacementScope.cancel()

            delegate.bind(scope = replacementScope)

            delegate.outcomes.test {
                delegate.resolve(destinations = listOf("+15550100"))
                runCurrent()

                assertEquals(
                    ConversationResolutionOutcome.Resolved(
                        conversationId = DEFAULT_CONVERSATION_ID,
                    ),
                    awaitItem(),
                )
            }
        }
    }

    @Test
    fun cancel_whileResolving_returnsToIdleWithoutEmittingAnOutcome() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenResolutionSuspendsUntil(gate = CompletableDeferred())
            val delegate = createBoundDelegate()

            delegate.outcomes.test {
                delegate.resolve(
                    destinations = listOf("+15550100"),
                    recipientDestination = "+1 555 0100",
                )
                runCurrent()
                assertEquals(
                    ConversationResolutionState.Resolving(
                        recipientDestination = "+1 555 0100",
                        isIndicatorVisible = false,
                    ),
                    delegate.state.value,
                )

                delegate.cancel()
                runCurrent()

                assertEquals(ConversationResolutionState.Idle, delegate.state.value)
                expectNoEvents()
            }
        }
    }

    @Test
    fun cancel_whenNothingIsResolving_isANoOp() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = createBoundDelegate()

            delegate.outcomes.test {
                delegate.cancel()
                runCurrent()

                assertEquals(ConversationResolutionState.Idle, delegate.state.value)
                expectNoEvents()
            }
            coVerify(exactly = 0) {
                resolveConversationId(destinations = any())
            }
        }
    }

    @Test
    fun cancel_afterAReentrantResolve_stillCancelsTheLatestResolveAndEmitsNoOutcome() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val firstGate = CompletableDeferred<ResolveConversationIdResult>()
            val secondGate = CompletableDeferred<ResolveConversationIdResult>()
            coEvery {
                resolveConversationId(destinations = listOf("first"))
            } coAnswers { firstGate.await() }
            coEvery {
                resolveConversationId(destinations = listOf("second"))
            } coAnswers { secondGate.await() }
            val delegate = createBoundDelegate()

            delegate.outcomes.test {
                delegate.resolve(destinations = listOf("first"), recipientDestination = "first")
                runCurrent()
                delegate.resolve(destinations = listOf("second"), recipientDestination = "second")
                runCurrent()

                delegate.cancel()
                runCurrent()
                assertEquals(ConversationResolutionState.Idle, delegate.state.value)

                secondGate.complete(
                    ResolveConversationIdResult.Resolved(conversationId = "conversation-second"),
                )
                runCurrent()

                expectNoEvents()
            }
        }
    }

    @Test
    fun resolve_whileAnEarlierResolveIsInFlight_cancelsItAndEmitsOnlyTheLatestOutcome() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val firstGate = CompletableDeferred<ResolveConversationIdResult>()
            val secondGate = CompletableDeferred<ResolveConversationIdResult>()
            coEvery {
                resolveConversationId(destinations = listOf("first"))
            } coAnswers { firstGate.await() }
            coEvery {
                resolveConversationId(destinations = listOf("second"))
            } coAnswers { secondGate.await() }
            val delegate = createBoundDelegate()

            delegate.outcomes.test {
                delegate.resolve(destinations = listOf("first"), recipientDestination = "first")
                runCurrent()
                delegate.resolve(destinations = listOf("second"), recipientDestination = "second")
                runCurrent()

                assertEquals(
                    ConversationResolutionState.Resolving(
                        recipientDestination = "second",
                        isIndicatorVisible = false,
                    ),
                    delegate.state.value,
                )

                secondGate.complete(
                    ResolveConversationIdResult.Resolved(conversationId = "conversation-second"),
                )
                runCurrent()

                assertEquals(
                    ConversationResolutionOutcome.Resolved(
                        conversationId = "conversation-second",
                    ),
                    awaitItem(),
                )
                assertEquals(ConversationResolutionState.Idle, delegate.state.value)
                expectNoEvents()
            }
        }
    }
}
