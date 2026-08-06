package com.android.messaging.ui.conversation.entry.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.data.MessageData

@Immutable
internal data class ConversationEntryLaunchRequest(
    val launchGeneration: Int,
    val conversationId: ConversationId?,
    val draftData: MessageData? = null,
    val startupAttachmentUri: String? = null,
    val startupAttachmentType: String? = null,
    val messagePosition: Int? = null,
    val isLaunchedFromBubble: Boolean = false,
)
