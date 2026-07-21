package com.android.messaging.ui.conversationsettings.navigation

import android.os.Bundle
import com.android.messaging.ui.UIIntents

internal fun conversationSettingsDefaultArgs(navKey: ConversationSettingsNavKey): Bundle {
    return Bundle().apply {
        putString(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, navKey.conversationId.value)
    }
}
