package com.android.messaging.ui.recipientselection.component.row

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.recipientselection.component.MultiDestinationContactRow
import com.android.messaging.ui.recipientselection.component.RecipientSelectionContactRow
import com.android.messaging.ui.recipientselection.component.RecipientSelectionContactsContent
import com.android.messaging.ui.recipientselection.component.recipientSelectionContactRowShape
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.selection.OnRecipientDestinationAction
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionRowDecorators
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
                RecipientSelectionContactsContent(
                    uiState = uiState,
                    rowDecorators = rowDecorators,
                    onRecipientDestinationClick = onContentDestinationClick,
                    onLoadMore = onLoadMore,
                    onPrimaryActionClick = onPrimaryActionClick,
                    onRecipientDestinationLongClick = onRecipientDestinationLongClick,
                    emptyStateText = R.string.contact_list_empty_text,
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
