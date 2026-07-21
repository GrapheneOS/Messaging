package com.android.messaging.ui.host

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.android.messaging.ui.blockedparticipants.navigation.blockedParticipantsEntry
import com.android.messaging.ui.conversation.navigation.conversationEntries
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.conversationlist.navigation.conversationListEntries
import com.android.messaging.ui.conversationsettings.navigation.conversationSettingsEntry
import com.android.messaging.ui.onboarding.navigation.onboardingEntry

internal fun appNavEntryProvider(): (NavKey) -> NavEntry<NavKey> {
    return entryProvider {
        conversationListEntries()
        onboardingEntry(destinationAfterOnboarding = ConversationListNavKey)
        conversationEntries()
        conversationSettingsEntry()
        blockedParticipantsEntry()
    }
}
