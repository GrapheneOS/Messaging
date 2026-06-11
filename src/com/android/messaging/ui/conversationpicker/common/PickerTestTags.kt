package com.android.messaging.ui.conversationpicker.common

internal const val ATTACHMENT_PREVIEW_LIST_TEST_TAG = "share_attachment_preview_list"
internal const val SUBJECT_CHIP_TEST_TAG = "share_subject_chip"
internal const val SUBJECT_CHIP_CLEAR_BUTTON_TEST_TAG = "share_subject_chip_clear_button"

internal fun attachmentItemTestTag(id: String): String {
    return "share_attachment_preview_item_$id"
}

internal fun attachmentRemoveButtonTestTag(id: String): String {
    return "share_attachment_preview_remove_button_$id"
}

internal fun pickerContactRowTestTag(id: String): String {
    return "share_contact_row_$id"
}
