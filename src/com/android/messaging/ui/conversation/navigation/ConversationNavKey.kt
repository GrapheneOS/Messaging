package com.android.messaging.ui.conversation.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.navigation.ConversationScopedNavKey
import kotlinx.serialization.Serializable

@Serializable
internal data object NewChatNavKey : NavKey

@Serializable
internal data class ConversationNavKey(
    override val conversationId: ConversationId,
) : ConversationScopedNavKey

@Serializable
internal data class RecipientPickerNavKey(
    val mode: RecipientPickerMode,
) : NavKey

@Serializable
internal data class AddParticipantsNavKey(
    override val conversationId: ConversationId,
) : ConversationScopedNavKey

@Serializable
internal data class MessageDetailsNavKey(
    override val conversationId: ConversationId,
    val messageId: MessageId,
) : ConversationScopedNavKey

@Serializable
internal enum class RecipientPickerMode {
    CREATE_GROUP,
    ADD_PARTICIPANTS,
}
