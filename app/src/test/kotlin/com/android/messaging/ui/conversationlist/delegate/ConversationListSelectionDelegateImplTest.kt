package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.ui.conversationlist.snapshotOfIds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListSelectionDelegateImplTest {

    private val delegate = ConversationListSelectionDelegateImpl()

    @Test
    fun toggle_addsThenRemovesConversation() {
        delegate.toggle(ConversationId("a"))
        delegate.toggle(ConversationId("b"))

        assertEquals(listOf("a", "b"), delegate.selectedIds.value.map { it.value })

        delegate.toggle(ConversationId("a"))

        assertEquals(listOf("b"), delegate.selectedIds.value.map { it.value })
    }

    @Test
    fun toggle_blankConversationId_isIgnored() {
        delegate.toggle(ConversationId("   "))

        assertTrue(delegate.selectedIds.value.isEmpty())
    }

    @Test
    fun clear_removesAllSelection() {
        delegate.toggle(ConversationId("a"))
        delegate.toggle(ConversationId("b"))

        delegate.clear()

        assertTrue(delegate.selectedIds.value.isEmpty())
    }

    @Test
    fun bind_dropsSelectionForConversationsMissingFromSnapshot() = runTest {
        val snapshot = MutableStateFlow<ConversationListSnapshot?>(snapshotOfIds("a", "b"))
        delegate.bind(backgroundScope, snapshot)
        runCurrent()

        delegate.toggle(ConversationId("a"))
        delegate.toggle(ConversationId("b"))

        snapshot.value = snapshotOfIds("a")
        runCurrent()

        assertEquals(listOf("a"), delegate.selectedIds.value.map { it.value })
    }
}
