package com.android.messaging.ui.conversationlist.redesign.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListOptimisticSnapshotDelegateImplTest {

    @Test
    fun archive_removesItemFromEffectiveSnapshot() = runTest(UnconfinedTestDispatcher()) {
        val delegate = bindDelegate(snapshot("a", "b"))

        delegate.archive(listOf("a"))
        advanceUntilIdle()

        assertEquals(listOf("b"), delegate.conversationIds())
    }

    @Test
    fun pin_reordersEffectiveSnapshotToTop() = runTest(UnconfinedTestDispatcher()) {
        val delegate = bindDelegate(
            snapshot(
                conversationItem("a", timestamp = 3_000L),
                conversationItem("b", timestamp = 2_000L),
                conversationItem("c", timestamp = 1_000L),
            ),
        )

        delegate.pin(
            conversationIds = listOf("c"),
            isPinned = true,
        )
        advanceUntilIdle()

        assertEquals(listOf("c", "a", "b"), delegate.conversationIds())
    }

    @Test
    fun markRead_overridesReadStateInEffectiveSnapshot() = runTest(UnconfinedTestDispatcher()) {
        val delegate = bindDelegate(
            snapshot(
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
        advanceUntilIdle()

        assertTrue(delegate.snapshot.value?.items?.single()?.latestMessage?.isRead == true)
    }

    @Test
    fun restoreArchived_afterArchive_bringsItemBack() = runTest(UnconfinedTestDispatcher()) {
        val delegate = bindDelegate(snapshot("a", "b"))

        delegate.archive(listOf("a"))
        advanceUntilIdle()

        delegate.restoreArchived(listOf("a"))
        advanceUntilIdle()

        assertTrue("a" in delegate.conversationIds())
    }

    @Test
    fun overrideIsDropped_whenDatabaseCatchesUp() = runTest(UnconfinedTestDispatcher()) {
        val rawSnapshot = MutableStateFlow(
            snapshot(
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
        advanceUntilIdle()

        rawSnapshot.value = snapshot(
            conversationItem(
                conversationId = "a",
                isRead = true,
            ),
        )
        advanceUntilIdle()

        assertTrue(delegate.snapshot.value?.items?.single()?.latestMessage?.isRead == true)
    }

    @Test
    fun bind_isIdempotent() = runTest(UnconfinedTestDispatcher()) {
        val delegate = bindDelegate(snapshot("a"))

        delegate.bind(TestScope())
        advanceUntilIdle()

        assertEquals(listOf("a"), delegate.conversationIds())
    }

    private fun TestScope.bindDelegate(
        rawSnapshot: ConversationListSnapshot,
    ): ConversationListOptimisticSnapshotDelegateImpl {
        return bindDelegate(MutableStateFlow(rawSnapshot))
    }

    private fun TestScope.bindDelegate(
        rawSnapshot: MutableStateFlow<ConversationListSnapshot>,
    ): ConversationListOptimisticSnapshotDelegateImpl {
        val repository = mockk<ConversationListRepository>(relaxed = true)
        every { repository.observeInboxSnapshot() } returns rawSnapshot

        return ConversationListOptimisticSnapshotDelegateImpl(
            repository = repository,
            reducer = ConversationListOptimisticReducer(),
        ).apply {
            bind(backgroundScope)
        }
    }

    private fun ConversationListOptimisticSnapshotDelegateImpl.conversationIds(): List<String> {
        return snapshot.value
            ?.items
            ?.map(ConversationListItem::conversationId)
            .orEmpty()
    }
}
