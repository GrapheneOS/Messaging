package com.android.messaging.ui.conversationsettings.navigation

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.navigation.ConversationScopedNavKey
import kotlinx.serialization.Serializable

@Serializable
internal data class ConversationSettingsNavKey(
    override val conversationId: ConversationId,
) : ConversationScopedNavKey
