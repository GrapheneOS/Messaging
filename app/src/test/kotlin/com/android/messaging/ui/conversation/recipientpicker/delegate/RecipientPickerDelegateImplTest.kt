package com.android.messaging.ui.conversation.recipientpicker.delegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.contact.formatter.ContactDestinationFormatterImpl
import com.android.messaging.data.contact.model.Contact
import com.android.messaging.data.contact.model.ContactDestination
import com.android.messaging.data.contact.model.ContactsPage
import com.android.messaging.data.contact.repository.ContactsRepository
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerListItem
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerUiState
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class RecipientPickerDelegateImplTest {

    private val phoneUtilsInstance = mockk<PhoneUtils>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(PhoneUtils::class)
        mockkStatic(MmsSmsUtils::class)
        every { PhoneUtils.getDefault() } returns phoneUtilsInstance
        every { PhoneUtils.isValidSmsMmsDestination(any()) } answers {
            val raw = firstArg<String>()
            raw.isNotBlank() && raw.any { character -> character.isDigit() }
        }
        every { MmsSmsUtils.isEmailAddress(any()) } answers {
            val raw = firstArg<String>()
            "@" in raw
        }
        every { phoneUtilsInstance.getCanonicalForEnteredPhoneNumber(any()) } answers {
            firstArg<String>()
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun emptyQueryAndEmptyRepoYieldsEmptyItems() = runTest {
        val delegate = createDelegate(
            initialQuery = "",
            pages = mapOf(searchKey(query = "", offset = 0) to emptyPage()),
        )

        val finalState = bindAndAwait(delegate = delegate)

        assertTrue(finalState.items.isEmpty())
        assertFalse(finalState.canLoadMore)
        assertTrue(finalState.hasContactsPermission)
        assertFalse(finalState.isLoading)
    }

    @Test
    fun searchReturnsMultiDestinationContact() = runTest {
        val multiDestContact = contact(
            id = 11L,
            displayName = "Multi Dest",
            destinations = listOf(
                destination(value = "+15550001", contactId = 11L),
                destination(value = "+15550002", contactId = 11L),
                destination(value = "multi@example.com", contactId = 11L, isEmail = true),
            ),
        )
        val delegate = createDelegate(
            initialQuery = "multi",
            pages = mapOf(
                searchKey(query = "multi", offset = 0) to pageOf(multiDestContact),
            ),
        )

        val finalState = bindAndAwait(delegate = delegate)

        val contactItem = finalState.items
            .filterIsInstance<RecipientPickerListItem.Contact>()
            .single()
        assertEquals(11L, contactItem.contact.id)
        assertEquals(3, contactItem.destinations.size)
        assertEquals(
            listOf("+15550001", "+15550002", "multi@example.com"),
            contactItem.destinations.map { it.value },
        )
    }

    @Test
    fun excludedDestinationRemovesMatchingDestinationButKeepsContact() = runTest {
        every {
            phoneUtilsInstance.getCanonicalForEnteredPhoneNumber("(555) 000-0001")
        } returns "+15550001"
        val multiDestContact = contact(
            id = 12L,
            displayName = "Excluded Sample",
            destinations = listOf(
                destination(
                    value = "+15550001",
                    contactId = 12L,
                    normalizedValue = "+15550001",
                ),
                destination(
                    value = "+15550002",
                    contactId = 12L,
                    normalizedValue = "+15550002",
                ),
            ),
        )
        val delegate = createDelegate(
            initialQuery = "",
            pages = mapOf(searchKey(query = "", offset = 0) to pageOf(multiDestContact)),
        )
        delegate.onExcludedDestinationsChanged(destinations = setOf("(555) 000-0001"))

        val finalState = bindAndAwait(delegate = delegate)

        val contactItem = finalState.items
            .filterIsInstance<RecipientPickerListItem.Contact>()
            .single()
        assertEquals(1, contactItem.destinations.size)
        assertEquals("+15550002", contactItem.destinations.single().value)
    }

    @Test
    fun excludedDestinationCanonicalizesIncomingValues() = runTest {
        every {
            phoneUtilsInstance.getCanonicalForEnteredPhoneNumber("+1 555-000-0001")
        } returns "+15550001"
        val singleDestContact = contact(
            id = 13L,
            displayName = "Cross Format",
            destinations = listOf(
                destination(
                    value = "(555) 000-0001",
                    contactId = 13L,
                    normalizedValue = "+15550001",
                ),
            ),
        )
        val delegate = createDelegate(
            initialQuery = "",
            pages = mapOf(searchKey(query = "", offset = 0) to pageOf(singleDestContact)),
        )
        delegate.onExcludedDestinationsChanged(destinations = setOf("+1 555-000-0001"))

        val finalState = bindAndAwait(delegate = delegate)

        val contactItems = finalState.items
            .filterIsInstance<RecipientPickerListItem.Contact>()
        assertTrue(contactItems.isEmpty())
    }

    @Test
    fun excludingAllDestinationsTriggersFallbackToNextPage() = runTest {
        val firstPageContact = contact(
            id = 21L,
            displayName = "All Excluded",
            destinations = listOf(
                destination(value = "+15550001", contactId = 21L),
                destination(value = "+15550002", contactId = 21L),
            ),
        )
        val secondPageContact = contact(
            id = 22L,
            displayName = "Has Free Destination",
            destinations = listOf(destination(value = "+15550003", contactId = 22L)),
        )
        val delegate = createDelegate(
            initialQuery = "",
            pages = mapOf(
                searchKey(query = "", offset = 0) to ContactsPage(
                    contacts = persistentListOf(firstPageContact),
                    nextOffset = 1,
                ),
                searchKey(query = "", offset = 1) to ContactsPage(
                    contacts = persistentListOf(secondPageContact),
                    nextOffset = null,
                ),
            ),
        )
        delegate.onExcludedDestinationsChanged(
            destinations = setOf("+15550001", "+15550002"),
        )

        val finalState = bindAndAwait(delegate = delegate)

        val contactItem = finalState.items
            .filterIsInstance<RecipientPickerListItem.Contact>()
            .single()
        assertEquals(22L, contactItem.contact.id)
        assertFalse(finalState.canLoadMore)
    }

    @Test
    fun loadMoreAppendsNextPage() = runTest {
        val firstContact = contact(
            id = 31L,
            displayName = "Alpha",
            destinations = listOf(destination(value = "+11111111", contactId = 31L)),
        )
        val secondContact = contact(
            id = 32L,
            displayName = "Beta",
            destinations = listOf(destination(value = "+22222222", contactId = 32L)),
        )
        val delegate = createDelegate(
            initialQuery = "",
            pages = mapOf(
                searchKey(query = "", offset = 0) to ContactsPage(
                    contacts = persistentListOf(firstContact),
                    nextOffset = 1,
                ),
                searchKey(query = "", offset = 1) to ContactsPage(
                    contacts = persistentListOf(secondContact),
                    nextOffset = null,
                ),
            ),
        )

        bindAndAwait(delegate = delegate)
        delegate.onLoadMore()
        testScheduler.advanceTimeBy(delayTimeMillis = 1_000L)
        testScheduler.runCurrent()

        val finalState = delegate.state.value
        val contactItems = finalState.items.filterIsInstance<RecipientPickerListItem.Contact>()
        assertEquals(listOf(31L, 32L), contactItems.map { it.contact.id })
        assertFalse(finalState.canLoadMore)
    }

    @Test
    fun missingContactsPermissionEmitsEmptyState() = runTest {
        val delegate = createDelegate(
            initialQuery = "",
            pages = emptyMap(),
            isPermissionGranted = false,
        )

        val finalState = bindAndAwait(delegate = delegate)

        assertTrue(finalState.items.isEmpty())
        assertFalse(finalState.hasContactsPermission)
        assertFalse(finalState.canLoadMore)
    }

    @Test
    fun syntheticPhoneAppearsWhenQueryHasDigitsAndNoMatchingDestination() = runTest {
        val contactWithDifferentNumber = contact(
            id = 41L,
            displayName = "Some Person",
            destinations = listOf(destination(value = "+19999999", contactId = 41L)),
        )
        val delegate = createDelegate(
            initialQuery = "5550001",
            pages = mapOf(
                searchKey(query = "5550001", offset = 0) to pageOf(contactWithDifferentNumber),
            ),
        )

        val finalState = bindAndAwait(delegate = delegate)

        val syntheticItem = finalState.items
            .filterIsInstance<RecipientPickerListItem.SyntheticPhone>()
            .single()
        assertEquals("5550001", syntheticItem.destination)
        assertEquals(2, finalState.items.size)
    }

    @Test
    fun syntheticPhoneSuppressedWhenContactHasMatchingDestination() = runTest {
        val contactWithMatchingNumber = contact(
            id = 51L,
            displayName = "Match",
            destinations = listOf(destination(value = "5550001", contactId = 51L)),
        )
        val delegate = createDelegate(
            initialQuery = "5550001",
            pages = mapOf(
                searchKey(query = "5550001", offset = 0) to pageOf(contactWithMatchingNumber),
            ),
        )

        val finalState = bindAndAwait(delegate = delegate)

        val syntheticItems = finalState.items
            .filterIsInstance<RecipientPickerListItem.SyntheticPhone>()
        assertTrue(syntheticItems.isEmpty())
        assertEquals(1, finalState.items.size)
    }

    private fun TestScope.bindAndAwait(
        delegate: RecipientPickerDelegateImpl,
    ): RecipientPickerUiState {
        delegate.bind(scope = backgroundScope)
        testScheduler.advanceTimeBy(delayTimeMillis = 1_000L)
        testScheduler.runCurrent()
        return delegate.state.value
    }

    private fun TestScope.createDelegate(
        initialQuery: String,
        pages: Map<SearchKey, ContactsPage>,
        isPermissionGranted: Boolean = true,
    ): RecipientPickerDelegateImpl {
        return RecipientPickerDelegateImpl(
            contactDestinationFormatter = ContactDestinationFormatterImpl(),
            contactsRepository = mockContactsRepository(pages = pages),
            isReadContactsPermissionGranted = mockIsReadContactsPermissionGranted(
                isPermissionGranted = isPermissionGranted,
            ),
            savedStateHandle = SavedStateHandle(
                initialState = mapOf("search_query" to initialQuery),
            ),
            defaultDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
    }

    private fun mockContactsRepository(
        pages: Map<SearchKey, ContactsPage>,
    ): ContactsRepository {
        val contactsRepository = mockk<ContactsRepository>()
        every {
            contactsRepository.searchContacts(
                query = any(),
                offset = any(),
            )
        } answers {
            val key = searchKey(
                query = firstArg(),
                offset = secondArg(),
            )
            val page = pages[key] ?: emptyPage()
            flowOf(page)
        }

        return contactsRepository
    }

    private fun mockIsReadContactsPermissionGranted(
        isPermissionGranted: Boolean,
    ): IsReadContactsPermissionGranted {
        val isReadContactsPermissionGranted = mockk<IsReadContactsPermissionGranted>()
        every { isReadContactsPermissionGranted() } returns isPermissionGranted

        return isReadContactsPermissionGranted
    }

    private fun searchKey(query: String, offset: Int): SearchKey {
        return SearchKey(query = query, offset = offset)
    }

    private fun emptyPage(): ContactsPage {
        return ContactsPage(
            contacts = persistentListOf(),
            nextOffset = null,
        )
    }

    private fun pageOf(vararg contacts: Contact): ContactsPage {
        return ContactsPage(
            contacts = contacts.toList().toImmutableList(),
            nextOffset = null,
        )
    }

    private fun contact(
        id: Long,
        displayName: String,
        destinations: List<ContactDestination>,
    ): Contact {
        return Contact(
            id = id,
            lookupKey = "lookup_$id",
            displayName = displayName,
            photoUri = null,
            destinations = destinations.toImmutableList(),
        )
    }

    private fun destination(
        value: String,
        contactId: Long,
        isEmail: Boolean = false,
        normalizedValue: String = value,
        displayValue: String = value,
    ): ContactDestination {
        return ContactDestination(
            dataId = contactId * 100 + value.hashCode().toLong(),
            contactId = contactId,
            value = value,
            normalizedValue = normalizedValue,
            displayValue = displayValue,
            kind = when {
                isEmail -> ContactDestination.Kind.EMAIL
                else -> ContactDestination.Kind.PHONE
            },
            type = 1,
            customLabel = null,
            isPrimary = false,
            isSuperPrimary = false,
        )
    }

    private data class SearchKey(
        val query: String,
        val offset: Int,
    )
}
