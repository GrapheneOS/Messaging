package com.android.messaging.util

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.installTestFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContactUtilSenderNameTest {

    private lateinit var context: Context
    private lateinit var prefs: FakeBuglePrefs
    private lateinit var contacts: FakeContactsProvider

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application).grantPermissions(Manifest.permission.READ_CONTACTS)
        context = application.applicationContext
        prefs = FakeBuglePrefs()
        installTestFactory(context = context, prefs = prefs)
        contacts = Robolectric.setupContentProvider(
            FakeContactsProvider::class.java,
            ContactsContract.AUTHORITY,
        )
        contacts.savedNumbers["My Bank"] = "MYBANKTAN"
        contacts.savedNumbers["Bank 24"] = "Bank24"
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun whenEnabled_findsTheContactThatHasTheSenderNameSavedAsItsNumber() {
        givenNamedSenderContacts(enabled = true)

        assertEquals(listOf("My Bank"), lookUpNames("MyBankTAN"))
    }

    @Test
    fun whenEnabled_findsSenderNamesThatContainDigits() {
        givenNamedSenderContacts(enabled = true)

        assertEquals(listOf("Bank 24"), lookUpNames("Bank24"))
    }

    @Test
    fun whenEnabled_onlyMatchesTheWholeSenderName() {
        givenNamedSenderContacts(enabled = true)

        assertEquals(emptyList<String>(), lookUpNames("MyBank"))
    }

    @Test
    fun whenEnabled_keepsLookingUpPhoneNumbersLoosely() {
        givenNamedSenderContacts(enabled = true)

        lookUpNames("+12015550123")

        assertTrue(contacts.queriedUris.single().toString().startsWith(PHONE_LOOKUP_PREFIX))
    }

    @Test
    fun byDefault_doesNotMatchSenderNames() {
        assertEquals(emptyList<String>(), lookUpNames("MyBankTAN"))
        assertTrue(contacts.queriedUris.single().toString().startsWith(PHONE_LOOKUP_PREFIX))
    }

    private fun givenNamedSenderContacts(enabled: Boolean) {
        prefs.putBoolean(context.getString(R.string.named_sender_contacts_pref_key), enabled)
    }

    private fun lookUpNames(destination: String): List<String> {
        // Contact lookups refuse to run on the main thread
        val executor = Executors.newSingleThreadExecutor()
        try {
            return executor.submit(
                Callable {
                    ContactUtil.lookupDestination(context, destination)
                        .performSynchronousQuery()
                        .use { cursor -> cursor.displayNames() }
                },
            ).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } finally {
            executor.shutdown()
        }
    }

    private fun Cursor.displayNames(): List<String> {
        return buildList {
            while (moveToNext()) {
                add(getString(ContactUtil.INDEX_DISPLAY_NAME))
            }
        }
    }

    /**
     * Answers exact number queries on the phone table the way CP2 does. PhoneLookup never
     * matches a sender name, since it only compares phone numbers.
     */
    class FakeContactsProvider : ContentProvider() {
        val savedNumbers = linkedMapOf<String, String>()
        val queriedUris = mutableListOf<Uri>()

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            queriedUris += uri
            val cursor = MatrixCursor(projection)
            if (uri == Phone.CONTENT_URI && selection == "${Phone.NUMBER} = ? COLLATE NOCASE") {
                savedNumbers
                    .filterValues { number -> number.equals(selectionArgs!!.single(), true) }
                    .forEach { (name, number) ->
                        cursor.addRow(contactRow(name = name, number = number))
                    }
            }
            return cursor
        }

        private fun contactRow(name: String, number: String): Array<Any?> {
            return arrayOf(1L, name, null, number, Phone.TYPE_OTHER, null, "lookup-key")
        }

        override fun onCreate(): Boolean = true

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            return 0
        }

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    private companion object {
        private const val TIMEOUT_SECONDS = 5L
        private val PHONE_LOOKUP_PREFIX =
            ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI.toString()
    }
}
