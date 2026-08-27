package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.delegate.ConversationRemovalOverride.Kind
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
            conversationItem(ConversationId("a")),
            conversationItem(ConversationId("b")),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(),
        )

        assertEquals(items, result)
    }

    @Test
    fun apply_removalOverride_hidesItem() {
        val removedItem = conversationItem(ConversationId("a"))
        val items = persistentListOf(
            removedItem,
            conversationItem(ConversationId("b")),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                removalById = persistentMapOf(
                    ConversationId("a") to ConversationRemovalOverride(
                        item = removedItem,
                        kind = Kind.Removed,
                        awaitingRemoval = true,
                    ),
                ),
            ),
        )

        assertEquals(listOf("b"), result.conversationIds())
    }

    @Test
    fun apply_readOverride_updatesStateWithoutReordering() {
        val items = persistentListOf(
            conversationItem(ConversationId("a"), isRead = false, timestamp = 1_000L),
            conversationItem(ConversationId("b"), timestamp = 2_000L),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                readById = persistentMapOf(ConversationId("a") to true),
            ),
        )

        assertEquals(listOf("a", "b"), result.conversationIds())
        assertTrue(result.first().latestMessage.isRead)
    }

    @Test
    fun apply_pinOverride_reordersByPinThenTimestamp() {
        val items = persistentListOf(
            conversationItem(ConversationId("a"), timestamp = 3_000L),
            conversationItem(ConversationId("b"), isPinned = true, timestamp = 2_000L),
            conversationItem(ConversationId("c"), timestamp = 1_000L),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                pinnedById = persistentMapOf(ConversationId("c") to true),
            ),
        )

        assertEquals(listOf("b", "c", "a"), result.conversationIds())
        assertTrue(result.first { it.conversationId == ConversationId("c") }.isPinned)
    }

    @Test
    fun apply_unpinOverride_movesItemIntoTimestampOrder() {
        val items = persistentListOf(
            conversationItem(ConversationId("a"), isPinned = true, timestamp = 1_000L),
            conversationItem(ConversationId("b"), timestamp = 3_000L),
        )

        val result = reducer.apply(
            items = items,
            overrides = ConversationListOptimisticOverrides(
                pinnedById = persistentMapOf(ConversationId("a") to false),
            ),
        )

        assertEquals(listOf("b", "a"), result.conversationIds())
        assertFalse(result.last().isPinned)
    }

    @Test
    fun apply_restoringItemMissingFromRawSnapshot_keepsItVisibleAndOrdered() {
        val restoredItem = conversationItem(
            conversationId = ConversationId("c"),
            isPinned = true,
            isRead = false,
            timestamp = 2_000L,
        )

        val result = reducer.apply(
            items = persistentListOf(
                conversationItem(ConversationId("a"), timestamp = 3_000L),
                conversationItem(ConversationId("b"), timestamp = 1_000L),
            ),
            overrides = ConversationListOptimisticOverrides(
                removalById = persistentMapOf(
                    ConversationId("c") to ConversationRemovalOverride(
                        item = restoredItem,
                        kind = Kind.Restored,
                        awaitingRemoval = false,
                    ),
                ),
                readById = persistentMapOf(ConversationId("c") to true),
            ),
        )

        assertEquals(listOf("c", "a", "b"), result.conversationIds())
        assertTrue(result.first().latestMessage.isRead)
    }

    @Test
    fun apply_restoringItemAlreadyInRawSnapshot_doesNotDuplicateCachedItem() {
        val cachedItem = conversationItem(ConversationId("a"), isRead = false)
        val rawItem = conversationItem(ConversationId("a"), isRead = true)

        val result = reducer.apply(
            items = persistentListOf(rawItem),
            overrides = ConversationListOptimisticOverrides(
                removalById = persistentMapOf(
                    ConversationId("a") to ConversationRemovalOverride(
                        item = cachedItem,
                        kind = Kind.Restored,
                        awaitingRemoval = false,
                    ),
                ),
            ),
        )

        assertEquals(listOf("a"), result.conversationIds())
        assertTrue(result.single().latestMessage.isRead)
    }

    @Test
    fun prune_removedItemMissingFromRawSnapshot_keepsItForUndo() {
        val removedItem = conversationItem(ConversationId("a"))
        val removalOverride = ConversationRemovalOverride(
            item = removedItem,
            kind = Kind.Removed,
            awaitingRemoval = true,
        )

        val pruned = reducer.prune(
            items = persistentListOf(),
            overrides = ConversationListOptimisticOverrides(
                removalById = persistentMapOf(ConversationId("a") to removalOverride),
            ),
        )

        assertThat(pruned.removalById[ConversationId("a")]).isEqualTo(
            removalOverride.copy(awaitingRemoval = false),
        )
    }

    @Test
    fun prune_removedItemBackInRawSnapshot_dropsStaleOverride() {
        val removedItem = conversationItem(ConversationId("a"))

        val pruned = reducer.prune(
            items = persistentListOf(removedItem),
            overrides = ConversationListOptimisticOverrides(
                removalById = persistentMapOf(
                    ConversationId("a") to ConversationRemovalOverride(
                        item = removedItem,
                        kind = Kind.Removed,
                        awaitingRemoval = false,
                    ),
                ),
            ),
        )

        assertThat(pruned.removalById[ConversationId("a")]).isNull()
    }

    @Test
    fun prune_restoreRace_retainsOverridesUntilRawSnapshotCatchesUp() {
        val cachedItem = conversationItem(
            conversationId = ConversationId("a"),
            isPinned = false,
            isRead = false,
        )
        var overrides = ConversationListOptimisticOverrides(
            removalById = persistentMapOf(
                ConversationId("a") to ConversationRemovalOverride(
                    item = cachedItem,
                    kind = Kind.Restored,
                    awaitingRemoval = true,
                ),
            ),
            readById = persistentMapOf(ConversationId("a") to true),
            pinnedById = persistentMapOf(ConversationId("a") to true),
        )

        overrides = reducer.prune(
            items = persistentListOf(cachedItem),
            overrides = overrides,
        )
        assertThat(overrides.removalById[ConversationId("a")]).isEqualTo(
            ConversationRemovalOverride(
                item = cachedItem,
                kind = Kind.Restored,
                awaitingRemoval = true,
            )
        )

        overrides = reducer.prune(
            items = persistentListOf(),
            overrides = overrides,
        )
        assertThat(overrides.removalById[ConversationId("a")]).isEqualTo(
            ConversationRemovalOverride(
                item = cachedItem,
                kind = Kind.Restored,
                awaitingRemoval = false,
            )
        )
        assertTrue(overrides.readById.getValue(ConversationId("a")))
        assertTrue(overrides.pinnedById.getValue(ConversationId("a")))

        overrides = reducer.prune(
            items = persistentListOf(cachedItem),
            overrides = overrides,
        )
        assertFalse(ConversationId("a") in overrides.removalById)
        assertTrue(overrides.readById.getValue(ConversationId("a")))
        assertTrue(overrides.pinnedById.getValue(ConversationId("a")))

        overrides = reducer.prune(
            items = persistentListOf(
                conversationItem(
                    conversationId = ConversationId("a"),
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
                readById = persistentMapOf(ConversationId("a") to true),
                pinnedById = persistentMapOf(ConversationId("a") to true),
            ),
        )

        assertTrue(pruned.isEmpty)
    }

    private fun List<ConversationListItem>.conversationIds(): List<String> {
        return map { item -> item.conversationId.value }
    }
}
