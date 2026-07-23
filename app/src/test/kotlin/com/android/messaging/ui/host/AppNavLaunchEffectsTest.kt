package com.android.messaging.ui.host

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavKey
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.navigation.ConversationNavKey
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
    fun launchDestinations_resetTheBackStackPerEmission() {
        val channel = Channel<List<NavKey>>(Channel.BUFFERED)
        val launchDestinations: Flow<List<NavKey>> = channel.receiveAsFlow()
        val resets = mutableListOf<List<NavKey>>()

        composeTestRule.setContent {
            AppNavLaunchEffects(
                launchDestinations = launchDestinations,
                onResetBackStack = { resets += it },
            )
        }
        composeTestRule.waitForIdle()

        val listOnly = listOf<NavKey>(ConversationListNavKey)
        val listAndConversation = listOf(
            ConversationListNavKey,
            ConversationNavKey(CONVERSATION_ID),
        )

        composeTestRule.runOnIdle {
            channel.trySend(listAndConversation)
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            channel.trySend(listOnly)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(listOf(listAndConversation, listOnly), resets)
        }
    }
}
