package com.android.messaging.ui.search.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.R
import com.android.messaging.ui.conversation.navigation.rememberConversationNavigator
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.paneTitleMetadata
import com.android.messaging.ui.search.SearchScreen
import com.android.messaging.ui.search.SearchViewModel

internal fun EntryProviderScope<NavKey>.searchEntries() {
    entry<SearchNavKey>(
        metadata = paneTitleMetadata(R.string.search),
        content = searchRouteContent(),
    )
}

private fun searchRouteContent(): @Composable (SearchNavKey) -> Unit {
    return {
        val appNavigator = LocalNavigator.current
        val conversationNavigator = rememberConversationNavigator()
        val screenModel = hiltViewModel<SearchViewModel>()

        SearchScreen(
            screenModel = screenModel,
            onNavigateBack = appNavigator::back,
            onNavigateToConversation = { conversationId ->
                conversationNavigator.navigateToConversation(conversationId = conversationId)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
