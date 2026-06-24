package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationListOptimisticReducerTest {

    private val reducer = ConversationListOptimisticReducer()

    @Test
    fun apply_emptyOverrides_returnsItemsUnchanged() {
        val items = persistentListOf(
            conversationItem("a"),
            conversationItem("b"),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(),
        )

        assertEquals(items, result)
    }

    @Test
    fun apply_archivedOverride_removesItem() {
        val archivedItem = conversationItem("a")
        val items = persistentListOf(
            archivedItem,
            conversationItem("b"),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                archiveById = persistentMapOf(
                    "a" to ConversationArchiveOverride.Archived(archivedItem),
                ),
            ),
        )

        assertEquals(listOf("b"), result.conversationIds())
    }

    @Test
    fun apply_readOverride_updatesStateWithoutReordering() {
        val items = persistentListOf(
            conversationItem("a", isRead = false, timestamp = 1_000L),
            conversationItem("b", timestamp = 2_000L),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                readById = persistentMapOf("a" to true),
            ),
        )

        assertEquals(listOf("a", "b"), result.conversationIds())
        assertTrue(result.first().latestMessage.isRead)
    }

    @Test
    fun apply_pinOverride_reordersByPinThenTimestamp() {
        val items = persistentListOf(
            conversationItem("a", timestamp = 3_000L),
            conversationItem("b", isPinned = true, timestamp = 2_000L),
            conversationItem("c", timestamp = 1_000L),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                pinnedById = persistentMapOf("c" to true),
            ),
        )

        assertEquals(listOf("b", "c", "a"), result.conversationIds())
        assertTrue(result.first { it.conversationId == "c" }.isPinned)
    }

    @Test
    fun apply_unpinOverride_movesItemIntoTimestampOrder() {
        val items = persistentListOf(
            conversationItem("a", isPinned = true, timestamp = 1_000L),
            conversationItem("b", timestamp = 3_000L),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                pinnedById = persistentMapOf("a" to false),
            ),
        )

        assertEquals(listOf("b", "a"), result.conversationIds())
        assertFalse(result.last().isPinned)
    }

    @Test
    fun apply_restoringItemMissingFromRawSnapshot_keepsItVisibleAndOrdered() {
        val restoringItem = conversationItem(
            conversationId = "c",
            isPinned = true,
            isRead = false,
            timestamp = 2_000L,
        )

        val result = reducer.apply(
            items = persistentListOf(
                conversationItem("a", timestamp = 3_000L),
                conversationItem("b", timestamp = 1_000L),
            ),
            overrides = ConversationListOptimisticOverrides(
                archiveById = persistentMapOf(
                    "c" to ConversationArchiveOverride.Restoring(
                        item = restoringItem,
                        awaitingRemoval = false,
                    ),
                ),
                readById = persistentMapOf("c" to true),
            ),
        )

        assertEquals(listOf("c", "a", "b"), result.conversationIds())
        assertTrue(result.first().latestMessage.isRead)
    }

    @Test
    fun apply_restoringItemAlreadyInRawSnapshot_doesNotDuplicateCachedItem() {
        val cachedItem = conversationItem("a", isRead = false)
        val rawItem = conversationItem("a", isRead = true)

        val result = reducer.apply(
            items = persistentListOf(rawItem),
            overrides = ConversationListOptimisticOverrides(
                archiveById = persistentMapOf(
                    "a" to ConversationArchiveOverride.Restoring(
                        item = cachedItem,
                        awaitingRemoval = false,
                    ),
                ),
            ),
        )

        assertEquals(listOf("a"), result.conversationIds())
        assertTrue(result.single().latestMessage.isRead)
    }

    @Test
    fun prune_archivedItemMissingFromRawSnapshot_keepsItForUndo() {
        val archivedItem = conversationItem("a")
        val archivedOverride = ConversationArchiveOverride.Archived(archivedItem)

        val pruned = reducer.prune(
            items = persistentListOf(),
            overrides = ConversationListOptimisticOverrides(
                archiveById = persistentMapOf("a" to archivedOverride),
            ),
        )

        assertEquals(archivedOverride, pruned.archiveById["a"])
    }

    @Test
    fun prune_restoreRace_retainsOverridesUntilRawSnapshotCatchesUp() {
        val cachedItem = conversationItem(
            conversationId = "a",
            isPinned = false,
            isRead = false,
        )
        var overrides = ConversationListOptimisticOverrides(
            archiveById = persistentMapOf(
                "a" to ConversationArchiveOverride.Restoring(
                    item = cachedItem,
                    awaitingRemoval = true,
                ),
            ),
            readById = persistentMapOf("a" to true),
            pinnedById = persistentMapOf("a" to true),
        )

        overrides = reducer.prune(
            items = persistentListOf(cachedItem),
            overrides = overrides,
        )
        assertEquals(
            ConversationArchiveOverride.Restoring(
                item = cachedItem,
                awaitingRemoval = true,
            ),
            overrides.archiveById["a"],
        )

        overrides = reducer.prune(
            items = persistentListOf(),
            overrides = overrides,
        )
        assertEquals(
            ConversationArchiveOverride.Restoring(
                item = cachedItem,
                awaitingRemoval = false,
            ),
            overrides.archiveById["a"],
        )
        assertTrue(overrides.readById.getValue("a"))
        assertTrue(overrides.pinnedById.getValue("a"))

        overrides = reducer.prune(
            items = persistentListOf(cachedItem),
            overrides = overrides,
        )
        assertFalse("a" in overrides.archiveById)
        assertTrue(overrides.readById.getValue("a"))
        assertTrue(overrides.pinnedById.getValue("a"))

        overrides = reducer.prune(
            items = persistentListOf(
                conversationItem(
                    conversationId = "a",
                    isPinned = true,
                    isRead = true,
                ),
            ),
            overrides = overrides,
        )
        assertTrue(overrides.isEmpty)
    }

    @Test
    fun prune_itemMissingAndNotRestoring_dropsReadAndPinOverrides() {
        val pruned = reducer.prune(
            items = persistentListOf(),
            overrides = ConversationListOptimisticOverrides(
                readById = persistentMapOf("a" to true),
                pinnedById = persistentMapOf("a" to true),
            ),
        )

        assertTrue(pruned.isEmpty)
    }

    private fun List<ConversationListItem>.conversationIds(): List<String> {
        return map(ConversationListItem::conversationId)
    }
}
