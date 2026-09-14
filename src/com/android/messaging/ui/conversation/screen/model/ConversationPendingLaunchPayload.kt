package com.android.messaging.ui.conversation.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment

@Immutable
internal data class ConversationPendingLaunchPayload(
    val draft: ConversationDraft? = null,
    val scrollPosition: Int? = null,
    val selfParticipantId: ParticipantId? = null,
    val startupAttachment: ConversationEntryStartupAttachment? = null,
)
