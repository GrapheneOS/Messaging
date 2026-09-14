package com.android.messaging.ui.conversation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversationpicker.navigation.ForwardMessageNavKey
import com.android.messaging.ui.conversationsettings.navigation.ConversationSettingsNavKey
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.Navigator
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.navigation.PhotoViewerNavKey
import com.android.messaging.ui.vcarddetail.navigation.VCardDetailNavKey

@Stable
internal interface ConversationNavigator {

    fun navigateToAddParticipants(conversationId: ConversationId)

    fun navigateToConversation(conversationId: ConversationId)

    fun navigateToNewChat()

    fun navigateToMessageDetails(
        conversationId: ConversationId,
        messageId: MessageId,
    )

    fun navigateToForward(
        conversationId: ConversationId,
        messageId: MessageId,
    )

    fun navigateToConversationSettings(conversationId: ConversationId)

    fun navigateToVCardDetail(uri: String)

    fun navigateToPhotoViewer(
        conversationId: ConversationId,
        launchRequest: PhotoViewerLaunchRequest,
    )

    fun replaceCurrentConversation(conversationId: ConversationId)

    fun replaceWithConversation(conversationId: ConversationId)
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

        val destination = ConversationNavKey(conversationId = conversationId)
        val openedIndex = backStack.indexOfLast { navKey -> navKey == destination }

        if (openedIndex >= 0) {
            navigator.reset(destinations = backStack.take(openedIndex + 1))
            return
        }

        when (backStack.lastOrNull()) {
            is ConversationNavKey -> navigator.replaceTop(destination = destination)
            else -> navigator.push(destination = destination)
        }
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

    override fun navigateToForward(
        conversationId: ConversationId,
        messageId: MessageId,
    ) {
        navigator.push(
            destination = ForwardMessageNavKey(
                conversationId = conversationId,
                messageId = messageId,
            ),
        )
    }

    override fun navigateToConversationSettings(conversationId: ConversationId) {
        navigator.push(
            destination = ConversationSettingsNavKey(conversationId = conversationId),
        )
    }

    override fun navigateToVCardDetail(uri: String) {
        navigator.push(
            destination = VCardDetailNavKey(uri = uri),
        )
    }

    override fun navigateToPhotoViewer(
        conversationId: ConversationId,
        launchRequest: PhotoViewerLaunchRequest,
    ) {
        navigator.push(
            destination = PhotoViewerNavKey(
                conversationId = conversationId,
                launchRequest = launchRequest,
            ),
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

    override fun replaceWithConversation(conversationId: ConversationId) {
        navigator.replaceTop(destination = ConversationNavKey(conversationId = conversationId))
    }

    private fun removeTrailingConversationEntryDestinations() {
        while (backStack.lastOrNull().isConversationEntryDestination()) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    private fun NavKey?.isConversationEntryDestination(): Boolean {
        return when (this) {
            is NewChatNavKey -> true
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
