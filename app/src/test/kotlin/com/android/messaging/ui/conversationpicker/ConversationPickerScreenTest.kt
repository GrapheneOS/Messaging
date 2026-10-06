package com.android.messaging.ui.conversationpicker

import android.view.inputmethod.EditorInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.android.messaging.testutil.messageFieldView
import com.android.messaging.ui.common.components.composer.MESSAGE_COMPOSE_FIELD_TEST_TAG
import com.android.messaging.ui.conversationpicker.model.ConversationPickerAction as Action
import com.android.messaging.ui.conversationpicker.model.ConversationPickerEffect as Effect
import com.android.messaging.ui.conversationpicker.model.ConversationPickerLabels
import com.android.messaging.ui.conversationpicker.model.ConversationPickerUiState as State
import com.android.messaging.ui.conversationpicker.model.DraftUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationPickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun keyboardSendAction_sendsTheReviewedDraft() {
        val screenModel = setReviewContent(isSendEnabled = true)

        composeTestRule
            .messageFieldView(testTag = MESSAGE_COMPOSE_FIELD_TEST_TAG)
            .onEditorAction(EditorInfo.IME_ACTION_SEND)

        assertEquals(1, screenModel.actions.count { it == Action.SendClicked })
    }

    @Test
    fun keyboardSendAction_doesNothingWhileTheSendButtonIsDisabled() {
        val screenModel = setReviewContent(isSendEnabled = false)

        composeTestRule
            .messageFieldView(testTag = MESSAGE_COMPOSE_FIELD_TEST_TAG)
            .onEditorAction(EditorInfo.IME_ACTION_SEND)

        assertEquals(0, screenModel.actions.count { it == Action.SendClicked })
    }

    private fun setReviewContent(isSendEnabled: Boolean): RecordingScreenModel {
        val screenModel = RecordingScreenModel(
            uiState = State(
                draft = DraftUiState(
                    isLoading = false,
                    isReviewing = true,
                    text = "Hello",
                ),
                isSendEnabled = isSendEnabled,
            ),
        )
        composeTestRule.setContent {
            AppTheme {
                ConversationPickerScreen(
                    screenModel = screenModel,
                    isInitialDraftLoading = true,
                    initialDraft = null,
                    effectHandler = object : ConversationPickerEffectHandler {
                        override fun handle(effect: Effect) {}
                    },
                    onNavigateBack = {},
                    allowMultiSelect = true,
                    labels = ConversationPickerLabels.Share,
                )
            }
        }
        return screenModel
    }

    private class RecordingScreenModel(
        uiState: State,
    ) : ConversationPickerScreenModel {
        val actions = mutableListOf<Action>()

        override val effects: Flow<Effect> = emptyFlow()
        override val uiState: StateFlow<State> = MutableStateFlow(uiState)

        override fun onAction(action: Action) {
            actions += action
        }
    }
}
