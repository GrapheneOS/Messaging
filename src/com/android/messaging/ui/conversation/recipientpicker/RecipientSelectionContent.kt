@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.android.messaging.ui.conversation.recipientpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val searchFieldShape = RoundedCornerShape(size = 22.dp)

@Composable
internal fun RecipientSelectionContent(
    uiState: RecipientSelectionContentUiState,
    strings: RecipientSelectionStrings,
    rowDecorators: RecipientSelectionRowDecorators,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {},
    onPrimaryActionClick: () -> Unit = {},
    onQueryChanged: (String) -> Unit = {},
    onRecipientDestinationLongClick: OnRecipientDestinationAction? = null,
    topListContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(height = 16.dp))

            RecipientSelectionQueryField(
                query = uiState.picker.query,
                enabled = uiState.isQueryEnabled,
                prefixText = strings.queryPrefixText,
                placeholderText = strings.queryPlaceholderText,
                onQueryChanged = onQueryChanged,
            )

            Spacer(modifier = Modifier.height(height = 12.dp))

            RecipientSelectionContactsContent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                rowDecorators = rowDecorators,
                onLoadMore = onLoadMore,
                onPrimaryActionClick = onPrimaryActionClick,
                onRecipientDestinationClick = onRecipientDestinationClick,
                onRecipientDestinationLongClick = onRecipientDestinationLongClick,
                topListContent = topListContent,
            )
        }
    }
}

@Composable
private fun RecipientSelectionQueryField(
    query: String,
    enabled: Boolean,
    prefixText: String,
    placeholderText: String,
    onQueryChanged: (String) -> Unit,
) {
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = query,
        onValueChange = onQueryChanged,
        enabled = enabled,
        singleLine = true,
        shape = searchFieldShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        prefix = {
            Text(
                modifier = Modifier.padding(end = 12.dp),
                text = prefixText,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        placeholder = {
            Text(text = placeholderText)
        },
    )
}
