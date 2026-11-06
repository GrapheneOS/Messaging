package com.android.messaging.ui.conversationlist.common.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeKind
import com.android.messaging.ui.conversationlist.common.support.CONVERSATION_LIST_TEST_TAG
import com.android.messaging.ui.conversationlist.common.support.conversationListItemTestTag
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationListItemsScrollPositionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val conversationIds = mutableStateOf(emptyList<String>())
    private val restoredConversationIds = mutableStateOf<ImmutableSet<ConversationId>>(
        persistentSetOf(),
    )
    private lateinit var listState: LazyListState

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun visibleRowMovesToTop_listAtTop_rowIsShownFullyAtTheTop() {
        val rows = longListOfConversationIds()
        val movedRow = rows[1]

        setListContent(rows)

        conversationIds.value = listOf(movedRow) + rows.minus(movedRow)
        composeTestRule.waitForIdle()

        assertRowIsFullyAtTheTop(movedRow)
    }

    @Test
    fun rowBelowTheScreenMovesToTop_listAtTop_rowIsShownFullyAtTheTop() {
        val rows = longListOfConversationIds()
        val movedRow = rows.last()

        setListContent(rows)

        conversationIds.value = listOf(movedRow) + rows.dropLast(1)
        composeTestRule.waitForIdle()

        assertRowIsFullyAtTheTop(movedRow)
    }

    @Test
    fun archivedTopRowIsRestored_listAtTop_rowIsShownFullyAtTheTop() {
        val rows = longListOfConversationIds()
        val archivedRow = rows.first()

        setListContent(rows)

        conversationIds.value = rows.minus(archivedRow)
        composeTestRule.waitForIdle()

        restoredConversationIds.value = persistentSetOf(ConversationId(archivedRow))
        conversationIds.value = rows
        composeTestRule.waitForIdle()

        assertRowIsFullyAtTheTop(archivedRow)
    }

    @Test
    fun topRowIsArchived_listAtTop_nextRowIsShownFullyAtTheTop() {
        val rows = longListOfConversationIds()
        val archivedRow = rows.first()

        setListContent(rows)

        conversationIds.value = rows.minus(archivedRow)
        composeTestRule.waitForIdle()

        assertRowIsFullyAtTheTop(rows[1])
    }

    @Test
    fun rowMovesToTop_listScrolledDown_visibleRowsStayInPlace() {
        val rows = longListOfConversationIds()
        val movedRow = rows.last()

        setListContent(rows)
        composeTestRule.runOnIdle {
            listState.requestScrollToItem(index = 5, scrollOffset = 10)
        }
        composeTestRule.waitForIdle()
        val firstVisibleRow = firstVisibleConversationId()
        val firstVisibleRowTop = rowTop(firstVisibleRow)

        conversationIds.value = listOf(movedRow) + rows.dropLast(1)
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(firstVisibleRow, firstVisibleConversationId())
        }
        assertEquals(firstVisibleRowTop, rowTop(firstVisibleRow))
    }

    private fun assertRowIsFullyAtTheTop(conversationId: String) {
        val rowTop = rowTop(conversationId)
        val listTop = composeTestRule
            .onNodeWithTag(CONVERSATION_LIST_TEST_TAG)
            .getUnclippedBoundsInRoot()
            .top

        assertTrue(
            "Row $conversationId starts at $rowTop, above the top of the list at $listTop",
            rowTop >= listTop,
        )
        composeTestRule.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(0, listState.firstVisibleItemScrollOffset)
        }
    }

    private fun rowTop(conversationId: String): Dp {
        return composeTestRule
            .onNodeWithTag(conversationListItemTestTag(ConversationId(conversationId)))
            .getUnclippedBoundsInRoot()
            .top
    }

    private fun firstVisibleConversationId(): String {
        return listState.layoutInfo.visibleItemsInfo
            .first { visibleItem -> visibleItem.index == listState.firstVisibleItemIndex }
            .key as String
    }

    private fun setListContent(initialConversationIds: List<String>) {
        conversationIds.value = initialConversationIds

        composeTestRule.setContent {
            listState = rememberLazyListState()

            AppTheme {
                ConversationListItems(
                    items = conversationIds.value.map(::item).toImmutableList(),
                    restoredConversationIds = restoredConversationIds.value,
                    listState = listState,
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
        composeTestRule.waitForIdle()
    }

    private fun longListOfConversationIds(): List<String> {
        return (0 until LONG_LIST_SIZE).map { index -> "row$index" }
    }

    private fun item(conversationId: String): ConversationListItemUiModel {
        return previewConversationListItem(
            conversationId = ConversationId(conversationId),
            title = conversationId,
            snippetText = conversationId,
        )
    }

    private companion object {
        const val LONG_LIST_SIZE = 40
    }
}
