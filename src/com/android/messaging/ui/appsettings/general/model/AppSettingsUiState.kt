package com.android.messaging.ui.appsettings.general.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.appsettings.model.ConversationSwipeSettings

@Immutable
internal data class AppSettingsUiState(
    val isDefaultSmsApp: Boolean = false,
    val defaultSmsAppLabel: String = "",
    val sendSoundEnabled: Boolean = true,
    val inConversationSoundEnabled: Boolean = false,
    val youTubeLinkPreviewsEnabled: Boolean = false,
    val conversationSwipeSettings: ConversationSwipeSettings = ConversationSwipeSettings.Default,
    val isDebugEnabled: Boolean = false,
    val dumpSmsEnabled: Boolean = false,
    val dumpMmsEnabled: Boolean = false,
)
