package com.android.messaging.ui.conversationsettings.screen.support

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsEffectHandler
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsScreen
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsScreenModel
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsNavEvent
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsScreenEffect
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule

internal abstract class ConversationSettingsTestBase {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    protected val uiStateFlow = MutableStateFlow(oneToOneState())
    protected val effectsFlow = MutableSharedFlow<ConversationSettingsScreenEffect>(
        extraBufferCapacity = 1,
    )
    protected val navEventsFlow = MutableSharedFlow<ConversationSettingsNavEvent>(
        extraBufferCapacity = 1,
    )
    protected val onNavigateBackCalls = mutableListOf<Int?>()

    protected lateinit var screenModel: ConversationSettingsScreenModel
    protected lateinit var effectHandler: ConversationSettingsEffectHandler

    @Before
    fun setUpScreenModel() {
        screenModel = mockk(relaxed = true)
        effectHandler = mockk(relaxed = true)
        every { screenModel.uiState } returns uiStateFlow
        every { screenModel.effects } returns effectsFlow
        every { screenModel.navigationEvents } returns navEventsFlow
        every { screenModel.rootConversationId } returns ROOT_CONVERSATION_ID
    }

    protected fun renderScreen(state: ConversationSettingsUiState = uiStateFlow.value) {
        uiStateFlow.value = state
        composeTestRule.setContent {
            AppTheme {
                ConversationSettingsScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = onNavigateBackCalls::add,
                    screenModel = screenModel,
                )
            }
        }
    }

    protected fun emitNavEvent(event: ConversationSettingsNavEvent) {
        composeTestRule.runOnIdle { navEventsFlow.tryEmit(event) }
        composeTestRule.waitForIdle()
    }

    protected fun string(resId: Int, vararg formatArgs: Any): String {
        return composeTestRule.activity.getString(resId, *formatArgs)
    }
}
