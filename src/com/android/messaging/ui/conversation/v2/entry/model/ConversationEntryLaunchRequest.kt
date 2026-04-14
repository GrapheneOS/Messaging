package com.android.messaging.ui.conversation.v2.entry.model

import androidx.compose.runtime.Immutable
import com.android.messaging.datamodel.data.MessageData

@Immutable
internal data class ConversationEntryLaunchRequest(
    val launchGeneration: Int,
    val conversationId: String?,
    val draftData: MessageData? = null,
    val startupAttachmentUri: String? = null,
    val startupAttachmentType: String? = null,
)
