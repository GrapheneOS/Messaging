@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.android.messaging.ui.conversation.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategyScope
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.common.components.ListDetailPaneSide
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.conversationlist.navigation.conversationListEntries
import com.android.messaging.ui.navigation.ListDetailPaneKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class ConversationNavEntriesTest {

    private val entryProvider = entryProvider<NavKey> {
        conversationListEntries()
        conversationEntries()
    }

    @Test
    fun newChat_inTwoPaneWindow_opensInTheDetailPaneNextToTheList() {
        val entries = navEntries(ConversationListNavKey, NewChatNavKey)

        val scene = listDetailScene(
            entries = entries,
            maxHorizontalPartitions = TWO_PANES,
        )

        assertEquals(entries, scene?.entries)
    }

    @Test
    fun newChat_overOpenConversationInTwoPaneWindow_goesBackToThatConversation() {
        val entries = navEntries(
            ConversationListNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
            NewChatNavKey,
        )

        val scene = listDetailScene(
            entries = entries,
            maxHorizontalPartitions = TWO_PANES,
        )

        assertEquals(entries, scene?.entries)
        assertEquals(entries.dropLast(n = 1), scene?.previousEntries)
    }

    @Test
    fun newChat_inSinglePaneWindow_isLeftToFillTheWindow() {
        val scene = listDetailScene(
            entries = navEntries(ConversationListNavKey, NewChatNavKey),
            maxHorizontalPartitions = SINGLE_PANE,
        )

        assertNull(scene)
    }

    @Test
    fun newChat_inListDetailLayout_takesTheEndSide() {
        val entry = entryProvider(NewChatNavKey)

        assertEquals(ListDetailPaneSide.End, entry.metadata[ListDetailPaneKey])
    }

    private fun navEntries(vararg keys: NavKey): List<NavEntry<NavKey>> {
        return keys.map(entryProvider)
    }

    private fun listDetailScene(
        entries: List<NavEntry<NavKey>>,
        maxHorizontalPartitions: Int,
    ): Scene<NavKey>? {
        val sceneStrategy = ListDetailSceneStrategy<NavKey>(
            shouldHandleSinglePaneLayout = false,
            backNavigationBehavior = BackNavigationBehavior.PopLatest,
            directive = PaneScaffoldDirective.Default.copy(
                maxHorizontalPartitions = maxHorizontalPartitions,
            ),
            adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
            paneExpansionDragHandle = null,
            paneExpansionState = null,
        )

        return with(SceneStrategyScope<NavKey>()) {
            with(sceneStrategy) {
                calculateScene(entries)
            }
        }
    }

    private companion object {
        private const val SINGLE_PANE = 1
        private const val TWO_PANES = 2
    }
}
