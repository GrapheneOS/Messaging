package com.android.messaging.ui.conversationlist.ui.support

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class ConversationAppearanceAnimationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scrollPositionChange_doesNotRecomposeAppearanceTokenProbe() {
        val conversationItems = previewConversationListItems()
        var probeCompositions = 0
        lateinit var listState: LazyListState

        composeRule.setContent {
            listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier.height(96.dp),
            ) {
                items(items = (0 until 50).toList()) { index ->
                    BasicText(
                        text = "row $index",
                        modifier = Modifier.height(48.dp),
                    )
                }
            }

            AppearanceTokenProbe(
                items = conversationItems,
                listState = listState,
                excludedConversationIds = persistentSetOf(),
                onComposed = {
                    probeCompositions += 1
                },
            )
        }

        composeRule.waitForIdle()
        val afterInitialComposition = probeCompositions

        composeRule.runOnIdle {
            listState.requestScrollToItem(index = 0, scrollOffset = 24)
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(24, listState.firstVisibleItemScrollOffset)
            assertEquals(afterInitialComposition, probeCompositions)
        }
    }

    @Test
    fun appearanceTokenHolder_unrelatedRecomposition_returnsSameInstance() {
        val tick = mutableIntStateOf(0)
        val seenTokens = mutableListOf<AppearanceAnimationTokens>()
        var latestSeenTick = -1

        composeRule.setContent {
            val listState = rememberLazyListState()
            val currentTick = tick.intValue
            val tokens = rememberAppearanceAnimationTokens(
                items = previewConversationListItems(),
                listState = listState,
                excludedConversationIds = persistentSetOf(),
            )

            SideEffect {
                latestSeenTick = currentTick
                seenTokens += tokens
            }
        }

        composeRule.waitForIdle()
        val initialTokens = seenTokens.last()
        val sizeBeforeTick = seenTokens.size

        composeRule.runOnIdle {
            tick.intValue += 1
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, latestSeenTick)
            assertTrue(seenTokens.size > sizeBeforeTick)
            assertSame(initialTokens, seenTokens.last())
        }
    }

    @Test
    fun newConversationAtTop_insertionComposition_getsToken() {
        val newConversationId = "new"
        val initialItems = previewConversationListItems()
        val itemsState = mutableStateOf(initialItems)
        val newConversationTokens = mutableListOf<AppearanceAnimationToken?>()

        composeRule.setContent {
            val listState = rememberLazyListState()
            val tokens = rememberAppearanceAnimationTokens(
                items = itemsState.value,
                listState = listState,
                excludedConversationIds = persistentSetOf(),
            )
            val newConversationToken = tokens.tokenFor(newConversationId)

            SideEffect {
                newConversationTokens += newConversationToken
            }
        }

        composeRule.waitForIdle()
        val recordsBeforeInsertion = newConversationTokens.size
        composeRule.runOnIdle {
            assertNull(newConversationTokens.last())
        }

        composeRule.runOnIdle {
            val newItem = previewConversationListItem(
                conversationId = newConversationId,
                title = "New conversation",
                snippetText = "New message",
            )
            itemsState.value = (listOf(newItem) + initialItems).toImmutableList()
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            // Check the first composition after insertion. A later tracker update could
            // hide stale enteringTokens from an over-broad remember key.
            assertTrue(newConversationTokens.size > recordsBeforeInsertion)
            assertNotNull(newConversationTokens[recordsBeforeInsertion])
        }
    }

    @Composable
    private fun AppearanceTokenProbe(
        items: ImmutableList<ConversationListItemUiModel>,
        listState: LazyListState,
        excludedConversationIds: ImmutableSet<String>,
        onComposed: () -> Unit,
    ) {
        rememberAppearanceAnimationTokens(
            items = items,
            listState = listState,
            excludedConversationIds = excludedConversationIds,
        )

        SideEffect {
            onComposed()
        }
    }
}
