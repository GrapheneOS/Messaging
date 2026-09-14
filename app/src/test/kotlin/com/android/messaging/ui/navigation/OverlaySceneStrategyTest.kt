package com.android.messaging.ui.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategyScope
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.navigation.ConversationNavKey
import com.android.messaging.ui.conversation.navigation.NewChatNavKey
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlaySceneStrategyTest {

    @Test
    fun overlayScene_resolvesOverlayEntryOnTopAsTheOnlyRenderedEntry() {
        val listEntry = navEntry(key = ConversationListNavKey)
        val overlayEntry = overlayNavEntry(key = NewChatNavKey)

        val scene = overlaySceneOrNull(
            entries = listOf(listEntry, overlayEntry),
            onNavigateBack = {},
        )

        assertEquals(listOf(overlayEntry), scene?.entries)
    }

    @Test
    fun overlayScene_keepsOverlaidEntriesAsPreviousEntries() {
        val listEntry = navEntry(key = ConversationListNavKey)
        val conversationEntry = navEntry(key = ConversationNavKey(conversationId = CONVERSATION_ID))
        val overlayEntry = overlayNavEntry(key = NewChatNavKey)

        val scene = overlaySceneOrNull(
            entries = listOf(listEntry, conversationEntry, overlayEntry),
            onNavigateBack = {},
        )

        assertEquals(listOf(listEntry, conversationEntry), scene?.previousEntries)
        assertEquals(listOf(listEntry, conversationEntry), scene?.overlaidEntries)
    }

    @Test
    fun overlayScene_usesContentKeyOfOverlayEntryAsSceneKey() {
        val overlayEntry = overlayNavEntry(key = NewChatNavKey)

        val scene = overlaySceneOrNull(
            entries = listOf(navEntry(key = ConversationListNavKey), overlayEntry),
            onNavigateBack = {},
        )

        assertEquals(overlayEntry.contentKey, scene?.key)
        assertNotEquals(NewChatNavKey, scene?.key)
    }

    @Test
    fun overlayScene_stackedOnAnotherOverlay_overlaysOnlyTheEntriesBeneathIt() {
        val listEntry = navEntry(key = ConversationListNavKey)
        val lowerOverlay = overlayNavEntry(key = NewChatNavKey)
        val upperOverlay = overlayNavEntry(
            key = ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        val scene = overlaySceneOrNull(
            entries = listOf(listEntry, lowerOverlay, upperOverlay),
            onNavigateBack = {},
        )

        assertEquals(listOf(upperOverlay), scene?.entries)
        assertEquals(listOf(listEntry, lowerOverlay), scene?.previousEntries)
    }

    @Test
    fun overlayScene_delegatesDismissToOnBack() {
        var didNavigateBack = false

        val scene = overlaySceneOrNull(
            entries = listOf(
                navEntry(key = ConversationListNavKey),
                overlayNavEntry(key = NewChatNavKey),
            ),
            onNavigateBack = { didNavigateBack = true },
        )

        scene?.onNavigateBack?.invoke()

        assertTrue(didNavigateBack)
    }

    @Test
    fun overlayScene_declinesWhenOverlayEntryIsTheOnlyEntry() {
        val scene = overlaySceneOrNull(
            entries = listOf(overlayNavEntry(key = NewChatNavKey)),
            onNavigateBack = {},
        )

        assertNull(scene)
    }

    @Test
    fun overlayScene_declinesWhenTopEntryIsNotAnOverlay() {
        val scene = overlaySceneOrNull(
            entries = listOf(
                overlayNavEntry(key = NewChatNavKey),
                navEntry(key = ConversationListNavKey),
            ),
            onNavigateBack = {},
        )

        assertNull(scene)
    }

    @Test
    fun overlaySceneStrategy_resolvesTheOverlaySceneThroughCalculateScene() {
        val listEntry = navEntry(key = ConversationListNavKey)
        val overlayEntry = overlayNavEntry(key = NewChatNavKey)

        val scene = with(SceneStrategyScope<NavKey>()) {
            with(OverlaySceneStrategy()) {
                calculateScene(listOf(listEntry, overlayEntry))
            }
        }

        assertEquals(listOf(overlayEntry), scene?.entries)
        assertEquals(listOf(listEntry), scene?.previousEntries)
    }

    @Test
    fun overlayScene_declinesWhenThereAreNoEntries() {
        val scene = overlaySceneOrNull(
            entries = emptyList(),
            onNavigateBack = {},
        )

        assertNull(scene)
    }

    private fun navEntry(key: NavKey): NavEntry<NavKey> {
        return navEntry(key = key, metadata = emptyMap())
    }

    private fun overlayNavEntry(key: NavKey): NavEntry<NavKey> {
        return navEntry(key = key, metadata = overlayMetadata())
    }

    private fun navEntry(
        key: NavKey,
        metadata: Map<String, Any>,
    ): NavEntry<NavKey> {
        return NavEntry(
            key = key,
            contentKey = "content-key-of-$key",
            metadata = metadata,
            content = {},
        )
    }
}
