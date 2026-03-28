package com.android.messaging.ui.conversation.v2

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

internal const val CONVERSATION_COMPOSE_BAR_TEST_TAG = "conversation_compose_bar"
internal const val CONVERSATION_LOADING_INDICATOR_TEST_TAG = "conversation_loading_indicator"
internal const val CONVERSATION_MESSAGES_LIST_TEST_TAG = "conversation_messages_list"
internal const val CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE = "circle"
internal const val CONVERSATION_SEND_BUTTON_TEST_TAG = "conversation_send_button"
internal const val CONVERSATION_TEXT_FIELD_TEST_TAG = "conversation_text_field"

internal fun conversationMessageItemTestTag(messageId: String): String {
    return "conversation_message_item_$messageId"
}

internal val conversationShapeSemanticsKey = SemanticsPropertyKey<String>(
    name = "conversation_shape",
)

internal var SemanticsPropertyReceiver.conversationShape by conversationShapeSemanticsKey
