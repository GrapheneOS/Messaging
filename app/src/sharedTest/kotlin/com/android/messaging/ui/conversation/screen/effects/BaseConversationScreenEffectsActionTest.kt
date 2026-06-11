package com.android.messaging.ui.conversation.screen.effects

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.screen.ConversationScreenEffects
import com.android.messaging.ui.conversation.screen.ConversationScreenModel
import com.android.messaging.ui.conversation.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule

internal abstract class BaseConversationScreenEffectsActionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    protected lateinit var effectsFlow: MutableSharedFlow<ConversationScreenEffect>
    protected lateinit var screenModel: ConversationScreenModel
    protected lateinit var snackbarHostState: SnackbarHostState
    protected lateinit var hostBoundsState: MutableState<ComposeRect?>
    protected lateinit var defaultSmsRoleLauncher: ActivityResultLauncher<Intent>
    protected lateinit var defaultSmsRoleResultCallbackSlot:
        CapturingSlot<ActivityResultCallback<ActivityResult>>

    private lateinit var activityResultRegistry: ActivityResultRegistry
    private lateinit var activityResultRegistryOwner: ActivityResultRegistryOwner

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()

        effectsFlow = MutableSharedFlow(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)
        screenModel = mockk(relaxed = true)
        every { screenModel.effects } returns effectsFlow
        every { screenModel.mediaPickerOverlayUiState } returns MutableStateFlow(
            ConversationMediaPickerOverlayUiState(),
        )
        every { screenModel.scaffoldUiState } returns MutableStateFlow(
            ConversationScreenScaffoldUiState(),
        )

        defaultSmsRoleLauncher = mockk(relaxed = true)
        defaultSmsRoleResultCallbackSlot = slot()
        activityResultRegistry = mockk(relaxed = true)
        activityResultRegistryOwner = mockk()
        every { activityResultRegistryOwner.activityResultRegistry } returns activityResultRegistry
        every {
            activityResultRegistry.register(
                any<String>(),
                any<ActivityResultContract<Intent, ActivityResult>>(),
                capture(defaultSmsRoleResultCallbackSlot),
            )
        } returns defaultSmsRoleLauncher
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    protected fun setEffectsContent(
        initialSnackbarMessage: String? = null,
        initialHostBounds: ComposeRect? = ComposeRect(
            left = 0f,
            top = 0f,
            right = 100f,
            bottom = 100f,
        ),
        onNavigateToMessageDetails: (String) -> Unit = {},
        onNavigateBack: () -> Unit = {},
    ) {
        snackbarHostState = SnackbarHostState()
        hostBoundsState = mutableStateOf(initialHostBounds)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
            ) {
                AppTheme {
                    Box {
                        SnackbarHost(hostState = snackbarHostState)
                        if (initialSnackbarMessage != null) {
                            LaunchedEffect(initialSnackbarMessage) {
                                snackbarHostState.showSnackbar(
                                    message = initialSnackbarMessage,
                                    duration = SnackbarDuration.Indefinite,
                                )
                            }
                        }
                        ConversationScreenEffects(
                            screenModel = screenModel,
                            snackbarHostState = snackbarHostState,
                            hostBoundsState = hostBoundsState,
                            onNavigateToMessageDetails = onNavigateToMessageDetails,
                            onNavigateBack = onNavigateBack,
                        )
                    }
                }
            }
        }
        waitForEffectsCollector()
    }

    protected fun emitEffect(effect: ConversationScreenEffect) {
        composeTestRule.runOnIdle {
            assertTrue(effectsFlow.tryEmit(effect))
        }
        composeTestRule.waitForIdle()
    }

    protected fun waitForSnackbarMessage(message: String) {
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            snackbarHostState.currentSnackbarData?.visuals?.message == message
        }
    }

    @Suppress("SameParameterValue")
    protected fun dispatchDefaultSmsRoleResult(resultCode: Int) {
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            defaultSmsRoleResultCallbackSlot.isCaptured
        }
        composeTestRule.runOnIdle {
            defaultSmsRoleResultCallbackSlot.captured.onActivityResult(
                ActivityResult(resultCode, null),
            )
        }
    }

    protected fun waitForEffectsCollector() {
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            effectsFlow.subscriptionCount.value == 1
        }
    }

    private companion object {
        private const val EFFECT_BUFFER_CAPACITY = 16
    }
}
