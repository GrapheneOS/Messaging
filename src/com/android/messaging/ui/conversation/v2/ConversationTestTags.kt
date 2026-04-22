package com.android.messaging.ui.conversation.v2

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

internal const val CONVERSATION_COMPOSE_BAR_TEST_TAG = "conversation_compose_bar"
internal const val CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG = "conversation_attachment_button"
internal const val CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG =
    "conversation_attachment_contact_menu_item"
internal const val CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG =
    "conversation_attachment_media_menu_item"
internal const val CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG =
    "conversation_attachment_preview_list"
internal const val CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG = "conversation_add_people_button"
internal const val CONVERSATION_CALL_BUTTON_TEST_TAG = "conversation_call_button"
internal const val CONVERSATION_OVERFLOW_BUTTON_TEST_TAG = "conversation_overflow_button"
internal const val CONVERSATION_ARCHIVE_BUTTON_TEST_TAG = "conversation_archive_button"
internal const val CONVERSATION_UNARCHIVE_BUTTON_TEST_TAG = "conversation_unarchive_button"
internal const val CONVERSATION_ADD_CONTACT_BUTTON_TEST_TAG = "conversation_add_contact_button"
internal const val CONVERSATION_DELETE_CONVERSATION_BUTTON_TEST_TAG =
    "conversation_delete_conversation_button"
internal const val CONVERSATION_LOADING_INDICATOR_TEST_TAG = "conversation_loading_indicator"
internal const val CONVERSATION_MESSAGES_LIST_TEST_TAG = "conversation_messages_list"
internal const val CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG = "conversation_media_picker_overlay"
internal const val CONVERSATION_INLINE_AUDIO_ATTACHMENT_PLAY_BUTTON_TEST_TAG =
    "conversation_inline_audio_attachment_play_button"
internal const val CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG =
    "conversation_inline_audio_attachment_progress"
internal const val CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG = "conversation_audio_recording_bar"
internal const val CONVERSATION_AUDIO_RECORDING_CANCEL_BUTTON_TEST_TAG =
    "conversation_audio_recording_cancel_button"
internal const val ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG =
    "add_participants_confirm_button"
internal const val NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG =
    "new_chat_create_group_next_button"
internal const val NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG =
    "new_chat_contact_resolving_indicator"
internal const val CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE = "circle"
internal const val CONVERSATION_SEND_BUTTON_TEST_TAG = "conversation_send_button"
internal const val CONVERSATION_TEXT_FIELD_TEST_TAG = "conversation_text_field"
internal const val CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG =
    "conversation_sim_selector_menu_item"
internal const val CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG = "conversation_sim_selector_sheet"

internal fun conversationSimSelectorItemTestTag(selfParticipantId: String): String {
    return "conversation_sim_selector_item_$selfParticipantId"
}

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

internal fun addParticipantsContactRowTestTag(contactId: String): String {
    return "add_participants_contact_row_$contactId"
}

internal val conversationShapeSemanticsKey = SemanticsPropertyKey<String>(
    name = "conversation_shape",
)

internal var SemanticsPropertyReceiver.conversationShape by conversationShapeSemanticsKey
