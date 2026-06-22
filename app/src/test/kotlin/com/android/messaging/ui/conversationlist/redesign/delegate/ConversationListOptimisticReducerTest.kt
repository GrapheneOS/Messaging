package com.android.messaging.ui.conversationlist.redesign.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationListOptimisticReducerTest {

    private val reducer = ConversationListOptimisticReducer()

    @Test
    fun apply_emptyOverrides_returnsSameItems() {
        val items = listOf(
            conversationItem("a"),
            conversationItem("b"),
        ).toImmutableList()

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(),
        )

        assertEquals(items, result)
    }

    @Test
    fun apply_archivedId_removesItem() {
        val items = listOf(
            conversationItem("a"),
            conversationItem("b"),
        ).toImmutableList()

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                archivedIds = persistentSetOf("a"),
            ),
        )

        assertEquals(listOf("b"), result.map { it.conversationId })
    }

    @Test
    fun apply_readOverride_overridesReadState() {
        val items = listOf(
            conversationItem(
                conversationId = "a",
                isRead = false,
            ),
        ).toImmutableList()

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                readById = persistentMapOf("a" to true),
            ),
        )

        assertTrue(result.single().latestMessage.isRead)
    }

    @Test
    fun apply_pinOverride_pinsAndReordersToTop() {
        val items = listOf(
            conversationItem("a", timestamp = 3_000L),
            conversationItem("b", timestamp = 2_000L),
            conversationItem("c", timestamp = 1_000L),
        ).toImmutableList()

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                pinnedById = persistentMapOf("c" to true),
            ),
        )

        assertEquals(listOf("c", "a", "b"), result.map { it.conversationId })
        assertTrue(result.first().isPinned)
    }

    @Test
    fun apply_restoringItemMissingFromDatabase_keepsItemVisible() {
        val item = conversationItem(
            conversationId = "a",
            isRead = false,
        )

        val result = reducer.apply(
            items = emptyList<ConversationListItem>().toImmutableList(),
            overrides = ConversationListOptimisticOverrides(
                restoringById = persistentMapOf(
                    "a" to RestoringConversation(
                        item = item,
                        hasObservedArchivedSnapshot = true,
                    ),
                ),
                readById = persistentMapOf("a" to true),
            ),
        )

        assertEquals(listOf("a"), result.map(ConversationListItem::conversationId))
        assertTrue(result.single().latestMessage.isRead)
    }

    @Test
    fun apply_restoringItemAlongsideExistingItems_reordersByPinThenTimestamp() {
        val present = listOf(
            conversationItem("a", timestamp = 3_000L),
            conversationItem("b", timestamp = 1_000L),
        ).toImmutableList()

        val restoring = conversationItem(
            conversationId = "c",
            isPinned = true,
            timestamp = 2_000L,
        )

        val result = reducer.apply(
            items = present,
            overrides = ConversationListOptimisticOverrides(
                restoringById = persistentMapOf(
                    "c" to RestoringConversation(
                        item = restoring,
                        hasObservedArchivedSnapshot = true,
                    ),
                ),
            ),
        )

        assertEquals(listOf("c", "a", "b"), result.map { it.conversationId })
    }

    @Test
    fun prune_dropsArchivedId_whenItemNoLongerPresent() {
        val raw = listOf(conversationItem("b")).toImmutableList()

        val pruned = reducer.prune(
            items = raw,
            overrides = ConversationListOptimisticOverrides(
                archivedIds = persistentSetOf("a"),
            ),
        )

        assertTrue(pruned.isEmpty)
    }

    @Test
    fun prune_keepsArchivedId_whilePresent() {
        val raw = listOf(conversationItem("a")).toImmutableList()

        val pruned = reducer.prune(
            items = raw,
            overrides = ConversationListOptimisticOverrides(
                archivedIds = persistentSetOf("a"),
            ),
        )

        assertTrue("a" in pruned.archivedIds)
    }

    @Test
    fun prune_dropsReadOverride_whenDatabaseMatches() {
        val raw = listOf(
            conversationItem(
                conversationId = "a",
                isRead = true,
            ),
        ).toImmutableList()

        val pruned = reducer.prune(
            items = raw,
            overrides = ConversationListOptimisticOverrides(
                readById = persistentMapOf("a" to true),
            ),
        )

        assertFalse("a" in pruned.readById)
    }

    @Test
    fun prune_keepsReadOverride_whileDatabaseDiffers() {
        val raw = listOf(
            conversationItem(
                conversationId = "a",
                isRead = false,
            ),
        ).toImmutableList()

        val pruned = reducer.prune(
            items = raw,
            overrides = ConversationListOptimisticOverrides(
                readById = persistentMapOf("a" to true),
            ),
        )

        assertEquals(true, pruned.readById["a"])
    }

    @Test
    fun archiveUndoThenRead_keepsItemAndReadOverrideThroughDatabaseRace() {
        val unreadItem = conversationItem(
            conversationId = "a",
            isRead = false,
        )
        var overrides = ConversationListOptimisticOverrides(
            archivedItemsById = persistentMapOf("a" to unreadItem),
            restoringById = persistentMapOf(
                "a" to RestoringConversation(
                    item = unreadItem,
                    hasObservedArchivedSnapshot = false,
                ),
            ),
            readById = persistentMapOf("a" to true),
        )

        overrides = reducer.prune(
            items = emptyList<ConversationListItem>().toImmutableList(),
            overrides = overrides,
        )

        val whileArchiveSnapshotIsVisible = reducer.apply(
            items = emptyList<ConversationListItem>().toImmutableList(),
            overrides = overrides,
        )

        assertEquals(listOf("a"), whileArchiveSnapshotIsVisible.map { it.conversationId })
        assertTrue(whileArchiveSnapshotIsVisible.single().latestMessage.isRead)
        assertTrue(overrides.restoringById.getValue("a").hasObservedArchivedSnapshot)
        assertEquals(true, overrides.readById["a"])

        overrides = reducer.prune(
            items = listOf(unreadItem).toImmutableList(),
            overrides = overrides,
        )

        val afterUnarchiveSnapshot = reducer.apply(
            items = listOf(unreadItem).toImmutableList(),
            overrides = overrides,
        )

        assertFalse("a" in overrides.restoringById)
        assertTrue(afterUnarchiveSnapshot.single().latestMessage.isRead)
        assertEquals(true, overrides.readById["a"])
    }

    @Test
    fun prune_dropsPinOverride_whenDatabaseMatches() {
        val raw = listOf(
            conversationItem(
                conversationId = "a",
                isPinned = true,
            ),
        ).toImmutableList()

        val pruned = reducer.prune(
            items = raw,
            overrides = ConversationListOptimisticOverrides(
                pinnedById = persistentMapOf("a" to true),
            ),
        )

        assertFalse("a" in pruned.pinnedById)
    }

    @Test
    fun prune_keepsPinOverride_whileDatabaseDiffers() {
        val raw = listOf(
            conversationItem(
                conversationId = "a",
                isPinned = false,
            ),
        ).toImmutableList()

        val pruned = reducer.prune(
            items = raw,
            overrides = ConversationListOptimisticOverrides(
                pinnedById = persistentMapOf("a" to true),
            ),
        )

        assertEquals(true, pruned.pinnedById["a"])
    }
}
