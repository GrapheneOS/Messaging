package com.android.messaging.ui.conversation.screen

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.provider.ContactsPickerSessionContract
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
    fun createIntent_requestsMultipleContactsUpToTheRemainingSlots() {
        val intent = createIntent(remainingAttachmentSlots = 3)

        assertEquals(Intent.ACTION_PICK, intent.action)
        assertEquals(Contacts.CONTENT_TYPE, intent.type)
        assertTrue(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
        assertEquals(3, intent.selectionLimit())
    }

    @Test
    fun createIntent_keepsTheSelectionLimitWithinTheSystemPickerRange() {
        assertEquals(1, createIntent(remainingAttachmentSlots = 0).selectionLimit())
        assertEquals(100, createIntent(remainingAttachmentSlots = 150).selectionLimit())
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

    private fun createIntent(remainingAttachmentSlots: Int): Intent {
        return contract.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            input = remainingAttachmentSlots,
        )
    }

    private fun Intent.selectionLimit(): Int {
        return getIntExtra(ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT, -1)
    }

    private companion object {
        private val FIRST_CONTACT_URI = Uri.parse("content://com.android.contacts/contacts/1")
        private val SECOND_CONTACT_URI = Uri.parse("content://com.android.contacts/contacts/2")
    }
}
