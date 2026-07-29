package com.android.messaging.ui.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.testutil.assertThat
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeededViewModelStoreOwnerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun seededViewModelStoreOwner_seedsDefaultArgsIntoTheSavedStateHandleAsUnderlyingTypes() {
        val savedStateHandle = captureInSeededScope(defaultArgs = defaultArgs()) {
            seedProbeSavedStateHandle()
        }

        assertThat(savedStateHandle.get<String>(CONVERSATION_ID_KEY))
            .isEqualTo(CONVERSATION_ID_VALUE)
        assertThat(savedStateHandle.get<String>(MESSAGE_ID_KEY))
            .isEqualTo(MESSAGE_ID_VALUE)
    }

    @Test
    fun seededViewModelStoreOwner_seedsArgumentsThatDecodeBackIntoTypedIds() {
        val savedStateHandle = captureInSeededScope(defaultArgs = defaultArgs()) {
            seedProbeSavedStateHandle()
        }

        assertThat(ConversationId.fromOrNull(savedStateHandle[CONVERSATION_ID_KEY]))
            .isEqualTo(ConversationId(CONVERSATION_ID_VALUE))
        assertThat(MessageId.fromOrNull(savedStateHandle[MESSAGE_ID_KEY]))
            .isEqualTo(MessageId(MESSAGE_ID_VALUE))
    }

    @Test
    fun seededViewModelStoreOwner_exposesTheSeededBundleAsDefaultArgs() {
        val defaultArgs = defaultArgs()

        val seededArgs = captureInSeededScope(defaultArgs = defaultArgs) {
            seededDefaultArgs()
        }

        assertSame(defaultArgs, seededArgs)
    }

    @Test
    fun seededViewModelStoreOwner_reusesTheParentViewModelStore() {
        val seededStore = captureInSeededScope(defaultArgs = defaultArgs()) {
            seededViewModelStore()
        }

        assertSame(composeTestRule.activity.viewModelStore, seededStore)
    }

    @Test
    fun seededViewModelStoreOwner_withoutMatchingArgument_readsBackNull() {
        val savedStateHandle = captureInSeededScope(defaultArgs = Bundle()) {
            seedProbeSavedStateHandle()
        }

        assertNull(savedStateHandle.get<String>(CONVERSATION_ID_KEY))
        assertNull(ConversationId.fromOrNull(savedStateHandle[CONVERSATION_ID_KEY]))
    }

    @Test
    fun seededViewModelStoreOwner_keepsSeparateSeedsForNestedScopes() {
        var outerHandle: SavedStateHandle? = null
        var innerHandle: SavedStateHandle? = null

        composeTestRule.setContent {
            SeededViewModelStoreOwner(defaultArgs = defaultArgs()) {
                outerHandle = seedProbeSavedStateHandle(key = "outer")

                SeededViewModelStoreOwner(
                    defaultArgs = defaultArgs(messageIdValue = OTHER_MESSAGE_ID_VALUE),
                ) {
                    innerHandle = seedProbeSavedStateHandle(key = "inner")
                }
            }
        }
        composeTestRule.waitForIdle()

        assertThat(outerHandle?.get<String>(MESSAGE_ID_KEY)).isEqualTo(MESSAGE_ID_VALUE)
        assertThat(innerHandle?.get<String>(MESSAGE_ID_KEY)).isEqualTo(OTHER_MESSAGE_ID_VALUE)
    }

    private fun <T : Any> captureInSeededScope(
        defaultArgs: Bundle,
        capture: @Composable () -> T,
    ): T {
        var captured: T? = null

        composeTestRule.setContent {
            SeededViewModelStoreOwner(defaultArgs = defaultArgs) {
                captured = capture()
            }
        }
        composeTestRule.waitForIdle()

        return checkNotNull(captured) { "Seeded scope produced no value" }
    }

    @Composable
    private fun seedProbeSavedStateHandle(key: String? = null): SavedStateHandle {
        return viewModel<SeedProbeViewModel>(key = key).savedStateHandle
    }

    @Composable
    private fun seededDefaultArgs(): Bundle {
        val owner = checkNotNull(LocalViewModelStoreOwner.current) as
            HasDefaultViewModelProviderFactory

        return checkNotNull(owner.defaultViewModelCreationExtras[DEFAULT_ARGS_KEY])
    }

    @Composable
    private fun seededViewModelStore(): ViewModelStore {
        return checkNotNull(LocalViewModelStoreOwner.current).viewModelStore
    }

    private fun defaultArgs(
        conversationIdValue: String = CONVERSATION_ID_VALUE,
        messageIdValue: String = MESSAGE_ID_VALUE,
    ): Bundle {
        return Bundle().apply {
            putString(CONVERSATION_ID_KEY, conversationIdValue)
            putString(MESSAGE_ID_KEY, messageIdValue)
        }
    }

    internal class SeedProbeViewModel(
        val savedStateHandle: SavedStateHandle,
    ) : ViewModel()

    private companion object {
        const val CONVERSATION_ID_KEY = "conversationId"
        const val MESSAGE_ID_KEY = "messageId"
        const val CONVERSATION_ID_VALUE = "conversation-1"
        const val MESSAGE_ID_VALUE = "message-1"
        const val OTHER_MESSAGE_ID_VALUE = "message-2"
    }
}
