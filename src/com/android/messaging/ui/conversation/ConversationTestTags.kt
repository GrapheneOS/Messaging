package com.android.messaging.ui.conversation

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

internal const val CONVERSATION_COMPOSE_BAR_TEST_TAG = "conversation_compose_bar"
internal const val CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG = "conversation_attachment_button"
internal const val CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG =
    "conversation_attachment_contact_menu_item"
internal const val CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG =
    "conversation_attachment_audio_menu_item"
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
internal const val CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG =
    "conversation_media_capture_shutter_button"
internal const val CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG = "conversation_media_picker_overlay"
internal const val CONVERSATION_MMS_INDICATOR_TEST_TAG = "conversation_mms_indicator"
internal const val CONVERSATION_SEGMENT_COUNTER_TEST_TAG = "conversation_segment_counter"
internal const val CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG =
    "conversation_selection_overflow_button"
internal const val CONVERSATION_DELETE_MESSAGES_CONFIRM_BUTTON_TEST_TAG =
    "conversation_delete_messages_confirm_button"
internal const val CONVERSATION_DELETE_MESSAGES_DISMISS_BUTTON_TEST_TAG =
    "conversation_delete_messages_dismiss_button"
internal const val CONVERSATION_INLINE_AUDIO_ATTACHMENT_PLAY_BUTTON_TEST_TAG =
    "conversation_inline_audio_attachment_play_button"
internal const val CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG =
    "conversation_inline_audio_attachment_progress"
internal const val CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG = "conversation_audio_recording_bar"
internal const val CONVERSATION_AUDIO_RECORDING_CANCEL_BUTTON_TEST_TAG =
    "conversation_audio_recording_cancel_button"
internal const val CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG =
    "conversation_audio_recording_lock_affordance"
internal const val ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG = "add_participants_confirm_button"
internal const val NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG = "new_chat_create_group_button"
internal const val NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG = "new_chat_create_group_next_button"
internal const val NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG =
    "new_chat_contact_resolving_indicator"
internal const val NEW_CHAT_NAVIGATE_BACK_BUTTON_TEST_TAG = "new_chat_navigate_back_button"
internal const val NEW_CHAT_SIM_SELECTOR_CHIP_TEST_TAG = "new_chat_sim_selector_chip"
internal const val NEW_CHAT_SIM_SELECTOR_DROPDOWN_TEST_TAG = "new_chat_sim_selector_dropdown"
internal const val NEW_CHAT_TOP_APP_BAR_TITLE_TEST_TAG = "new_chat_top_app_bar_title"
internal const val RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG = "recipient_selection_query_field"
internal const val CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE = "circle"
internal const val CONVERSATION_SEND_BUTTON_TEST_TAG = "conversation_send_button"
internal const val CONVERSATION_TEXT_FIELD_TEST_TAG = "conversation_text_field"
internal const val CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG = "conversation_top_app_bar_title"
internal const val CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG =
    "conversation_sim_selector_menu_item"
internal const val CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG = "conversation_sim_selector_sheet"
internal const val CONVERSATION_SHOW_SUBJECT_FIELD_MENU_ITEM_TEST_TAG =
    "conversation_show_subject_field_menu_item"
internal const val CONVERSATION_SUBJECT_CHIP_TEST_TAG = "conversation_subject_chip"
internal const val CONVERSATION_SUBJECT_CHIP_CLEAR_BUTTON_TEST_TAG =
    "conversation_subject_chip_clear_button"
internal const val CONVERSATION_SUBJECT_DIALOG_TEST_TAG = "conversation_subject_dialog"
internal const val CONVERSATION_SUBJECT_DIALOG_TEXT_FIELD_TEST_TAG =
    "conversation_subject_dialog_text_field"
internal const val CONVERSATION_SUBJECT_DIALOG_CLEAR_BUTTON_TEST_TAG =
    "conversation_subject_dialog_clear_button"

internal fun conversationSimSelectorItemTestTag(selfParticipantId: String): String {
    return "conversation_sim_selector_item_$selfParticipantId"
}

internal fun conversationMessageItemTestTag(messageId: String): String {
    return "conversation_message_item_$messageId"
}

internal fun conversationSettingsParticipantRowTestTag(participantId: String): String {
    return "conversation_settings_participant_row_$participantId"
}

internal fun conversationMessageBubbleTestTag(messageId: String): String {
    return "conversation_message_bubble_$messageId"
}

internal fun conversationMessageSelectionRowTestTag(messageId: String): String {
    return "conversation_message_selection_row_$messageId"
}

internal fun conversationMessageSelectionActionButtonTestTag(action: String): String {
    return "conversation_message_selection_action_${action.lowercase()}"
}

internal fun conversationAttachmentPreviewItemTestTag(attachmentKey: String): String {
    return "conversation_attachment_preview_item_$attachmentKey"
}

internal fun conversationAttachmentPreviewRemoveButtonTestTag(
    attachmentKey: String,
): String {
    return "conversation_attachment_preview_remove_button_$attachmentKey"
}

internal fun conversationMediaReviewPreviewTestTag(contentUri: String): String {
    return "conversation_media_review_preview_$contentUri"
}

internal fun newChatContactRowTestTag(contactId: String): String {
    return "new_chat_contact_row_$contactId"
}

internal fun newChatContactDestinationRowTestTag(
    contactId: String,
    destination: String,
): String {
    return "new_chat_contact_destination_row_${contactId}_$destination"
}

internal fun addParticipantsContactRowTestTag(contactId: String): String {
    return "add_participants_contact_row_$contactId"
}

internal fun addParticipantsContactDestinationRowTestTag(
    contactId: String,
    destination: String,
): String {
    return "add_participants_contact_destination_row_${contactId}_$destination"
}

internal val conversationShapeSemanticsKey = SemanticsPropertyKey<String>(
    name = "conversation_shape",
)

internal var SemanticsPropertyReceiver.conversationShape by conversationShapeSemanticsKey
