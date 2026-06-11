package com.android.messaging.ui.conversationpicker.viewmodel

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.conversationpicker.model.SendTarget
import com.android.messaging.testutil.TEST_RESOLVED_CONVERSATION_ID
import com.android.messaging.testutil.contactTarget
import com.android.messaging.testutil.conversationTarget
import com.android.messaging.ui.conversationpicker.model.ConversationPickerAction as Action
import com.android.messaging.ui.conversationpicker.model.ConversationPickerEffect as Effect
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationPickerViewModelEffectTest : BaseConversationPickerViewModelTest() {

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
            givenResolvedConversation()

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(
                    Action.TargetClicked(
                        contactTarget(),
                    ),
                )
                assertEquals(
                    Effect.OpenConversation(TEST_RESOLVED_CONVERSATION_ID),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun targetClicked_contact_whenNotResolved_emitsNoEffect() =
        runTest(mainDispatcherRule.testDispatcher) {
            givenUnresolvedConversation()

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(
                    Action.TargetClicked(
                        contactTarget(),
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
                        SendTarget.Conversation("1"),
                        SendTarget.Contact("+15550002"),
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
