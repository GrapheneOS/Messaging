package com.android.messaging.ui.smsstoragelow

import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction

internal const val SMS_STORAGE_LOW_WARNING_CANCEL_BUTTON_TEST_TAG =
    "sms_storage_low_warning_cancel_button"
internal const val SMS_STORAGE_LOW_WARNING_CONFIRM_BUTTON_TEST_TAG =
    "sms_storage_low_warning_confirm_button"
internal const val SMS_STORAGE_LOW_WARNING_CONFIRMATION_DIALOG_TEST_TAG =
    "sms_storage_low_warning_confirmation_dialog"
internal const val SMS_STORAGE_LOW_WARNING_CONFIRMATION_MESSAGE_TEST_TAG =
    "sms_storage_low_warning_confirmation_message"
internal const val SMS_STORAGE_LOW_WARNING_DIALOG_TEST_TAG = "sms_storage_low_warning_dialog"
internal const val SMS_STORAGE_LOW_WARNING_IGNORE_BUTTON_TEST_TAG =
    "sms_storage_low_warning_ignore_button"
internal const val SMS_STORAGE_LOW_WARNING_LOADING_TEST_TAG = "sms_storage_low_warning_loading"
internal const val SMS_STORAGE_LOW_WARNING_MESSAGE_TEST_TAG = "sms_storage_low_warning_message"
internal const val SMS_STORAGE_LOW_WARNING_TITLE_TEST_TAG = "sms_storage_low_warning_title"
internal const val SMS_STORAGE_LOW_WARNING_DELETE_MEDIA_ACTION_TEST_TAG =
    "sms_storage_low_warning_delete_media_action"
internal const val SMS_STORAGE_LOW_WARNING_DELETE_OLD_ACTION_TEST_TAG =
    "sms_storage_low_warning_delete_old_action"

internal fun smsStorageLowWarningActionTestTag(
    action: SmsStorageLowWarningAction,
): String {
    return when (action) {
        is SmsStorageLowWarningAction.DeleteMediaMessages -> {
            SMS_STORAGE_LOW_WARNING_DELETE_MEDIA_ACTION_TEST_TAG
        }

        is SmsStorageLowWarningAction.DeleteOldMessages -> {
            SMS_STORAGE_LOW_WARNING_DELETE_OLD_ACTION_TEST_TAG
        }
    }
}
