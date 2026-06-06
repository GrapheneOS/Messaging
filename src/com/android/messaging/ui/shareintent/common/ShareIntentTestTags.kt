package com.android.messaging.ui.shareintent.common

internal const val SHARE_ATTACHMENT_PREVIEW_LIST_TEST_TAG = "share_attachment_preview_list"
internal const val SHARE_SUBJECT_CHIP_TEST_TAG = "share_subject_chip"
internal const val SHARE_SUBJECT_CHIP_CLEAR_BUTTON_TEST_TAG = "share_subject_chip_clear_button"

internal fun shareAttachmentItemTestTag(id: String): String {
    return "share_attachment_preview_item_$id"
}

internal fun shareAttachmentRemoveButtonTestTag(id: String): String {
    return "share_attachment_preview_remove_button_$id"
}
