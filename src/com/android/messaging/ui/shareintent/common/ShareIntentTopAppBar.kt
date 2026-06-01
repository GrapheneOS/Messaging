package com.android.messaging.ui.shareintent.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
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
internal fun ShareIntentTopAppBar(
    isSearchActive: Boolean,
    inSelectionMode: Boolean,
    selectedCount: Int,
    searchState: TextFieldState,
    onNavigateBack: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSelectionClear: () -> Unit,
    onSendToSelected: () -> Unit,
) {
    TopAppBar(
        title = {
            ShareIntentTopAppBarTitle(
                isSearchActive = isSearchActive,
                inSelectionMode = inSelectionMode,
                selectedCount = selectedCount,
                searchState = searchState,
            )
        },
        navigationIcon = {
            ShareIntentNavigationIcon(
                isSearchActive = isSearchActive,
                inSelectionMode = inSelectionMode,
                onNavigateBack = onNavigateBack,
                onSearchClose = onSearchClose,
                onSelectionClear = onSelectionClear,
            )
        },
        actions = {
            ShareIntentTopAppBarActions(
                isSearchActive = isSearchActive,
                inSelectionMode = inSelectionMode,
                searchState = searchState,
                onSearchOpen = onSearchOpen,
                onSendToSelected = onSendToSelected,
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

@Composable
private fun ShareIntentTopAppBarTitle(
    isSearchActive: Boolean,
    inSelectionMode: Boolean,
    selectedCount: Int,
    searchState: TextFieldState,
) {
    when {
        inSelectionMode -> {
            Text(
                text = stringResource(R.string.share_selection_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        isSearchActive -> {
            ShareIntentSearchField(state = searchState)
        }

        else -> {
            Text(
                text = stringResource(R.string.share_intent_activity_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ShareIntentNavigationIcon(
    isSearchActive: Boolean,
    inSelectionMode: Boolean,
    onNavigateBack: () -> Unit,
    onSearchClose: () -> Unit,
    onSelectionClear: () -> Unit,
) {
    val onClick = when {
        inSelectionMode -> onSelectionClear
        isSearchActive -> onSearchClose
        else -> onNavigateBack
    }

    val imageVector = when {
        inSelectionMode -> Icons.Outlined.Close
        else -> Icons.AutoMirrored.Outlined.ArrowBack
    }

    val contentDescription = when {
        inSelectionMode -> stringResource(R.string.share_selection_clear)
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
private fun ShareIntentTopAppBarActions(
    isSearchActive: Boolean,
    inSelectionMode: Boolean,
    searchState: TextFieldState,
    onSearchOpen: () -> Unit,
    onSendToSelected: () -> Unit,
) {
    when {
        inSelectionMode -> {
            IconButton(onClick = onSendToSelected) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = stringResource(R.string.share_selection_send),
                )
            }
        }

        isSearchActive && searchState.text.isNotEmpty() -> {
            IconButton(onClick = { searchState.clearText() }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.share_search_clear),
                )
            }
        }

        !isSearchActive -> {
            IconButton(onClick = onSearchOpen) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.share_search),
                )
            }
        }
    }
}

@Composable
private fun ShareIntentSearchField(
    state: TextFieldState,
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
                        text = stringResource(R.string.share_search_hint),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}
