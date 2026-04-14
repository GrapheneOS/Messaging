package com.android.messaging.ui.conversation.v2.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.datamodel.data.MessageData

@Immutable
internal data class ConversationLaunchRequest(
    val launchGeneration: Int,
    val conversationId: String?,
    val draftData: MessageData? = null,
    val startupAttachmentUri: String? = null,
    val startupAttachmentType: String? = null,
)
