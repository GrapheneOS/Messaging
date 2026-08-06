package com.android.messaging.ui.conversationlist.common.list

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationListItemsTest {

    @Test
    fun resolvePinChangeScrollRequest_noPinChange_returnsNull() {
        val items = listOf(item(ConversationId("a")), item(ConversationId("b")))

        val result = resolvePinChangeScrollRequest(
            previousItems = items,
            currentItems = items,
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = ConversationId("b"),
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 12,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_conversationSetChanged_returnsNull() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item(ConversationId("a")), item(ConversationId("b"))),
            currentItems = listOf(
                item(ConversationId("a"), isPinned = true),
                item(ConversationId("c"))
            ),
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = ConversationId("a"),
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_newTopItemWhileAtStart_requestsFirstItem() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item(ConversationId("a")), item(ConversationId("b"))),
            currentItems = listOf(
                item(ConversationId("c")),
                item(ConversationId("a")),
                item(ConversationId("b"))
            ),
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = ConversationId("a"),
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
            previousItems = listOf(item(ConversationId("a")), item(ConversationId("b"))),
            currentItems = listOf(
                item(ConversationId("c")),
                item(ConversationId("a")),
                item(ConversationId("b"))
            ),
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = ConversationId("b"),
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffset = 10,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_restoredTopItemWhileAtStart_returnsNull() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item(ConversationId("a")), item(ConversationId("b"))),
            currentItems = listOf(
                item(ConversationId("c")),
                item(ConversationId("a")),
                item(ConversationId("b"))
            ),
            restoredConversationIds = setOf(ConversationId("c")),
            firstVisibleConversationId = ConversationId("a"),
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_atStart_requestsFirstItem() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item(ConversationId("a")), item(ConversationId("b"))),
            currentItems = listOf(
                item(ConversationId("b"), isPinned = true),
                item(ConversationId("a"))
            ),
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = ConversationId("a"),
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
            previousItems = listOf(
                item(ConversationId("a")),
                item(ConversationId("b")),
                item(ConversationId("c"))
            ),
            currentItems = listOf(
                item(ConversationId("b"), isPinned = true),
                item(ConversationId("a")),
                item(ConversationId("c"))
            ),
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = ConversationId("b"),
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
            previousItems = listOf(
                item(ConversationId("a")),
                item(ConversationId("b")),
                item(ConversationId("c"))
            ),
            currentItems = listOf(
                item(ConversationId("a"), isPinned = true),
                item(ConversationId("b")),
                item(ConversationId("c"))
            ),
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = ConversationId("b"),
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 24,
        )

        assertNull(result)
    }

    @Test
    fun resolvePinChangeScrollRequest_firstVisibleItemUnknown_returnsNull() {
        val result = resolvePinChangeScrollRequest(
            previousItems = listOf(item(ConversationId("a")), item(ConversationId("b"))),
            currentItems = listOf(
                item(ConversationId("b"), isPinned = true),
                item(ConversationId("a"))
            ),
            restoredConversationIds = emptySet(),
            firstVisibleConversationId = null,
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 24,
        )

        assertNull(result)
    }

    private fun item(
        conversationId: ConversationId,
        isPinned: Boolean = false,
    ): ConversationListItemUiModel {
        return previewConversationListItem(
            conversationId = conversationId,
            title = conversationId.value,
            snippetText = conversationId.value,
            isPinned = isPinned,
        )
    }
}
