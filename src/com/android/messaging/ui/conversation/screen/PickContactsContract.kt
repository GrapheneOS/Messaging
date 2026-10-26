package com.android.messaging.ui.conversation.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.Contacts
import androidx.activity.result.contract.ActivityResultContract

internal class PickContactsContract : ActivityResultContract<Unit, List<Uri>>() {

    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_PICK)
            .setType(Contacts.CONTENT_TYPE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return when {
            resultCode != Activity.RESULT_OK || intent == null -> emptyList()
            else -> intent.pickedContactUris()
        }
    }
}

// A multiple selection comes back as clip data; pickers that ignore the extra return one data URI.
private fun Intent.pickedContactUris(): List<Uri> {
    val clipDataUris = clipData?.let { clipData ->
        (0 until clipData.itemCount).mapNotNull { index -> clipData.getItemAt(index).uri }
    }.orEmpty()

    return when {
        clipDataUris.isNotEmpty() -> clipDataUris
        else -> listOfNotNull(data)
    }
}
