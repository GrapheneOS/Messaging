package com.android.messaging.ui.conversationlist.common.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.common.components.reorder.OverlayReorderAnimationController
import com.android.messaging.ui.common.components.reorder.rememberOverlayReorderAnimationController
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeKind
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationListItemsPinAnimationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var controller:
        OverlayReorderAnimationController<ConversationListItemUiModel, ConversationId>

    private var items by mutableStateOf(emptyList<ConversationListItemUiModel>())

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun pinAnimation_pinnedRowKeepsItsPosition_acceptsRowAsTarget() {
        setListContent(listOf(item(conversationId = "a"), item(conversationId = "b")))

        commitPinChange(conversationId = "a", isPinned = true)

        assertNotNull(currentTargetBounds())
    }

    @Test
    fun pinAnimation_unpinnedRowKeepsItsPosition_acceptsRowAsTarget() {
        setListContent(
            listOf(
                item(conversationId = "a", isPinned = true),
                item(conversationId = "b"),
            ),
        )

        commitPinChange(conversationId = "a", isPinned = false)

        assertNotNull(currentTargetBounds())
    }

    private fun setListContent(initialItems: List<ConversationListItemUiModel>) {
        items = initialItems

        composeTestRule.setContent {
            controller = rememberOverlayReorderAnimationController(
                key = ConversationListItemUiModel::conversationId,
                isSettled = { item, anchorToTop -> item.isPinned == anchorToTop },
            )

            AppTheme {
                ConversationListItems(
                    items = items.toImmutableList(),
                    restoredConversationIds = persistentSetOf(),
                    listState = rememberLazyListState(),
                    isSelectionMode = false,
                    scaffoldContentPadding = PaddingValues(),
                    fabBottomReserve = 0.dp,
                    pinAnimationController = controller,
                    swipeSpec = ConversationListSwipeSpec(
                        startToEnd = ConversationSwipeKind.ToggleRead,
                        endToStart = ConversationSwipeKind.Archive,
                    ),
                    onItemEvent = {},
                )
            }
        }
    }

    private fun commitPinChange(conversationId: String, isPinned: Boolean) {
        val changedConversationId = ConversationId(conversationId)

        composeTestRule.runOnIdle {
            controller.prepare(
                keys = listOf(changedConversationId),
                anchorToTop = isPinned,
                transform = { item -> item.copy(isPinned = isPinned) },
            )
            items = items.map { item ->
                when (item.conversationId) {
                    changedConversationId -> item.copy(isPinned = isPinned)
                    else -> item
                }
            }
            controller.markCommitted()
        }
        composeTestRule.waitForIdle()
    }

    private fun currentTargetBounds(): Rect? {
        return composeTestRule.runOnIdle {
            controller.animations.single().targetBounds
        }
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
