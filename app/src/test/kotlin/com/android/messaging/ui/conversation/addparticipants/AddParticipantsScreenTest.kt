package com.android.messaging.ui.conversation.addparticipants

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsEffect
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsUiState
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddParticipantsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_withConversationId_notifiesModel() {
        val model = createScreenModel()

        setContent(model = model)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                model.onConversationIdChanged(conversationId = CONVERSATION_ID)
            }
        }
    }

    @Test
    fun confirmButton_withSelectedRecipient_forwardsClick() {
        val model = createScreenModel(
            initialUiState = AddParticipantsUiState(
                isLoadingConversationParticipants = false,
                selectedRecipients = persistentListOf(
                    selectedRecipient(destination = "+15550100"),
                ),
            ),
        )

        setContent(model = model)

        composeTestRule
            .onNodeWithTag(ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                model.onConfirmClick()
            }
        }
    }

    @Test
    fun title_defaultState_isRendered() {
        setContent(model = createScreenModel())

        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.conversation_add_people))
            .assertIsDisplayed()
    }

    @Test
    fun navigationIcon_forwardsBackClick() {
        val model = createScreenModel()
        val onNavigateBack = mockk<() -> Unit>(relaxed = true)

        setContent(
            model = model,
            onNavigateBack = onNavigateBack,
        )

        composeTestRule
            .onNodeWithContentDescription(targetContext.getString(R.string.back))
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onNavigateBack.invoke()
            }
        }
    }

    @Test
    fun navigateEffect_forwardsConversationId() {
        val effectsFlow = MutableSharedFlow<AddParticipantsEffect>(extraBufferCapacity = 1)
        val model = createScreenModel(effectsFlow = effectsFlow)
        val onNavigateToConversation = mockk<(String) -> Unit>(relaxed = true)

        setContent(
            model = model,
            onNavigateToConversation = onNavigateToConversation,
        )
        waitForEffectsCollector(effectsFlow = effectsFlow)

        composeTestRule.runOnIdle {
            effectsFlow.tryEmit(
                AddParticipantsEffect.NavigateToConversation(
                    conversationId = TARGET_CONVERSATION_ID,
                ),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onNavigateToConversation.invoke(TARGET_CONVERSATION_ID)
            }
        }
    }

    @Test
    fun selectedRecipientWhileResolving_disablesConfirm() {
        val model = createScreenModel(
            initialUiState = AddParticipantsUiState(
                isLoadingConversationParticipants = false,
                isResolvingConversation = true,
                selectedRecipients = persistentListOf(
                    selectedRecipient(destination = "+15550100"),
                ),
            ),
        )

        setContent(model = model)

        composeTestRule
            .onNodeWithTag(testTag = ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun selectedRecipient_usesAddMoreQueryHint() {
        val model = createScreenModel(
            initialUiState = AddParticipantsUiState(
                isLoadingConversationParticipants = false,
                selectedRecipients = persistentListOf(
                    selectedRecipient(destination = "+15550100"),
                ),
            ),
        )

        setContent(model = model)

        composeTestRule
            .onNodeWithText(
                text = targetContext.getString(R.string.recipient_selection_query_hint_more),
            )
            .assertIsDisplayed()
    }

    private fun setContent(
        model: AddParticipantsScreenModel,
        onNavigateBack: () -> Unit = {},
        onNavigateToConversation: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                AddParticipantsScreen(
                    conversationId = CONVERSATION_ID,
                    onNavigateBack = onNavigateBack,
                    onNavigateToConversation = onNavigateToConversation,
                    screenModel = model,
                )
            }
        }
    }

    private fun selectedRecipient(destination: String): SelectedRecipient {
        return SelectedRecipient(
            destination = destination,
            label = destination,
            displayDestination = destination,
            photoUri = null,
        )
    }

    private fun createScreenModel(
        effectsFlow: MutableSharedFlow<AddParticipantsEffect> = MutableSharedFlow(),
    ): AddParticipantsScreenModel {
        return createScreenModel(
            initialUiState = defaultUiState,
            effectsFlow = effectsFlow,
        )
    }

    private fun createScreenModel(
        initialUiState: AddParticipantsUiState,
        effectsFlow: MutableSharedFlow<AddParticipantsEffect> = MutableSharedFlow(),
    ): AddParticipantsScreenModel {
        return mockk<AddParticipantsScreenModel>(relaxed = true) {
            every { effects } returns effectsFlow
            every { uiState } returns MutableStateFlow(value = initialUiState)
        }
    }

    private fun waitForEffectsCollector(effectsFlow: MutableSharedFlow<AddParticipantsEffect>) {
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            effectsFlow.subscriptionCount.value == 1
        }
    }

    private companion object {
        private const val TARGET_CONVERSATION_ID = "conversation-target"

        private val defaultUiState = AddParticipantsUiState(
            isLoadingConversationParticipants = false,
        )
    }
}
