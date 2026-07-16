package com.android.messaging.ui.conversation.entry.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.draft.ConversationDraft

@Immutable
internal data class ConversationEntryUiState(
    val launchGeneration: Int? = null,
    val conversationId: ConversationId? = null,
    val pendingDraft: ConversationDraft? = null,
    val pendingScrollPosition: Int? = null,
    val pendingSelfParticipantId: String? = null,
    val pendingStartupAttachment: ConversationEntryStartupAttachment? = null,
)

@Immutable
internal data class ConversationEntryStartupAttachment(
    val contentType: String,
    val contentUri: String,
)
