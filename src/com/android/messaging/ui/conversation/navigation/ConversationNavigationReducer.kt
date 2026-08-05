package com.android.messaging.ui.conversation.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.navigation.NavigationReducer
import com.android.messaging.ui.navigation.NavigationReducerImpl

internal interface ConversationNavigationReducer {
    fun navigateToAddParticipants(
        backStack: MutableList<NavKey>,
        conversationId: ConversationId,
    )

    fun navigateToConversation(
        backStack: MutableList<NavKey>,
        conversationId: ConversationId,
    )

    fun navigateToMessageDetails(
        backStack: MutableList<NavKey>,
        conversationId: ConversationId,
        messageId: MessageId,
    )

    fun navigateToRecipientPicker(
        backStack: MutableList<NavKey>,
        mode: RecipientPickerMode,
    )

    fun popBackStack(backStack: MutableList<NavKey>): Boolean

    fun replaceCurrentConversation(
        backStack: MutableList<NavKey>,
        conversationId: ConversationId,
    )

    fun resetBackStack(
        backStack: MutableList<NavKey>,
        destination: NavKey,
    )
}

internal class ConversationNavigationReducerImpl(
    private val navigationReducer: NavigationReducer = NavigationReducerImpl(),
) : ConversationNavigationReducer {

    override fun navigateToAddParticipants(
        backStack: MutableList<NavKey>,
        conversationId: ConversationId,
    ) {
        navigationReducer.push(
            backStack = backStack,
            destination = AddParticipantsNavKey(conversationId = conversationId),
        )
    }

    override fun navigateToConversation(
        backStack: MutableList<NavKey>,
        conversationId: ConversationId,
    ) {
        removeTrailingConversationEntryDestinations(backStack = backStack)

        navigationReducer.push(
            backStack = backStack,
            destination = ConversationNavKey(conversationId = conversationId),
        )
    }

    override fun navigateToMessageDetails(
        backStack: MutableList<NavKey>,
        conversationId: ConversationId,
        messageId: MessageId,
    ) {
        navigationReducer.push(
            backStack = backStack,
            destination = MessageDetailsNavKey(
                conversationId = conversationId,
                messageId = messageId,
            ),
        )
    }

    override fun navigateToRecipientPicker(
        backStack: MutableList<NavKey>,
        mode: RecipientPickerMode,
    ) {
        navigationReducer.push(
            backStack = backStack,
            destination = RecipientPickerNavKey(mode = mode),
        )
    }

    override fun popBackStack(backStack: MutableList<NavKey>): Boolean {
        return navigationReducer.pop(backStack = backStack)
    }

    override fun replaceCurrentConversation(
        backStack: MutableList<NavKey>,
        conversationId: ConversationId,
    ) {
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

    override fun resetBackStack(
        backStack: MutableList<NavKey>,
        destination: NavKey,
    ) {
        navigationReducer.reset(
            backStack = backStack,
            destination = destination,
        )
    }

    private fun removeTrailingConversationEntryDestinations(backStack: MutableList<NavKey>) {
        while (backStack.lastOrNull().isConversationEntryDestination()) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    private fun NavKey?.isConversationEntryDestination(): Boolean {
        return when (this) {
            NewChatNavKey -> true
            is RecipientPickerNavKey -> true
            else -> false
        }
    }
}
