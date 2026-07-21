package com.android.messaging.ui.conversation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversationsettings.navigation.ConversationSettingsNavKey
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.Navigator

@Stable
internal interface ConversationNavigator {

    fun navigateToAddParticipants(conversationId: ConversationId)

    fun navigateToConversation(conversationId: ConversationId)

    fun navigateToNewChat()

    fun navigateToMessageDetails(
        conversationId: ConversationId,
        messageId: MessageId,
    )

    fun navigateToRecipientPicker(mode: RecipientPickerMode)

    fun navigateToConversationSettings(conversationId: ConversationId)

    fun replaceCurrentConversation(conversationId: ConversationId)

    fun closeConversation(conversationId: ConversationId)

    fun back()
}

internal class ConversationNavigatorImpl(
    private val navigator: Navigator,
) : ConversationNavigator {

    private val backStack: MutableList<NavKey>
        get() = navigator.backStack

    override fun navigateToAddParticipants(conversationId: ConversationId) {
        navigator.push(destination = AddParticipantsNavKey(conversationId = conversationId))
    }

    override fun navigateToConversation(conversationId: ConversationId) {
        removeTrailingConversationEntryDestinations()

        navigator.push(destination = ConversationNavKey(conversationId = conversationId))
    }

    override fun navigateToNewChat() {
        navigator.push(destination = NewChatNavKey)
    }

    override fun navigateToMessageDetails(
        conversationId: ConversationId,
        messageId: MessageId,
    ) {
        navigator.push(
            destination = MessageDetailsNavKey(
                conversationId = conversationId,
                messageId = messageId,
            ),
        )
    }

    override fun navigateToRecipientPicker(mode: RecipientPickerMode) {
        navigator.push(destination = RecipientPickerNavKey(mode = mode))
    }

    override fun navigateToConversationSettings(conversationId: ConversationId) {
        navigator.push(
            destination = ConversationSettingsNavKey(conversationId = conversationId),
        )
    }

    override fun replaceCurrentConversation(conversationId: ConversationId) {
        if (backStack.lastOrNull() is AddParticipantsNavKey) {
            backStack.removeAt(backStack.lastIndex)
        }

        val updatedConversation = ConversationNavKey(conversationId = conversationId)
        val currentConversationIndex = backStack.indexOfLast { navKey ->
            navKey is ConversationNavKey
        }

        if (currentConversationIndex >= 0) {
            backStack[currentConversationIndex] = updatedConversation
            return
        }

        backStack.add(updatedConversation)
    }

    override fun closeConversation(conversationId: ConversationId) {
        val remainingDestinations = backStack.dropLastWhile { navKey ->
            navKey.belongsToConversation(conversationId)
        }

        if (remainingDestinations.isEmpty()) {
            navigator.finish()
            return
        }

        navigator.reset(destinations = remainingDestinations)
    }

    override fun back() {
        navigator.back()
    }

    private fun NavKey.belongsToConversation(conversationId: ConversationId): Boolean {
        return this is ConversationScopedNavKey && this.conversationId == conversationId
    }

    private fun removeTrailingConversationEntryDestinations() {
        while (backStack.lastOrNull().isConversationEntryDestination()) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    private fun NavKey?.isConversationEntryDestination(): Boolean {
        return when (this) {
            is NewChatNavKey -> true
            is RecipientPickerNavKey -> true
            else -> false
        }
    }
}

@Composable
internal fun rememberConversationNavigator(): ConversationNavigator {
    val navigator = LocalNavigator.current

    return remember(navigator) {
        ConversationNavigatorImpl(navigator = navigator)
    }
}
