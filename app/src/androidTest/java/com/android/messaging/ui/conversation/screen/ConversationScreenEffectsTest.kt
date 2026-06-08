package com.android.messaging.ui.conversation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.android.messaging.ui.conversation.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val CAPTURED_INPUT_CHANGE_COUNT = 5
private const val TEST_WAIT_TIMEOUT_MILLIS = 5_000L

internal class ConversationScreenEffectsTest {
    @get:Rule
    val composeRule = createComposeRule()

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

        composeRule.setContent {
            val snackbarHostState = remember(inputGeneration.value) {
                SnackbarHostState()
            }
            val hostBoundsState = remember(inputGeneration.value) {
                mutableStateOf<ComposeRect?>(value = null)
            }
            val onNavigateBack = remember(inputGeneration.value) {
                {}
            }

            ConversationScreenEffects(
                screenModel = screenModel,
                snackbarHostState = snackbarHostState,
                hostBoundsState = hostBoundsState,
                onNavigateToMessageDetails = {},
                onNavigateBack = onNavigateBack,
            )
        }

        composeRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            subscriptionCount.get() == 1
        }

        repeat(CAPTURED_INPUT_CHANGE_COUNT) { changeIndex ->
            composeRule.runOnIdle {
                inputGeneration.value = changeIndex + 1
            }
            composeRule.waitForIdle()
        }

        composeRule.runOnIdle {
            assertEquals(1, subscriptionCount.get())
            assertEquals(0, cancellationCount.get())
        }
    }

    @Test
    fun collectedEffectUsesLatestCapturedNavigateBackCallback() {
        val effectFlow = MutableSharedFlow<ConversationScreenEffect>(
            extraBufferCapacity = 1,
        )
        val screenModel = mockConversationScreenModel(effectsFlow = effectFlow)
        val staleNavigateBackCount = AtomicInteger()
        val currentNavigateBackCount = AtomicInteger()
        val inputGeneration = mutableStateOf(value = 0)

        composeRule.setContent {
            val snackbarHostState = remember(inputGeneration.value) {
                SnackbarHostState()
            }
            val hostBoundsState = remember(inputGeneration.value) {
                mutableStateOf<ComposeRect?>(value = null)
            }
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

            ConversationScreenEffects(
                screenModel = screenModel,
                snackbarHostState = snackbarHostState,
                hostBoundsState = hostBoundsState,
                onNavigateToMessageDetails = {},
                onNavigateBack = onNavigateBack,
            )
        }

        composeRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            effectFlow.subscriptionCount.value == 1
        }

        composeRule.runOnIdle {
            inputGeneration.value = 1
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, effectFlow.subscriptionCount.value)
            assertEquals(true, effectFlow.tryEmit(ConversationScreenEffect.CloseConversation))
        }

        composeRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            currentNavigateBackCount.get() == 1
        }

        composeRule.runOnIdle {
            assertEquals(0, staleNavigateBackCount.get())
            assertEquals(1, currentNavigateBackCount.get())
        }
    }

    private fun mockConversationScreenModel(
        effectsFlow: Flow<ConversationScreenEffect>,
    ): ConversationScreenModel {
        return mockk<ConversationScreenModel>(relaxed = true) {
            every { effects } returns effectsFlow
            every { mediaPickerOverlayUiState } returns MutableStateFlow(
                value = ConversationMediaPickerOverlayUiState(),
            )
            every { scaffoldUiState } returns MutableStateFlow(
                value = ConversationScreenScaffoldUiState(),
            )
        }
    }
}
