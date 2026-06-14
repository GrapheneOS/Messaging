package com.android.messaging.data.contact.repository

import android.content.ContentResolver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import com.android.messaging.data.contact.formatter.ContactDestinationFormatterImpl
import com.android.messaging.data.contact.model.Contact
import com.android.messaging.data.contact.model.ContactDestination
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ContactsRepositoryImplTest {

    private val contentResolver = mockk<ContentResolver>()
    private val phoneUtilsInstance = mockk<PhoneUtils>(relaxed = true)

    private var nextDataId = 1L

    @Before
    fun setUp() {
        nextDataId = 1L
        mockkStatic(PhoneUtils::class)
        mockkStatic(MmsSmsUtils::class)
        every { PhoneUtils.getDefault() } returns phoneUtilsInstance
        every { MmsSmsUtils.isEmailAddress(any()) } answers {
            val raw = firstArg<String>()
            "@" in raw
        }
        every { phoneUtilsInstance.getCanonicalForEnteredPhoneNumber(any()) } answers {
            firstArg<String>()
        }
        every { phoneUtilsInstance.countryCandidatesForEnteredPhoneNumber } returns emptyList()
        every {
            phoneUtilsInstance.getCanonicalForEnteredPhoneNumber(any(), any<List<String>>())
        } answers {
            firstArg<String>()
        }
        every { phoneUtilsInstance.formatForDisplay(any()) } answers {
            firstArg<String>()
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun multiNumberContactReturnsAllDestinationsWithSuperPrimaryFirst() = runTest {
        stubFilterPhoneCursor(
            query = "Multi",
            rows = listOf(
                phoneRow(
                    contactId = 1L,
                    sortKey = "Multi Person",
                    number = "+15550001",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                    isPrimary = false,
                    isSuperPrimary = false,
                ),
            ),
        )
        stubFilterEmailCursor(query = "Multi", rows = emptyList())
        stubExpansionPhoneCursor(
            rows = listOf(
                phoneRow(
                    contactId = 1L,
                    sortKey = "Multi Person",
                    number = "+15550001",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
                phoneRow(
                    contactId = 1L,
                    sortKey = "Multi Person",
                    number = "+15550002",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
                    isSuperPrimary = true,
                ),
                phoneRow(
                    contactId = 1L,
                    sortKey = "Multi Person",
                    number = "+15550003",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
                    isPrimary = true,
                ),
            ),
        )
        stubExpansionEmailCursor(rows = emptyList())

        val repo = createRepository()
        val page = repo.searchContacts(query = "Multi", offset = 0).first()

        val contact = page.contacts.single()
        Assert.assertEquals(1L, contact.id)
        Assert.assertEquals(3, contact.destinations.size)
        Assert.assertEquals(
            listOf("+15550002", "+15550003", "+15550001"),
            contact.destinations.map { it.value },
        )
        Assert.assertEquals(ContactDestination.Kind.PHONE, contact.destinations[0].kind)
        Assert.assertTrue(contact.destinations[0].isSuperPrimary)
        Assert.assertNull(page.nextOffset)
    }

    @Test
    fun nameAndNumberMatchesCollapseToSingleContactExpandedOnce() = runTest {
        stubFilterPhoneCursor(
            query = "Bob",
            rows = listOf(
                phoneRow(
                    contactId = 7L,
                    sortKey = "Bob",
                    number = "+17777777",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
                phoneRow(
                    contactId = 7L,
                    sortKey = "Bob",
                    number = "+17777778",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
                ),
            ),
        )
        stubFilterEmailCursor(query = "Bob", rows = emptyList())
        stubExpansionPhoneCursor(
            rows = listOf(
                phoneRow(
                    contactId = 7L,
                    sortKey = "Bob",
                    number = "+17777777",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
                phoneRow(
                    contactId = 7L,
                    sortKey = "Bob",
                    number = "+17777778",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
                ),
            ),
        )
        stubExpansionEmailCursor(rows = emptyList())

        val repo = createRepository()
        val page = repo.searchContacts(query = "Bob", offset = 0).first()

        val contact = page.contacts.single()
        Assert.assertEquals(7L, contact.id)
        Assert.assertEquals(
            listOf("+17777777", "+17777778"),
            contact.destinations.map { it.value },
        )
    }

    @Test
    fun sameNumberWithDifferentFormattingCollapsesToSingleDestination() = runTest {
        val rows = listOf(
            phoneRow(
                contactId = 8L,
                sortKey = "Dana",
                number = "+1 777-7777",
                type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            ),
            phoneRow(
                contactId = 8L,
                sortKey = "Dana",
                number = "+17777777",
                type = ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
            ),
        )

        stubFilterPhoneCursor(
            query = "Dana",
            rows = rows
        )
        stubFilterEmailCursor(
            query = "Dana",
            rows = emptyList()
        )

        stubExpansionPhoneCursor(rows = rows)
        stubExpansionEmailCursor(rows = emptyList())

        val repo = createRepository()
        val page = repo.searchContacts(
            query = "Dana",
            offset = 0,
        ).first()

        val destination = page.contacts.single().destinations.single()
        Assert.assertEquals("+17777777", destination.normalizedValue)
    }

    @Test
    fun twoContactsSharingNumberBothKeepIt() = runTest {
        stubFilterPhoneCursor(
            query = "Same",
            rows = listOf(
                phoneRow(
                    contactId = 1L,
                    sortKey = "Alpha",
                    number = "+15551111",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
                phoneRow(
                    contactId = 2L,
                    sortKey = "Beta",
                    number = "+15551111",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
            ),
        )
        stubFilterEmailCursor(query = "Same", rows = emptyList())
        stubExpansionPhoneCursor(
            rows = listOf(
                phoneRow(
                    contactId = 1L,
                    sortKey = "Alpha",
                    number = "+15551111",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
                phoneRow(
                    contactId = 2L,
                    sortKey = "Beta",
                    number = "+15551111",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
            ),
        )
        stubExpansionEmailCursor(rows = emptyList())

        val repo = createRepository()
        val page = repo.searchContacts(query = "Same", offset = 0).first()

        Assert.assertEquals(listOf(1L, 2L), page.contacts.map(Contact::id))
        page.contacts.forEach { contact ->
            Assert.assertEquals("+15551111", contact.destinations.single().value)
        }
    }

    @Test
    fun digitFallbackRecoversContactWhenFilterMatchesNothing() = runTest {
        stubFilterPhoneCursor(query = "1234", rows = emptyList())
        stubFilterEmailCursor(query = "1234", rows = emptyList())
        stubDefaultPhoneCursor(
            rows = listOf(
                phoneRow(
                    contactId = 3L,
                    sortKey = "Charlie",
                    number = "+1 (555) 1234567",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
            ),
        )
        stubExpansionPhoneCursor(
            rows = listOf(
                phoneRow(
                    contactId = 3L,
                    sortKey = "Charlie",
                    number = "+1 (555) 1234567",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
            ),
        )
        stubExpansionEmailCursor(rows = emptyList())

        val repo = createRepository()
        val page = repo.searchContacts(query = "1234", offset = 0).first()

        Assert.assertEquals(3L, page.contacts.single().id)
    }

    @Test
    fun paginationSplitsContactsAcrossPages() = runTest {
        val rows = (1..250).map { id ->
            phoneRow(
                contactId = id.toLong(),
                sortKey = "Person %03d".format(id),
                number = "+1555%07d".format(id),
                type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            )
        }
        stubDefaultPhoneCursor(rows = rows)
        stubDefaultEmailCursor(rows = emptyList())
        stubExpansionPhoneCursor(
            rows = rows,
        )
        stubExpansionEmailCursor(
            rows = emptyList(),
        )

        val repo = createRepository()
        val firstPage = repo.searchContacts(query = "", offset = 0).first()
        val secondPage = repo.searchContacts(query = "", offset = 200).first()

        Assert.assertEquals(200, firstPage.contacts.size)
        Assert.assertEquals(200, firstPage.nextOffset)
        Assert.assertEquals(50, secondPage.contacts.size)
        Assert.assertNull(secondPage.nextOffset)
    }

    @Test
    fun browseIncludesEmailDestinationsAndEmailOnlyContacts() = runTest {
        stubDefaultPhoneCursor(
            rows = listOf(
                phoneRow(
                    contactId = 1L,
                    sortKey = "Ada",
                    number = "+15550001",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
            ),
        )
        stubDefaultEmailCursor(
            rows = listOf(
                emailRow(
                    contactId = 1L,
                    sortKey = "Ada",
                    address = "ada@example.com",
                    type = ContactsContract.CommonDataKinds.Email.TYPE_HOME,
                ),
                emailRow(
                    contactId = 2L,
                    sortKey = "Bea",
                    address = "bea@example.com",
                    type = ContactsContract.CommonDataKinds.Email.TYPE_WORK,
                ),
            ),
        )

        val repo = createRepository()
        val page = repo.searchContacts(
            query = "",
            offset = 0,
        ).first()

        Assert.assertEquals(listOf(1L, 2L), page.contacts.map(Contact::id))
        Assert.assertEquals(
            listOf(ContactDestination.Kind.PHONE, ContactDestination.Kind.EMAIL),
            page.contacts.first { it.id == 1L }.destinations.map(ContactDestination::kind),
        )
        Assert.assertEquals(
            ContactDestination.Kind.EMAIL,
            page.contacts.first { it.id == 2L }.destinations.single().kind,
        )
    }

    @Test
    fun phoneAndEmailDestinationsForSameContactMerge() = runTest {
        stubFilterPhoneCursor(
            query = "Dee",
            rows = listOf(
                phoneRow(
                    contactId = 4L,
                    sortKey = "Dee",
                    number = "+14444444",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
            ),
        )
        stubFilterEmailCursor(query = "Dee", rows = emptyList())
        stubExpansionPhoneCursor(
            rows = listOf(
                phoneRow(
                    contactId = 4L,
                    sortKey = "Dee",
                    number = "+14444444",
                    type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
            ),
        )
        stubExpansionEmailCursor(
            rows = listOf(
                emailRow(
                    contactId = 4L,
                    sortKey = "Dee",
                    address = "dee@example.com",
                    type = ContactsContract.CommonDataKinds.Email.TYPE_WORK,
                ),
            ),
        )

        val repo = createRepository()
        val page = repo.searchContacts(query = "Dee", offset = 0).first()

        val contact = page.contacts.single()
        Assert.assertEquals(2, contact.destinations.size)
        val kinds = contact.destinations.map { it.kind }
        Assert.assertEquals(ContactDestination.Kind.PHONE, kinds[0])
        Assert.assertEquals(ContactDestination.Kind.EMAIL, kinds[1])
    }

    @Test
    fun largeContactSetGetsChunkedAndMergedEquivalently() = runTest {
        val contactCount = 1100
        val ids = (1..contactCount).map { it.toLong() }
        val rows = ids.map { id ->
            phoneRow(
                contactId = id,
                sortKey = "Person %05d".format(id),
                number = "+1555%07d".format(id),
                type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            )
        }
        stubFilterPhoneCursor(query = "Person", rows = rows)
        stubFilterEmailCursor(query = "Person", rows = emptyList())
        stubExpansionPhoneCursor(rows = rows)
        stubExpansionEmailCursor(rows = emptyList())

        val repo = createRepository()
        val page = repo.searchContacts(query = "Person", offset = 0).first()

        Assert.assertEquals(200, page.contacts.size)
        Assert.assertEquals(200, page.nextOffset)
        val firstContact = page.contacts.first()
        Assert.assertFalse(firstContact.destinations.isEmpty())
    }

    @Test
    fun searchExpandsOnlyPageContactsNotAllMatches() = runTest {
        val totalMatches = 600
        val ids = (1..totalMatches).map { it.toLong() }
        val rows = ids.map { id ->
            phoneRow(
                contactId = id,
                sortKey = "Person %05d".format(id),
                number = "+1555%07d".format(id),
                type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            )
        }
        stubFilterPhoneCursor(query = "Person", rows = rows)
        stubFilterEmailCursor(query = "Person", rows = emptyList())

        val queriedPhoneContactIds = mutableSetOf<Long>()
        val queriedEmailContactIds = mutableSetOf<Long>()

        every {
            contentResolver.query(
                match { uri -> uri == ContactsContract.CommonDataKinds.Phone.CONTENT_URI },
                any(),
                match { selection ->
                    selection.startsWith(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                },
                any(),
                isNull(),
            )
        } answers {
            val selectionArgs = arg<Array<String>?>(3) ?: emptyArray()
            selectionArgs.forEach { queriedPhoneContactIds.add(it.toLong()) }
            val argSet = selectionArgs.toSet()
            val matchingRows = rows.filter { row -> row.contactId.toString() in argSet }
            phoneCursor(rows = matchingRows)
        }

        every {
            contentResolver.query(
                match { uri -> uri == ContactsContract.CommonDataKinds.Email.CONTENT_URI },
                any(),
                match { selection ->
                    selection.startsWith(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                },
                any(),
                isNull(),
            )
        } answers {
            val selectionArgs = arg<Array<String>?>(3) ?: emptyArray()
            selectionArgs.forEach { queriedEmailContactIds.add(it.toLong()) }
            emailCursor(rows = emptyList())
        }

        val repo = createRepository()
        val page = repo.searchContacts(query = "Person", offset = 0).first()

        Assert.assertEquals(200, page.contacts.size)
        Assert.assertEquals(200, page.nextOffset)
        Assert.assertEquals((1L..200L).toSet(), queriedPhoneContactIds)
        Assert.assertEquals((1L..200L).toSet(), queriedEmailContactIds)
    }

    private fun createRepository(): ContactsRepositoryImpl {
        return ContactsRepositoryImpl(
            formatter = ContactDestinationFormatterImpl(),
            contentResolver = contentResolver,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun stubFilterPhoneCursor(query: String, rows: List<RawRow>) {
        every {
            contentResolver.query(
                match { uri -> isPhoneFilterUri(uri = uri, query = query) },
                any(),
                isNull(),
                isNull(),
                any(),
            )
        } answers { phoneCursor(rows = rows) }
    }

    private fun stubFilterEmailCursor(query: String, rows: List<RawRow>) {
        every {
            contentResolver.query(
                match { uri -> isEmailFilterUri(uri = uri, query = query) },
                any(),
                isNull(),
                isNull(),
                any(),
            )
        } answers { emailCursor(rows = rows) }
    }

    private fun stubDefaultPhoneCursor(rows: List<RawRow>) {
        every {
            contentResolver.query(
                match { uri -> isDefaultPhoneUri(uri = uri) },
                any(),
                isNull(),
                isNull(),
                any(),
            )
        } answers { phoneCursor(rows = rows) }
    }

    private fun stubDefaultEmailCursor(rows: List<RawRow>) {
        every {
            contentResolver.query(
                match { uri -> isDefaultEmailUri(uri = uri) },
                any(),
                isNull(),
                isNull(),
                any(),
            )
        } answers { emailCursor(rows = rows) }
    }

    private fun stubExpansionPhoneCursor(rows: List<RawRow>) {
        every {
            contentResolver.query(
                match { uri -> uri == ContactsContract.CommonDataKinds.Phone.CONTENT_URI },
                any(),
                match { selection ->
                    selection.startsWith(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                },
                any(),
                isNull(),
            )
        } answers {
            val selectionArgs = (arg<Array<String>?>(3) ?: emptyArray()).toSet()
            val matchingRows = rows.filter { row ->
                row.contactId.toString() in selectionArgs
            }
            phoneCursor(rows = matchingRows)
        }
    }

    private fun stubExpansionEmailCursor(rows: List<RawRow>) {
        every {
            contentResolver.query(
                match { uri -> uri == ContactsContract.CommonDataKinds.Email.CONTENT_URI },
                any(),
                match { selection ->
                    selection.startsWith(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                },
                any(),
                isNull(),
            )
        } answers {
            val selectionArgs = (arg<Array<String>?>(3) ?: emptyArray()).toSet()
            val matchingRows = rows.filter { row ->
                row.contactId.toString() in selectionArgs
            }
            emailCursor(rows = matchingRows)
        }
    }

    private fun isPhoneFilterUri(uri: Uri, query: String): Boolean {
        val expectedPrefix = ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI.toString()
        return uri.toString().startsWith(expectedPrefix) &&
            uri.pathSegments.contains(query)
    }

    private fun isEmailFilterUri(uri: Uri, query: String): Boolean {
        val expectedPrefix = ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI.toString()
        return uri.toString().startsWith(expectedPrefix) &&
            uri.pathSegments.contains(query)
    }

    private fun isDefaultPhoneUri(uri: Uri): Boolean {
        return uri.path == ContactsContract.CommonDataKinds.Phone.CONTENT_URI.path &&
            uri.getQueryParameter("directory") != null
    }

    private fun isDefaultEmailUri(uri: Uri): Boolean {
        return uri.path == ContactsContract.CommonDataKinds.Email.CONTENT_URI.path &&
            uri.getQueryParameter("directory") != null
    }

    private fun phoneCursor(rows: List<RawRow>): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
            ),
        )
        rows.forEach { row ->
            cursor.addRow(
                arrayOf<Any?>(
                    row.dataId,
                    row.contactId,
                    row.lookupKey,
                    row.displayName,
                    row.sortKey,
                    row.photoUri,
                    row.value,
                    row.type,
                    row.customLabel,
                    if (row.isPrimary) 1 else 0,
                    if (row.isSuperPrimary) 1 else 0,
                ),
            )
        }
        return cursor
    }

    private fun emailCursor(rows: List<RawRow>): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                ContactsContract.CommonDataKinds.Email._ID,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Email.SORT_KEY_PRIMARY,
                ContactsContract.CommonDataKinds.Email.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.LABEL,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
                ContactsContract.CommonDataKinds.Email.IS_SUPER_PRIMARY,
            ),
        )
        rows.forEach { row ->
            cursor.addRow(
                arrayOf<Any?>(
                    row.dataId,
                    row.contactId,
                    row.lookupKey,
                    row.displayName,
                    row.sortKey,
                    row.photoUri,
                    row.value,
                    row.type,
                    row.customLabel,
                    if (row.isPrimary) 1 else 0,
                    if (row.isSuperPrimary) 1 else 0,
                ),
            )
        }
        return cursor
    }

    private fun phoneRow(
        contactId: Long,
        sortKey: String,
        number: String,
        type: Int,
        customLabel: String? = null,
        isPrimary: Boolean = false,
        isSuperPrimary: Boolean = false,
        dataId: Long = nextDataId++,
    ): RawRow {
        return RawRow(
            dataId = dataId,
            contactId = contactId,
            lookupKey = "lookup_$contactId",
            displayName = sortKey,
            sortKey = sortKey,
            photoUri = null,
            value = number,
            type = type,
            customLabel = customLabel,
            isPrimary = isPrimary,
            isSuperPrimary = isSuperPrimary,
        )
    }

    private fun emailRow(
        contactId: Long,
        sortKey: String,
        address: String,
        type: Int,
        customLabel: String? = null,
        dataId: Long = nextDataId++,
    ): RawRow {
        return RawRow(
            dataId = dataId,
            contactId = contactId,
            lookupKey = "lookup_$contactId",
            displayName = sortKey,
            sortKey = sortKey,
            photoUri = null,
            value = address,
            type = type,
            customLabel = customLabel,
            isPrimary = false,
            isSuperPrimary = false,
        )
    }

    private data class RawRow(
        val dataId: Long,
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val sortKey: String,
        val photoUri: String?,
        val value: String,
        val type: Int,
        val customLabel: String?,
        val isPrimary: Boolean,
        val isSuperPrimary: Boolean,
    )
}
