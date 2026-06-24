package com.android.messaging.ui.conversationlist.ui

import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationListContentTest {

    @Test
    fun resolvePinChangeScrollRequest_noPinChange_returnsNull() {
        val items = listOf(item("a"), item("b"))

        val result = resolvePinChangeScrollRequest(
            previousItems = items,
            currentItems = items,
            firstVisibleConversationId = "b",
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 12,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_conversationSetChanged_returnsNull() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item("a"), item("b")),
            currentItems = listOf(item("a", isPinned = true), item("c")),
            firstVisibleConversationId = "a",
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_atStart_requestsFirstItem() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item("a"), item("b")),
            currentItems = listOf(item("b", isPinned = true), item("a")),
            firstVisibleConversationId = "a",
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )

        assertEquals(
            ConversationListScrollRequest(
                index = 0,
                scrollOffset = 0,
            ),
            result,
        )
    }

    @Test
    fun resolvePinChangeScrollRequest_firstVisibleItemPinned_preservesPreviousPosition() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item("a"), item("b"), item("c")),
            currentItems = listOf(item("b", isPinned = true), item("a"), item("c")),
            firstVisibleConversationId = "b",
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 24,
        )

        assertEquals(
            ConversationListScrollRequest(
                index = 1,
                scrollOffset = 24,
            ),
            result,
        )
    }

    @Test
    fun resolvePinChangeScrollRequest_otherItemPinned_returnsNull() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item("a"), item("b"), item("c")),
            currentItems = listOf(item("a", isPinned = true), item("b"), item("c")),
            firstVisibleConversationId = "b",
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 24,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_firstVisibleItemUnknown_returnsNull() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item("a"), item("b")),
            currentItems = listOf(item("b", isPinned = true), item("a")),
            firstVisibleConversationId = null,
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 24,
        )

        assertNull(result)
    }

    private fun item(
        conversationId: String,
        isPinned: Boolean = false,
    ): ConversationListItemUiModel {
        return previewConversationListItem(
            conversationId = conversationId,
            title = conversationId,
            snippetText = conversationId,
            isPinned = isPinned,
        )
    }
}
