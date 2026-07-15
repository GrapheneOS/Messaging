package com.android.messaging.ui.conversationpicker.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.android.messaging.R

@Immutable
sealed class ConversationPickerLabels {

    @get:StringRes
    open val title: Int = R.string.share_intent_activity_label

    @get:StringRes
    open val recentConversationsTitle: Int = R.string.share_recent_conversations_title

    @get:StringRes
    open val searchHint: Int = R.string.share_search_hint

    @get:StringRes
    abstract val emptyStateText: Int

    data object Share : ConversationPickerLabels() {
        override val emptyStateText = R.string.contact_list_empty_text
    }

    data object Forward : ConversationPickerLabels() {
        override val title = R.string.forward_message_activity_title
        override val emptyStateText = R.string.forward_picker_empty_text
    }

    data object Widget : ConversationPickerLabels() {
        override val emptyStateText = R.string.widget_picker_empty_text
    }
}
