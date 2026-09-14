package com.android.messaging.ui.conversationlist.common.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeKind
import com.android.messaging.ui.conversationlist.common.support.conversationListItemTestTag
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationListItemsTraversalOrderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun rowsAreTraversedInListOrderWhenNothingIsPinned() {
        val items = listOf(
            item(conversationId = "a"),
            item(conversationId = "b"),
            item(conversationId = "c"),
        )

        setListContent(items)

        assertEquals(tagsOf(items), traversedRowTags(items))
    }

    @Test
    fun pinnedRowIsTraversedFirstJustAsItIsRenderedFirst() {
        val items = listOf(
            item(conversationId = "c", isPinned = true),
            item(conversationId = "a"),
            item(conversationId = "b"),
        )

        setListContent(items)

        assertEquals(tagsOf(items), traversedRowTags(items))
    }

    @Test
    fun pinnedRowsKeepTheirOrderAmongThemselvesAndStayAheadOfTheRest() {
        val items = listOf(
            item(conversationId = "c", isPinned = true),
            item(conversationId = "b", isPinned = true),
            item(conversationId = "a"),
        )

        setListContent(items)

        assertEquals(tagsOf(items), traversedRowTags(items))
    }

    private fun setListContent(items: List<ConversationListItemUiModel>) {
        composeTestRule.setContent {
            AppTheme {
                ConversationListItems(
                    items = items.toImmutableList(),
                    restoredConversationIds = persistentSetOf(),
                    listState = rememberLazyListState(),
                    isSelectionMode = false,
                    scaffoldContentPadding = PaddingValues(),
                    fabBottomReserve = 0.dp,
                    pinAnimationController = null,
                    swipeSpec = ConversationListSwipeSpec(
                        startToEnd = ConversationSwipeKind.ToggleRead,
                        endToStart = ConversationSwipeKind.Archive,
                    ),
                    onItemEvent = {},
                )
            }
        }
    }

    /** The row test tags in the order the semantics tree hands them out. */
    private fun traversedRowTags(items: List<ConversationListItemUiModel>): List<String> {
        val rowTags = tagsOf(items).toSet()
        val isConversationRow = SemanticsMatcher("is a conversation row") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag) in rowTags
        }

        return composeTestRule
            .onAllNodes(isConversationRow)
            .fetchSemanticsNodes()
            .map { node -> node.config[SemanticsProperties.TestTag] }
    }

    private fun tagsOf(items: List<ConversationListItemUiModel>): List<String> {
        return items.map { item -> conversationListItemTestTag(item.conversationId) }
    }

    private fun item(
        conversationId: String,
        isPinned: Boolean = false,
    ): ConversationListItemUiModel {
        return previewConversationListItem(
            conversationId = ConversationId(conversationId),
            title = conversationId,
            snippetText = conversationId,
            isPinned = isPinned,
        )
    }
}
