package com.android.messaging.ui.conversationpicker.navigation

import android.os.Bundle
import com.android.messaging.ui.conversationpicker.host.forward.FORWARD_CONVERSATION_ID_ARG
import com.android.messaging.ui.conversationpicker.host.forward.FORWARD_MESSAGE_ID_ARG

internal fun forwardMessageDefaultArgs(navKey: ForwardMessageNavKey): Bundle {
    return Bundle().apply {
        putString(FORWARD_CONVERSATION_ID_ARG, navKey.conversationId.value)
        putString(FORWARD_MESSAGE_ID_ARG, navKey.messageId.value)
    }
}
