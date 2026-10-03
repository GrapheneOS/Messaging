package com.android.messaging.ui.conversationlist.chats.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.appsettings.model.ConversationSwipeSettings
import com.android.messaging.ui.conversationlist.chats.mapper.toSwipeSpec
import com.android.messaging.ui.conversationlist.common.list.ConversationListSwipeSpec
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState

@Immutable
internal data class ConversationListUiState(
    val content: ConversationListContentUiState = ConversationListContentUiState.Loading,
    val selection: ConversationListSelectionUiState = ConversationListSelectionUiState(),
    val isScrollToTopVisible: Boolean = false,
    val hasBlockedParticipants: Boolean = false,
    val isDebugEnabled: Boolean = false,
    val swipeSpec: ConversationListSwipeSpec = ConversationSwipeSettings.Default.toSwipeSpec(),
)
