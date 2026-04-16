package com.android.messaging.ui.conversation.v2

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

internal const val CONVERSATION_COMPOSE_BAR_TEST_TAG = "conversation_compose_bar"
internal const val CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG = "conversation_attachment_button"
internal const val CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG =
    "conversation_attachment_preview_list"
internal const val CONVERSATION_LOADING_INDICATOR_TEST_TAG = "conversation_loading_indicator"
internal const val CONVERSATION_MESSAGES_LIST_TEST_TAG = "conversation_messages_list"
internal const val CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG = "conversation_media_picker_overlay"
internal const val NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG =
    "new_chat_create_group_next_button"
internal const val NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG =
    "new_chat_contact_resolving_indicator"
internal const val CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE = "circle"
internal const val CONVERSATION_SEND_BUTTON_TEST_TAG = "conversation_send_button"
internal const val CONVERSATION_TEXT_FIELD_TEST_TAG = "conversation_text_field"

internal fun conversationMessageItemTestTag(messageId: String): String {
    return "conversation_message_item_$messageId"
}

internal fun conversationAttachmentPreviewItemTestTag(attachmentKey: String): String {
    return "conversation_attachment_preview_item_$attachmentKey"
}

internal fun conversationAttachmentPreviewRemoveButtonTestTag(
    attachmentKey: String,
): String {
    return "conversation_attachment_preview_remove_button_$attachmentKey"
}

internal fun newChatContactRowTestTag(contactId: String): String {
    return "new_chat_contact_row_$contactId"
}

internal val conversationShapeSemanticsKey = SemanticsPropertyKey<String>(
    name = "conversation_shape",
)

internal var SemanticsPropertyReceiver.conversationShape by conversationShapeSemanticsKey
