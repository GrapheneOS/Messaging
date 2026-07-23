package com.android.messaging.ui.host

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppNavLaunchEffectsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun initialLaunchRequest_isSeededOnceAndDoesNotResetBackStack() {
        val request = ConversationEntryLaunchRequest(conversationId = CONVERSATION_ID)
        val seeded = mutableListOf<ConversationEntryLaunchRequest>()
        val backStackResets = mutableListOf<ConversationEntryLaunchRequest?>()
        val recomposeTrigger = mutableStateOf(value = 0)

        composeTestRule.setContent {
            @Suppress("UNUSED_EXPRESSION")
            recomposeTrigger.value

            AppNavLaunchEffects(
                initialLaunchRequest = request,
                launchRequests = emptyFlow(),
                onLaunchRequest = { seeded += it },
                onLaunchBackStack = { backStackResets += it },
            )
        }

        composeTestRule.runOnIdle {
            repeat(times = 3) { recomposeTrigger.value += 1 }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(listOf(request), seeded)
            assertEquals(emptyList<ConversationEntryLaunchRequest?>(), backStackResets)
        }
    }

    @Test
    fun nullInitialLaunchRequest_isNotSeeded() {
        val seeded = mutableListOf<ConversationEntryLaunchRequest>()

        composeTestRule.setContent {
            AppNavLaunchEffects(
                initialLaunchRequest = null,
                launchRequests = emptyFlow(),
                onLaunchRequest = { seeded += it },
                onLaunchBackStack = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(emptyList<ConversationEntryLaunchRequest>(), seeded)
        }
    }

    @Test
    fun launchRequests_seedPayloadAndResetBackStackPerEmission() {
        val channel = Channel<ConversationEntryLaunchRequest?>(Channel.BUFFERED)
        val launchRequests: Flow<ConversationEntryLaunchRequest?> = channel.receiveAsFlow()
        val request = ConversationEntryLaunchRequest(conversationId = CONVERSATION_ID)
        val seeded = mutableListOf<ConversationEntryLaunchRequest>()
        val backStackResets = mutableListOf<ConversationEntryLaunchRequest?>()

        composeTestRule.setContent {
            AppNavLaunchEffects(
                initialLaunchRequest = null,
                launchRequests = launchRequests,
                onLaunchRequest = { seeded += it },
                onLaunchBackStack = { backStackResets += it },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            channel.trySend(request)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            channel.trySend(null)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(listOf(request), seeded)
            assertEquals(listOf(request, null), backStackResets)
        }
    }
}
