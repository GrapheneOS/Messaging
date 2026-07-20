package com.android.messaging.ui.host

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.android.messaging.ui.conversation.navigation.conversationEntries
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.conversationlist.navigation.conversationListEntry
import com.android.messaging.ui.onboarding.navigation.onboardingEntry

internal fun appNavEntryProvider(): (NavKey) -> NavEntry<NavKey> {
    return entryProvider {
        conversationListEntry()
        onboardingEntry(destinationsAfterOnboarding = listOf(ConversationListNavKey))
        conversationEntries()
    }
}
