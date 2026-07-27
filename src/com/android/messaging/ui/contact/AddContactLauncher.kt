package com.android.messaging.ui.contact

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Intents
import com.android.messaging.R
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UiUtils

private const val LOG_TAG = "AddContactLauncher"

internal fun launchAddContact(
    context: Context,
    destination: String,
) {
    val destinationType = when {
        MmsSmsUtils.isEmailAddress(destination) -> Intents.Insert.EMAIL
        else -> Intents.Insert.PHONE
    }
    val intent = Intent(Intent.ACTION_INSERT_OR_EDIT)
        .setType(Contacts.CONTENT_ITEM_TYPE)
        .putExtra(destinationType, destination)

    try {
        context.startActivity(intent)
    } catch (exception: ActivityNotFoundException) {
        LogUtil.w(LOG_TAG, "No activity found for the add contact intent", exception)
        UiUtils.showToastAtBottom(R.string.activity_not_found_message)
    }
}
