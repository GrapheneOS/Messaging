package com.android.messaging.ui.conversation.screen

import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenSelectionBackTest : BaseConversationScreenTest() {

    @Test
    fun systemBackInSelectionMode_dismissesMessageSelection() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-2"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Delete,
                ),
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule.runOnIdle {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.dismissMessageSelection()
            }
        }
    }
}
