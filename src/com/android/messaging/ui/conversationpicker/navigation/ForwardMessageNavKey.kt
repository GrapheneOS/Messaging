package com.android.messaging.ui.conversationpicker.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import kotlinx.serialization.Serializable

@Serializable
internal data class ForwardMessageNavKey(
    val conversationId: ConversationId,
    val messageId: MessageId,
) : NavKey
