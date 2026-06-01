package com.android.messaging.ui.conversation.recipientpicker.component.row

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.recipientpicker.component.MultiDestinationContactRow
import com.android.messaging.ui.conversation.recipientpicker.component.RecipientSelectionContactRow
import com.android.messaging.ui.conversation.recipientpicker.component.RecipientSelectionContent
import com.android.messaging.ui.conversation.recipientpicker.component.recipientSelectionContactRowShape
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerListItem
import com.android.messaging.ui.conversation.recipientpicker.model.selection.OnRecipientDestinationAction
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionRowDecorators
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionStrings
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Before
import org.junit.Rule

internal abstract class BaseRecipientSelectionContactRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    protected val onContentDestinationClick = mockk<OnRecipientDestinationAction>(relaxed = true)
    protected val onContentDestinationLongClick = mockk<OnRecipientDestinationAction>(
        relaxed = true,
    )
    protected val onLoadMore = mockk<() -> Unit>(relaxed = true)
    protected val onPrimaryActionClick = mockk<() -> Unit>(relaxed = true)
    protected val onRowDestinationClick = mockk<(String) -> Unit>(relaxed = true)
    protected val onRowDestinationLongClick = mockk<(String) -> Unit>(relaxed = true)

    @Before
    fun setUpBaseRecipientSelectionContactRowTest() {
        clearAllMocks()
    }

    protected fun setMultiDestinationRowContent(
        item: RecipientPickerListItem.Contact = multiDestinationContactItem(),
        enabled: Boolean = true,
        selectedDestinations: ImmutableSet<String> = persistentSetOf(),
        rowDecorators: RecipientSelectionRowDecorators = defaultRowDecorators(),
        onDestinationLongClick: ((String) -> Unit)? = onRowDestinationLongClick,
    ) {
        composeTestRule.setContent {
            AppTheme {
                MultiDestinationContactRow(
                    item = item,
                    enabled = enabled,
                    selectedDestinations = selectedDestinations,
                    onDestinationClick = onRowDestinationClick,
                    onDestinationLongClick = onDestinationLongClick,
                    shape = RoundedCornerShape(size = 18.dp),
                    rowDecorators = rowDecorators,
                )
            }
        }
    }

    protected fun setContactRowContent(
        item: RecipientPickerListItem,
        enabled: Boolean = true,
        selectedDestinations: ImmutableSet<String> = persistentSetOf(),
        rowDecorators: RecipientSelectionRowDecorators = defaultRowDecorators(),
        onDestinationLongClick: ((String) -> Unit)? = onRowDestinationLongClick,
    ) {
        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionContactRow(
                    item = item,
                    enabled = enabled,
                    selectedDestinations = selectedDestinations,
                    onDestinationClick = onRowDestinationClick,
                    onDestinationLongClick = onDestinationLongClick,
                    shape = recipientSelectionContactRowShape(index = 0, totalCount = 1),
                    rowDecorators = rowDecorators,
                )
            }
        }
    }

    protected fun setSelectionContent(
        uiState: RecipientSelectionContentUiState,
        rowDecorators: RecipientSelectionRowDecorators = defaultRowDecorators(),
        onRecipientDestinationLongClick: OnRecipientDestinationAction? =
            onContentDestinationLongClick,
    ) {
        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionContent(
                    uiState = uiState,
                    strings = RecipientSelectionStrings(
                        queryPrefixText = targetContext.getString(R.string.to_address_label),
                        queryPlaceholderText = targetContext.getString(
                            R.string.new_chat_query_hint,
                        ),
                    ),
                    rowDecorators = rowDecorators,
                    onRecipientDestinationClick = onContentDestinationClick,
                    onLoadMore = onLoadMore,
                    onPrimaryActionClick = onPrimaryActionClick,
                    onQueryChanged = {},
                    onRecipientDestinationLongClick = onRecipientDestinationLongClick,
                )
            }
        }
    }

    protected fun defaultRowDecorators(
        showTrailingIndicator: (RecipientPickerListItem, String) -> Boolean = { _, _ -> false },
    ): RecipientSelectionRowDecorators {
        return RecipientSelectionRowDecorators(
            recipientRowTestTag = { item ->
                recipientRowTestTag(item = item)
            },
            destinationRowTestTag = { item, destination ->
                destinationRowTestTag(item = item, destination = destination)
            },
            showRecipientTrailingIndicator = showTrailingIndicator,
            trailingIndicatorTestTag = TRAILING_INDICATOR_TEST_TAG,
        )
    }
}
