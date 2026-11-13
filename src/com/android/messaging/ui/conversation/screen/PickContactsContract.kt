package com.android.messaging.ui.conversation.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.provider.ContactsPickerSessionContract
import androidx.activity.result.contract.ActivityResultContract

internal class PickContactsContract : ActivityResultContract<Int, List<Uri>>() {

    // The system contacts picker enforces the limit while the user selects; others ignore it.
    @SuppressLint("InlinedApi")
    override fun createIntent(context: Context, input: Int): Intent {
        val selectionLimit = input.coerceIn(
            minimumValue = 1,
            maximumValue = MAX_SYSTEM_PICKER_SELECTION_LIMIT,
        )

        return Intent(Intent.ACTION_PICK)
            .setType(Contacts.CONTENT_TYPE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            .putExtra(
                ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT,
                selectionLimit,
            )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return when {
            resultCode != Activity.RESULT_OK || intent == null -> emptyList()
            else -> intent.pickedContactUris()
        }
    }

    private companion object {
        // The system picker rejects the whole request above this.
        private const val MAX_SYSTEM_PICKER_SELECTION_LIMIT = 100
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
