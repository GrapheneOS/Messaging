package com.android.messaging.ui.conversation.screen

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.Contacts
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PickContactsContractTest {

    private val contract = PickContactsContract()

    @Test
    fun createIntent_requestsMultipleContacts() {
        val intent = contract.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            input = Unit,
        )

        assertEquals(Intent.ACTION_PICK, intent.action)
        assertEquals(Contacts.CONTENT_TYPE, intent.type)
        assertTrue(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
    }

    @Test
    fun parseResult_returnsEveryClipDataUri() {
        val clipData = ClipData.newRawUri(null, FIRST_CONTACT_URI).apply {
            addItem(ClipData.Item(SECOND_CONTACT_URI))
        }
        val intent = Intent().apply {
            this.clipData = clipData
        }

        assertEquals(
            listOf(FIRST_CONTACT_URI, SECOND_CONTACT_URI),
            contract.parseResult(resultCode = Activity.RESULT_OK, intent = intent),
        )
    }

    @Test
    fun parseResult_fallsBackToTheDataUri() {
        val intent = Intent().setData(FIRST_CONTACT_URI)

        assertEquals(
            listOf(FIRST_CONTACT_URI),
            contract.parseResult(resultCode = Activity.RESULT_OK, intent = intent),
        )
    }

    @Test
    fun parseResult_returnsNothingWhenCanceledOrEmpty() {
        val intent = Intent().setData(FIRST_CONTACT_URI)

        assertEquals(
            emptyList<Uri>(),
            contract.parseResult(resultCode = Activity.RESULT_CANCELED, intent = intent),
        )
        assertEquals(
            emptyList<Uri>(),
            contract.parseResult(resultCode = Activity.RESULT_OK, intent = null),
        )
        assertEquals(
            emptyList<Uri>(),
            contract.parseResult(resultCode = Activity.RESULT_OK, intent = Intent()),
        )
    }

    private companion object {
        private val FIRST_CONTACT_URI = Uri.parse("content://com.android.contacts/contacts/1")
        private val SECOND_CONTACT_URI = Uri.parse("content://com.android.contacts/contacts/2")
    }
}
