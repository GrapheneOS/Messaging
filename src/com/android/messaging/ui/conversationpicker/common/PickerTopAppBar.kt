package com.android.messaging.ui.conversationpicker.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.android.messaging.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PickerTopAppBar(
    isSearchActive: Boolean,
    inSelectionMode: Boolean,
    selectedCount: Int,
    searchState: TextFieldState,
    @StringRes title: Int,
    @StringRes searchHint: Int,
    onNavigateBack: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSelectionClear: () -> Unit,
) {
    TopAppBar(
        title = {
            PickerTopAppBarTitle(
                isSearchActive = isSearchActive,
                inSelectionMode = inSelectionMode,
                selectedCount = selectedCount,
                searchState = searchState,
                title = title,
                searchHint = searchHint,
            )
        },
        navigationIcon = {
            PickerNavigationIcon(
                isSearchActive = isSearchActive,
                inSelectionMode = inSelectionMode,
                onNavigateBack = onNavigateBack,
                onSearchClose = onSearchClose,
                onSelectionClear = onSelectionClear,
            )
        },
        actions = {
            PickerTopAppBarActions(
                isSearchActive = isSearchActive,
                inSelectionMode = inSelectionMode,
                searchState = searchState,
                onSearchOpen = onSearchOpen,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PickerReviewTopAppBar(
    @StringRes title: Int,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.share_cancel),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun PickerTopAppBarTitle(
    isSearchActive: Boolean,
    inSelectionMode: Boolean,
    selectedCount: Int,
    searchState: TextFieldState,
    @StringRes title: Int,
    @StringRes searchHint: Int,
) {
    when {
        isSearchActive -> {
            PickerSearchField(
                state = searchState,
                searchHint = searchHint,
            )
        }

        inSelectionMode -> {
            Text(
                text = stringResource(R.string.share_selection_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        else -> {
            Text(
                text = stringResource(id = title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PickerNavigationIcon(
    isSearchActive: Boolean,
    inSelectionMode: Boolean,
    onNavigateBack: () -> Unit,
    onSearchClose: () -> Unit,
    onSelectionClear: () -> Unit,
) {
    val onClick = when {
        isSearchActive -> onSearchClose
        inSelectionMode -> onSelectionClear
        else -> onNavigateBack
    }

    val imageVector = when {
        inSelectionMode && !isSearchActive -> Icons.Default.Close
        else -> Icons.AutoMirrored.Default.ArrowBack
    }

    val contentDescription = when {
        inSelectionMode && !isSearchActive -> stringResource(R.string.share_selection_clear)
        else -> stringResource(R.string.share_cancel)
    }

    IconButton(onClick = onClick) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun PickerTopAppBarActions(
    isSearchActive: Boolean,
    inSelectionMode: Boolean,
    searchState: TextFieldState,
    onSearchOpen: () -> Unit,
) {
    when {
        isSearchActive && searchState.text.isNotEmpty() -> {
            IconButton(onClick = { searchState.clearText() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.share_search_clear),
                )
            }
        }

        isSearchActive -> Unit

        inSelectionMode -> Unit

        else -> {
            IconButton(onClick = onSearchOpen) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.share_search),
                )
            }
        }
    }
}

@Composable
private fun PickerSearchField(
    state: TextFieldState,
    @StringRes searchHint: Int,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BasicTextField(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        decorator = { innerTextField ->
            Box {
                if (state.text.isEmpty()) {
                    Text(
                        text = stringResource(id = searchHint),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
        },
    )
}
