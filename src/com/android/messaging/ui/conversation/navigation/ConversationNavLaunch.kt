package com.android.messaging.ui.conversation.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest

internal fun conversationLaunchBackStack(
    rootDestinations: List<NavKey>,
    launchRequest: ConversationEntryLaunchRequest?,
): List<NavKey> {
    val destination = conversationLaunchDestination(launchRequest = launchRequest)

    return rootDestinations + listOfNotNull(destination)
}

internal fun conversationLaunchDestination(
    launchRequest: ConversationEntryLaunchRequest?,
): NavKey? {
    if (launchRequest == null) {
        return null
    }

    return launchRequest
        .conversationId
        ?.let(::ConversationNavKey)
        ?: NewChatNavKey
}
