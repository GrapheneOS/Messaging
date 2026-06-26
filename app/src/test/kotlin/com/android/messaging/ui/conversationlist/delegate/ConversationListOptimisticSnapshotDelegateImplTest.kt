package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListMode
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.snapshotOfIds
import com.android.messaging.ui.conversationlist.snapshotOfItems
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationListOptimisticSnapshotDelegateImplTest {

    @Test
    fun archive_removesItemFromEffectiveSnapshot() = runTest {
        val delegate = bindDelegate(snapshotOfIds("a", "b"))

        delegate.remove(listOf("a"))

        assertEquals(listOf("b"), delegate.conversationIds())
    }

    @Test
    fun pin_reordersEffectiveSnapshotToTop() = runTest {
        val delegate = bindDelegate(
            snapshotOfItems(
                conversationItem("a", timestamp = 3_000L),
                conversationItem("b", timestamp = 2_000L),
                conversationItem("c", timestamp = 1_000L),
            ),
        )

        delegate.pin(
            conversationIds = listOf("c"),
            isPinned = true,
        )

        assertEquals(listOf("c", "a", "b"), delegate.conversationIds())
    }

    @Test
    fun markRead_overridesReadStateInEffectiveSnapshot() = runTest {
        val delegate = bindDelegate(
            snapshotOfItems(
                conversationItem(
                    conversationId = "a",
                    isRead = false,
                ),
            ),
        )

        delegate.markRead(
            conversationIds = listOf("a"),
            isRead = true,
        )

        val item = requireNotNull(delegate.snapshot.value).items.single()
        assertTrue(item.latestMessage.isRead)
    }

    @Test
    fun restoreArchived_afterArchive_bringsItemBack() = runTest {
        val delegate = bindDelegate(snapshotOfIds("a", "b"))

        delegate.remove(listOf("a"))
        delegate.restore(listOf("a"))

        assertTrue("a" in delegate.conversationIds())
    }

    @Test
    fun readOverrideIsDropped_afterDatabaseCatchesUp() = runTest {
        val rawSnapshot = MutableStateFlow(
            snapshotOfItems(
                conversationItem(
                    conversationId = "a",
                    isRead = false,
                ),
            ),
        )

        val delegate = bindDelegate(rawSnapshot)
        delegate.markRead(
            conversationIds = listOf("a"),
            isRead = true,
        )

        rawSnapshot.value = snapshotOfItems(
            conversationItem(
                conversationId = "a",
                isRead = true,
            ),
        )
        runCurrent()

        rawSnapshot.value = snapshotOfItems(
            conversationItem(
                conversationId = "a",
                isRead = false,
            ),
        )
        runCurrent()

        val item = requireNotNull(delegate.snapshot.value).items.single()
        assertFalse(item.latestMessage.isRead)
    }

    @Test
    fun discardArchived_afterDatabaseRemovesItem_keepsItemHidden() = runTest {
        val rawSnapshot = MutableStateFlow(snapshotOfIds("a", "b"))
        val delegate = bindDelegate(rawSnapshot)

        delegate.remove(listOf("a"))
        rawSnapshot.value = snapshotOfIds("b")
        runCurrent()

        delegate.discardRemoval(listOf("a"))

        assertEquals(listOf("b"), delegate.conversationIds())
    }

    @Test
    fun restoreThenDiscard_keepsRestoredItemVisible() = runTest {
        val delegate = bindDelegate(snapshotOfIds("a", "b"))

        delegate.remove(listOf("a"))
        delegate.restore(listOf("a"))
        delegate.discardRemoval(listOf("a"))

        assertTrue("a" in delegate.conversationIds())
    }

    @Test
    fun pinOverrideIsDropped_afterDatabaseCatchesUp() = runTest {
        val rawSnapshot = MutableStateFlow(
            snapshotOfItems(
                conversationItem("a", isPinned = false, timestamp = 1_000L),
                conversationItem("b", timestamp = 2_000L),
            ),
        )
        val delegate = bindDelegate(rawSnapshot)

        delegate.pin(
            conversationIds = listOf("a"),
            isPinned = true,
        )
        assertEquals(listOf("a", "b"), delegate.conversationIds())

        rawSnapshot.value = snapshotOfItems(
            conversationItem("a", isPinned = true, timestamp = 1_000L),
            conversationItem("b", timestamp = 2_000L),
        )
        runCurrent()

        rawSnapshot.value = snapshotOfItems(
            conversationItem("b", timestamp = 2_000L),
            conversationItem("a", isPinned = false, timestamp = 1_000L),
        )
        runCurrent()

        val pinnedItem = delegate.snapshot.value
            ?.items
            ?.first { item -> item.conversationId == "a" }

        assertFalse(requireNotNull(pinnedItem).isPinned)
        assertEquals(listOf("b", "a"), delegate.conversationIds())
    }

    @Test
    fun bind_isIdempotent() = runTest {
        val repository = mockk<ConversationListRepository>()
        val delegate = ConversationListOptimisticSnapshotDelegateImpl(
            repository = repository,
            reducer = ConversationListOptimisticReducer(),
        )
        every {
            repository.observeSnapshot(ConversationListMode.Inbox)
        } returns MutableStateFlow(snapshotOfIds("a"))

        delegate.bind(backgroundScope, ConversationListMode.Inbox)
        delegate.bind(backgroundScope, ConversationListMode.Inbox)
        runCurrent()

        verify(exactly = 1) { repository.observeSnapshot(ConversationListMode.Inbox) }
    }

    private fun TestScope.bindDelegate(
        rawSnapshot: ConversationListSnapshot,
    ): ConversationListOptimisticSnapshotDelegateImpl {
        return bindDelegate(MutableStateFlow(rawSnapshot))
    }

    private fun TestScope.bindDelegate(
        rawSnapshot: MutableStateFlow<ConversationListSnapshot>,
    ): ConversationListOptimisticSnapshotDelegateImpl {
        val repository = mockk<ConversationListRepository>()
        every { repository.observeSnapshot(ConversationListMode.Inbox) } returns rawSnapshot

        return ConversationListOptimisticSnapshotDelegateImpl(
            repository = repository,
            reducer = ConversationListOptimisticReducer(),
        ).apply {
            bind(backgroundScope, ConversationListMode.Inbox)
            runCurrent()
        }
    }

    private fun ConversationListOptimisticSnapshotDelegateImpl.conversationIds(): List<String> {
        return snapshot.value
            ?.items
            ?.map(ConversationListItem::conversationId)
            .orEmpty()
    }
}
