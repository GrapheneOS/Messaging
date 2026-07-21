package com.android.messaging.ui.conversationsettings.navigation

import android.os.Bundle

internal fun conversationSettingsDefaultArgs(navKey: ConversationSettingsNavKey): Bundle {
    return Bundle().apply {
        putString(CONVERSATION_SETTINGS_CONVERSATION_ID_ARG, navKey.conversationId.value)
    }
}

internal const val CONVERSATION_SETTINGS_CONVERSATION_ID_ARG = "conversationId"
