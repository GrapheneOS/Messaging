package com.android.messaging.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.messaging.datamodel.action.MarkAsReadAction
import com.android.messaging.ui.UIIntents

class ConversationReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == UIIntents.ACTION_MESSAGE_READ) {
            val conversationId = intent.getStringExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID)
            MarkAsReadAction.markAsRead(conversationId, true)
        }
    }
}
