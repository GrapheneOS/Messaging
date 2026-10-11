package com.android.messaging.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.search.model.MessageSearchResult
import com.android.messaging.ui.common.components.TopAppBarSearchField
import com.android.messaging.util.Dates
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    screenModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToConversation: (ConversationId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchState = rememberTextFieldState()
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateToConversation by rememberUpdatedState(onNavigateToConversation)

    LaunchedEffect(screenModel) {
        screenModel.navigationEvents.collect(currentOnNavigateToConversation)
    }

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect(screenModel::onQueryChanged)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchTopAppBar(
                searchState = searchState,
                onNavigateBack = onNavigateBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { paddingValues ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)

        when (val state = uiState) {
            SearchViewModel.UiState.Idle,
            SearchViewModel.UiState.Searching,
            -> Box(modifier = contentModifier)

            is SearchViewModel.UiState.Results -> {
                if (state.results.isEmpty()) {
                    SearchEmptyState(modifier = contentModifier)
                } else {
                    SearchResultsList(
                        results = state.results,
                        onResultClick = screenModel::onResultClicked,
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopAppBar(
    searchState: TextFieldState,
    onNavigateBack: () -> Unit,
) {
    TopAppBar(
        title = {
            TopAppBarSearchField(
                state = searchState,
                searchHint = R.string.search_messages_hint,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.back),
                )
            }
        },
        actions = {
            if (searchState.text.isNotEmpty()) {
                IconButton(onClick = { searchState.clearText() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.search_clear),
                    )
                }
            }
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
private fun SearchEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.search_no_results),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchResultsList(
    results: ImmutableList<MessageSearchResult>,
    onResultClick: (MessageSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items = results, key = { result -> result.messageId.value }) { result ->
            SearchResultRow(
                result = result,
                onClick = { onResultClick(result) },
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    result: MessageSearchResult,
    onClick: () -> Unit,
) {
    val subtitle = listOfNotNull(
        result.senderName?.takeIf { it.isNotBlank() },
        result.conversationName?.takeIf { it.isNotBlank() },
    ).joinToString(separator = " · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f),
                text = result.snippet,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = Dates.getConversationTimeString(result.timestamp).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
