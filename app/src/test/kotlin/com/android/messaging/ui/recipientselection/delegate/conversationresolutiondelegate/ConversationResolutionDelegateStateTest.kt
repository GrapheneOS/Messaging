package com.android.messaging.ui.recipientselection.delegate.conversationresolutiondelegate

import app.cash.turbine.test
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.ui.recipientselection.model.picker.ConversationResolutionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationResolutionDelegateStateTest : BaseConversationResolutionDelegateTest() {

    @Test
    fun resolve_fromIdle_emitsResolvingWithHiddenIndicatorAndRecipientDestination() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenResolutionSuspendsUntil(gate = CompletableDeferred())
            val delegate = createBoundDelegate()

            delegate.state.test {
                assertEquals(ConversationResolutionState.Idle, awaitItem())

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
                    awaitItem(),
                )
            }
        }
    }

    @Test
    fun resolve_withoutRecipientDestination_carriesNullIntoResolvingState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenResolutionSuspendsUntil(gate = CompletableDeferred())
            val delegate = createBoundDelegate()

            delegate.resolve(destinations = listOf("+15550100"))
            runCurrent()

            assertEquals(
                ConversationResolutionState.Resolving(
                    recipientDestination = null,
                    isIndicatorVisible = false,
                ),
                delegate.state.value,
            )
        }
    }

    @Test
    fun resolve_keepsIndicatorHiddenUntilDelayElapsesThenRevealsItWhileStillResolving() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenResolutionSuspendsUntil(gate = CompletableDeferred())
            val delegate = createBoundDelegate()
            delegate.resolve(
                destinations = listOf("+15550100"),
                recipientDestination = "+1 555 0100",
            )
            runCurrent()

            advanceTimeBy(INDICATOR_DELAY_MILLIS - 1)
            assertEquals(
                ConversationResolutionState.Resolving(
                    recipientDestination = "+1 555 0100",
                    isIndicatorVisible = false,
                ),
                delegate.state.value,
            )

            advanceTimeBy(2)
            assertEquals(
                ConversationResolutionState.Resolving(
                    recipientDestination = "+1 555 0100",
                    isIndicatorVisible = true,
                ),
                delegate.state.value,
            )
        }
    }

    @Test
    fun resolve_completingBeforeIndicatorDelay_neverRevealsIndicator() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<ResolveConversationIdResult>()
            givenResolutionSuspendsUntil(gate = gate)
            val delegate = createBoundDelegate()

            delegate.state.test {
                assertEquals(ConversationResolutionState.Idle, awaitItem())

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
                    awaitItem(),
                )

                gate.complete(
                    ResolveConversationIdResult.Resolved(conversationId = "conversation-42"),
                )
                runCurrent()

                assertEquals(ConversationResolutionState.Idle, awaitItem())
                expectNoEvents()
            }
        }
    }

    @Test
    fun resolve_whenResolutionCompletes_returnsToIdle() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<ResolveConversationIdResult>()
            givenResolutionSuspendsUntil(gate = gate)
            val delegate = createBoundDelegate()
            delegate.resolve(destinations = listOf("+15550100"))
            runCurrent()

            gate.complete(
                ResolveConversationIdResult.Resolved(conversationId = "conversation-42"),
            )
            runCurrent()

            assertEquals(ConversationResolutionState.Idle, delegate.state.value)
        }
    }

    private companion object {
        const val INDICATOR_DELAY_MILLIS = 200L
    }
}
