package com.android.messaging.ui.conversation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.screen.model.ConversationScreenNavEvent as NavEvent
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.core.CollectEvents
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationScreenEffectsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun changingCapturedHandlerInputsDoesNotRestartEffectsCollector() {
        val subscriptionCount = AtomicInteger()
        val cancellationCount = AtomicInteger()
        val screenModel = mockConversationScreenModel(
            effectsFlow = callbackFlow {
                subscriptionCount.incrementAndGet()
                awaitClose {
                    cancellationCount.incrementAndGet()
                }
            },
        )
        val inputGeneration = mutableStateOf(value = 0)

        composeTestRule.setContent {
            val snackbarHostState = remember(inputGeneration.value) {
                SnackbarHostState()
            }
            val hostBoundsState = remember(inputGeneration.value) {
                mutableStateOf<ComposeRect?>(value = null)
            }

            ConversationScreenEffects(
                screenModel = screenModel,
                snackbarHostState = snackbarHostState,
                hostBoundsState = hostBoundsState,
                onNavigateToVCardDetail = {},
                onNavigateToPhotoViewer = {},
            )
        }

        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            subscriptionCount.get() == 1
        }

        repeat(CAPTURED_INPUT_CHANGE_COUNT) { changeIndex ->
            composeTestRule.runOnIdle {
                inputGeneration.value = changeIndex + 1
            }
            composeTestRule.waitForIdle()
        }

        composeTestRule.runOnIdle {
            assertEquals(1, subscriptionCount.get())
            assertEquals(0, cancellationCount.get())
        }
    }

    @Test
    fun collectedNavigationEventUsesLatestCapturedNavigateBackCallback() {
        val navEventsFlow = MutableSharedFlow<NavEvent>(extraBufferCapacity = 1)
        val screenModel = mockConversationScreenModel(navEventsFlow = navEventsFlow)
        val staleNavigateBackCount = AtomicInteger()
        val currentNavigateBackCount = AtomicInteger()
        val inputGeneration = mutableStateOf(value = 0)

        composeTestRule.setContent {
            val onNavigateBack: () -> Unit = remember(inputGeneration.value) {
                when (inputGeneration.value) {
                    0 -> {
                        {
                            staleNavigateBackCount.incrementAndGet()
                        }
                    }

                    else -> {
                        {
                            currentNavigateBackCount.incrementAndGet()
                        }
                    }
                }
            }

            CollectEvents(events = screenModel.navigationEvents) { event ->
                when (event) {
                    is NavEvent.CloseConversation -> onNavigateBack()
                    is NavEvent.NavigateToMessageDetails -> Unit
                    is NavEvent.ForwardMessage -> Unit
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            navEventsFlow.subscriptionCount.value == 1
        }

        composeTestRule.runOnIdle {
            inputGeneration.value = 1
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(1, navEventsFlow.subscriptionCount.value)
            assertEquals(true, navEventsFlow.tryEmit(NavEvent.CloseConversation))
        }

        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            currentNavigateBackCount.get() == 1
        }

        composeTestRule.runOnIdle {
            assertEquals(0, staleNavigateBackCount.get())
            assertEquals(1, currentNavigateBackCount.get())
        }
    }

    private fun mockConversationScreenModel(
        effectsFlow: Flow<ConversationScreenEffect> = emptyFlow(),
        navEventsFlow: Flow<NavEvent> = emptyFlow(),
    ): ConversationScreenModel {
        return mockk<ConversationScreenModel>(relaxed = true) {
            every { effects } returns effectsFlow
            every { navigationEvents } returns navEventsFlow
            every { mediaPickerOverlayUiState } returns MutableStateFlow(
                value = ConversationMediaPickerOverlayUiState(),
            )
            every { scaffoldUiState } returns MutableStateFlow(
                value = ConversationScreenScaffoldUiState(),
            )
        }
    }

    private companion object {
        private const val CAPTURED_INPUT_CHANGE_COUNT = 5
    }
}
