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
            restoredConversationIds = emptySet(),
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
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = "a",
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_newTopItemWhileAtStart_requestsFirstItem() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item("a"), item("b")),
            currentItems = listOf(item("c"), item("a"), item("b")),
            restoredConversationIds = emptySet(),
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
    fun resolvePinChangeScrollRequest_newTopItemWhileScrolled_returnsNull() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item("a"), item("b")),
            currentItems = listOf(item("c"), item("a"), item("b")),
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = "b",
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffset = 10,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_restoredTopItemWhileAtStart_returnsNull() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item("a"), item("b")),
            currentItems = listOf(item("c"), item("a"), item("b")),
            restoredConversationIds = setOf("c"),
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
            restoredConversationIds = emptySet(),
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
            restoredConversationIds = emptySet(),
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
            restoredConversationIds = emptySet(),
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
            restoredConversationIds = emptySet(),
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
