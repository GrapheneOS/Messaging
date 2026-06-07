package com.android.messaging.ui.shareintent.screen.viewmodel

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.shareintent.model.ShareSendTarget
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ShareIntentViewModelEffectTest : BaseShareIntentViewModelTest() {

    @Test
    fun targetClicked_conversation_emitsOpenConversation() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onAction(
                    Action.TargetClicked(
                        conversationTarget(conversationId = "42"),
                    ),
                )
                assertEquals(Effect.OpenConversation("42"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun targetClicked_contact_whenResolved_emitsOpenConversation() =
        runTest(mainDispatcherRule.testDispatcher) {
            givenResolvedConversation(
                destination = "+15550001",
                conversationId = "99",
            )

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(
                    Action.TargetClicked(
                        contactTarget(contactId = 1L, destination = "+15550001"),
                    ),
                )
                assertEquals(Effect.OpenConversation("99"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun targetClicked_contact_whenNotResolved_emitsNoEffect() =
        runTest(mainDispatcherRule.testDispatcher) {
            givenUnresolvedConversation(destination = "+15550001")

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(
                    Action.TargetClicked(
                        contactTarget(contactId = 1L, destination = "+15550001"),
                    ),
                )
                expectNoEvents()
            }
        }

    @Test
    fun sendClicked_emitsSendToSelectedWithMappedTargetsAndDraft() =
        runTest(mainDispatcherRule.testDispatcher) {
            val draft = ConversationDraft(messageText = "shared")
            every { draftDelegate.currentDraft() } returns draft

            givenSelectedTargets(
                listOf(
                    conversationTarget(conversationId = "1"),
                    contactTarget(contactId = 2L, destination = "+15550002"),
                ),
            )

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(Action.SendClicked)

                val effect = awaitItem() as Effect.SendToSelected
                assertEquals(
                    setOf(
                        ShareSendTarget.Conversation("1"),
                        ShareSendTarget.Contact("+15550002"),
                    ),
                    effect.targets,
                )
                assertEquals(draft, effect.draft)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun sendClicked_calledTwice_emitsEffectEachTime() =
        runTest(mainDispatcherRule.testDispatcher) {
            givenSelectedTargets(
                listOf(
                    conversationTarget(conversationId = "1"),
                ),
            )

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(Action.SendClicked)
                awaitItem()
                viewModel.onAction(Action.SendClicked)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
