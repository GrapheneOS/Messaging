package com.android.messaging.ui.debug.screen.model

import com.android.messaging.data.subscription.model.SubId

internal sealed interface DebugMmsConfigAction {

    data class SimSelected(
        val subId: SubId,
    ) : DebugMmsConfigAction

    data class EntryToggled(
        val key: String,
        val checked: Boolean,
    ) : DebugMmsConfigAction

    data class EntryValueSubmitted(
        val key: String,
        val value: String,
        val isNumeric: Boolean,
    ) : DebugMmsConfigAction
}
