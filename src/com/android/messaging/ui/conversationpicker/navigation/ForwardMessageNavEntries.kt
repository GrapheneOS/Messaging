package com.android.messaging.ui.conversationpicker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.R
import com.android.messaging.ui.conversation.navigation.ConversationNavKey
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.conversationpicker.ConversationPickerScreen
import com.android.messaging.ui.conversationpicker.ConversationPickerViewModel
import com.android.messaging.ui.conversationpicker.host.forward.ForwardMessageNavEvent
import com.android.messaging.ui.conversationpicker.host.forward.ForwardMessageViewModel
import com.android.messaging.ui.conversationpicker.host.forward.rememberForwardMessageEffectHandler
import com.android.messaging.ui.conversationpicker.model.ConversationPickerLabels
import com.android.messaging.ui.core.CollectEvents
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.SeededViewModelStoreOwner
import com.android.messaging.ui.navigation.paneTitleMetadata
import com.android.messaging.util.UiUtils

internal fun EntryProviderScope<NavKey>.forwardMessageEntries() {
    entry<ForwardMessageNavKey>(
        metadata = paneTitleMetadata(R.string.forward_message_activity_title),
        content = forwardMessageRouteContent(),
    )
}

private fun forwardMessageRouteContent(): @Composable (ForwardMessageNavKey) -> Unit {
    return { navKey ->
        val navigator = LocalNavigator.current
        val defaultArgs = remember(navKey) {
            forwardMessageDefaultArgs(navKey = navKey)
        }

        SeededViewModelStoreOwner(defaultArgs = defaultArgs) {
            val screenModel = hiltViewModel<ForwardMessageViewModel>()
            val uiState by screenModel.uiState.collectAsStateWithLifecycle()
            val effectHandler = rememberForwardMessageEffectHandler(
                onTargetSelected = screenModel::onTargetSelected,
                onSendToSelected = { targets, draft ->
                    screenModel.onSendToTargets(
                        targets = targets,
                        draft = draft,
                    ) {
                        UiUtils.showToastAtBottom(R.string.send_message_failure)
                    }
                },
            )

            CollectEvents(events = screenModel.navigationEvents) { event ->
                when (event) {
                    is ForwardMessageNavEvent.OpenConversation -> {
                        navigator.reset(
                            destinations = listOf(
                                ConversationListNavKey,
                                ConversationNavKey(conversationId = event.conversationId),
                            ),
                        )
                    }

                    is ForwardMessageNavEvent.Close -> {
                        navigator.reset(
                            destinations = listOf(ConversationListNavKey),
                        )
                    }
                }
            }

            ConversationPickerScreen(
                screenModel = hiltViewModel<ConversationPickerViewModel>(),
                isInitialDraftLoading = uiState.isLoading,
                initialDraft = uiState.draft,
                effectHandler = effectHandler,
                onNavigateBack = navigator::back,
                allowMultiSelect = true,
                labels = ConversationPickerLabels.Forward,
            )
        }
    }
}
