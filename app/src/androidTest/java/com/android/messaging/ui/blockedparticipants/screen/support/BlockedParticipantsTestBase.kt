package com.android.messaging.ui.blockedparticipants.screen.support

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsEffectHandler
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsScreen
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsScreenModel
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsNavEvent
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsScreenEffect
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule

internal abstract class BlockedParticipantsTestBase {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    protected val uiStateFlow = MutableStateFlow(loadedState())
    protected val effectsFlow = MutableSharedFlow<BlockedParticipantsScreenEffect>(
        extraBufferCapacity = 1,
    )
    protected val navEventsFlow = MutableSharedFlow<BlockedParticipantsNavEvent>(
        extraBufferCapacity = 1,
    )
    protected val onNavigateBackCalls = mutableListOf<Unit>()

    protected lateinit var screenModel: BlockedParticipantsScreenModel
    protected lateinit var effectHandler: BlockedParticipantsEffectHandler

    @Before
    fun setUpScreenModel() {
        screenModel = mockk(relaxed = true)
        effectHandler = mockk(relaxed = true)
        every { screenModel.uiState } returns uiStateFlow
        every { screenModel.effects } returns effectsFlow
        every { screenModel.navigationEvents } returns navEventsFlow
    }

    protected fun renderScreen(state: BlockedParticipantsUiState = uiStateFlow.value) {
        uiStateFlow.value = state
        composeTestRule.setContent {
            AppTheme {
                BlockedParticipantsScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = { onNavigateBackCalls += Unit },
                    screenModel = screenModel,
                )
            }
        }
    }

    protected fun updateState(state: BlockedParticipantsUiState) {
        composeTestRule.runOnIdle { uiStateFlow.value = state }
        composeTestRule.waitForIdle()
    }

    protected fun emitNavEvent(event: BlockedParticipantsNavEvent) {
        composeTestRule.runOnIdle { navEventsFlow.tryEmit(event) }
        composeTestRule.waitForIdle()
    }

    protected fun string(resId: Int, vararg formatArgs: Any): String {
        return composeTestRule.activity.getString(resId, *formatArgs)
    }

    protected fun quantityString(resId: Int, quantity: Int, vararg formatArgs: Any): String {
        return composeTestRule.activity.resources.getQuantityString(
            resId,
            quantity,
            *formatArgs,
        )
    }
}
