package com.android.messaging.ui.conversation.v2.navigation

import androidx.navigation3.runtime.NavKey

internal interface ConversationNavigationReducer {
    fun navigateToAddParticipants(
        backStack: MutableList<NavKey>,
        conversationId: String,
    )

    fun navigateToConversation(
        backStack: MutableList<NavKey>,
        conversationId: String,
    )

    fun navigateToRecipientPicker(
        backStack: MutableList<NavKey>,
        mode: RecipientPickerMode,
    )

    fun popBackStack(backStack: MutableList<NavKey>): Boolean

    fun replaceCurrentConversation(
        backStack: MutableList<NavKey>,
        conversationId: String,
    )

    fun resetBackStack(
        backStack: MutableList<NavKey>,
        destination: NavKey,
    )
}

internal class ConversationNavigationReducerImpl : ConversationNavigationReducer {

    override fun navigateToAddParticipants(
        backStack: MutableList<NavKey>,
        conversationId: String,
    ) {
        AddParticipantsNavKey(conversationId = conversationId)
            .takeIf { it != backStack.lastOrNull() }
            ?.let(backStack::add)
    }

    override fun navigateToConversation(
        backStack: MutableList<NavKey>,
        conversationId: String,
    ) {
        removeTrailingConversationEntryDestinations(backStack = backStack)

        val destination = ConversationNavKey(conversationId = conversationId)

        if (destination == backStack.lastOrNull()) {
            return
        }

        backStack.add(destination)
    }

    override fun navigateToRecipientPicker(
        backStack: MutableList<NavKey>,
        mode: RecipientPickerMode,
    ) {
        RecipientPickerNavKey(mode = mode)
            .takeIf { it != backStack.lastOrNull() }
            ?.let(backStack::add)
    }

    override fun popBackStack(backStack: MutableList<NavKey>): Boolean {
        if (backStack.size <= 1) {
            return false
        }

        backStack.removeAt(backStack.lastIndex)
        return true
    }

    override fun replaceCurrentConversation(
        backStack: MutableList<NavKey>,
        conversationId: String,
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
        if (backStack.size == 1 && backStack.firstOrNull() == destination) {
            return
        }

        backStack.clear()
        backStack.add(destination)
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
