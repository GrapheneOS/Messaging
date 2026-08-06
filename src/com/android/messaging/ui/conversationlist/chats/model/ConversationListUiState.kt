package com.android.messaging.ui.conversationlist.chats.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.common.components.participant.PhoneNumberCopyTarget
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Immutable
internal data class ConversationListUiState(
    val content: ConversationListContentUiState = ConversationListContentUiState.Loading,
    val selection: ConversationListSelectionUiState = ConversationListSelectionUiState(),
    val isScrollToTopVisible: Boolean = false,
    val hasBlockedParticipants: Boolean = false,
    val isDebugEnabled: Boolean = false,
    val phoneNumberCopyTargets: ImmutableMap<String, ImmutableList<PhoneNumberCopyTarget>> =
        persistentMapOf(),
)
