package com.android.messaging.ui.conversation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data object NewChatNavKey : NavKey

@Serializable
internal data class ConversationNavKey(
    val conversationId: String,
) : NavKey

@Serializable
internal data class RecipientPickerNavKey(
    val mode: RecipientPickerMode,
) : NavKey

@Serializable
internal data class AddParticipantsNavKey(
    val conversationId: String,
) : NavKey

@Serializable
internal enum class RecipientPickerMode {
    CREATE_GROUP,
    ADD_PARTICIPANTS,
}
