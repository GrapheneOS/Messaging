package com.android.messaging.ui.recipientselection.delegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.contact.formatter.ContactDestinationFormatterImpl
import com.android.messaging.data.contact.model.Contact
import com.android.messaging.data.contact.model.ContactDestination
import com.android.messaging.data.contact.model.ContactsPage
import com.android.messaging.data.contact.repository.ContactsRepository
import com.android.messaging.data.phone.formatter.PhoneNumberFormatterImpl
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.contact.mapper.ContactUiModelMapperImpl
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecipientPickerDelegateNumberMatchingTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        installTestFactory(context = RuntimeEnvironment.getApplication().applicationContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun queryMatchedDestination_invalidNationalNumberSavedWithCountryCode_isThatNumber() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val state = loadState(
                query = INVALID_US_NATIONAL_NUMBER,
                countryCandidates = listOf(US),
                contactNumbers = listOf(INVALID_US_NUMBER, OTHER_INVALID_US_NUMBER),
            )

            val contactItem = state.items.single() as RecipientPickerListItem.Contact
            assertEquals(INVALID_US_NUMBER, contactItem.queryMatchedDestination?.value)
        }
    }

    @Test
    fun queryMatchedDestination_exactNumberListedAfterCountryCodeVariant_isExactNumber() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val state = loadState(
                query = INVALID_US_NATIONAL_NUMBER,
                countryCandidates = listOf(US),
                contactNumbers = listOf(INVALID_US_NUMBER, INVALID_US_NATIONAL_NUMBER),
            )

            val contactItem = state.items.single() as RecipientPickerListItem.Contact
            assertEquals(INVALID_US_NATIONAL_NUMBER, contactItem.queryMatchedDestination?.value)
        }
    }

    @Test
    fun queryMatchedDestination_sameDigitsInAnotherCountryListedFirst_isResolvedNumber() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val state = loadState(
                query = VALID_US_NATIONAL_NUMBER,
                countryCandidates = listOf(US, INDIA),
                contactNumbers = listOf(VALID_INDIAN_NUMBER, VALID_US_NUMBER),
            )

            val contactItem = state.items.single() as RecipientPickerListItem.Contact
            assertEquals(VALID_US_NUMBER, contactItem.queryMatchedDestination?.value)
        }
    }

    @Test
    fun syntheticPhone_contactHasSameDigitsOnlyInAnotherCountry_isShown() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val state = loadState(
                query = VALID_US_NATIONAL_NUMBER,
                countryCandidates = listOf(US, INDIA),
                contactNumbers = listOf(VALID_INDIAN_NUMBER),
            )

            val syntheticItem = state.items
                .filterIsInstance<RecipientPickerListItem.SyntheticPhone>()
                .single()
            val contactItem = state.items
                .filterIsInstance<RecipientPickerListItem.Contact>()
                .single()
            assertEquals(VALID_US_NUMBER, syntheticItem.normalizedDestination)
            assertNull(contactItem.queryMatchedDestination)
        }
    }

    @Test
    fun syntheticPhone_numberResolvedByLaterCountry_isNotMatchedToPrimaryCountryContact() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val state = loadState(
                query = INVALID_US_NATIONAL_NUMBER,
                countryCandidates = listOf(US, INDIA),
                contactNumbers = listOf(INVALID_US_NUMBER),
            )

            val syntheticItem = state.items
                .filterIsInstance<RecipientPickerListItem.SyntheticPhone>()
                .single()
            val contactItem = state.items
                .filterIsInstance<RecipientPickerListItem.Contact>()
                .single()
            assertEquals("+913371234567", syntheticItem.normalizedDestination)
            assertNull(contactItem.queryMatchedDestination)
        }
    }

    @Test
    fun syntheticPhone_unresolvedNumberMatchingOnlyUnderSecondaryCountry_isShown() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val state = loadState(
                query = INVALID_US_NATIONAL_NUMBER,
                countryCandidates = listOf(ESTONIA, US),
                contactNumbers = listOf(INVALID_US_NUMBER),
            )

            val syntheticItem = state.items
                .filterIsInstance<RecipientPickerListItem.SyntheticPhone>()
                .single()
            val contactItem = state.items
                .filterIsInstance<RecipientPickerListItem.Contact>()
                .single()
            assertEquals(INVALID_US_NATIONAL_NUMBER, syntheticItem.normalizedDestination)
            assertNull(contactItem.queryMatchedDestination)
        }
    }

    private fun TestScope.loadState(
        query: String,
        countryCandidates: List<String>,
        contactNumbers: List<String>,
    ): RecipientPickerUiState {
        val phoneNumberFormatter = PhoneNumberFormatterImpl(
            phoneUtils = phoneUtilsWithCountryCandidates(countryCandidates = countryCandidates),
        )
        val contactDestinationFormatter = ContactDestinationFormatterImpl(
            phoneNumberFormatter = phoneNumberFormatter,
        )
        val contact = contactWithNumbers(
            numbers = contactNumbers,
            contactDestinationFormatter = contactDestinationFormatter,
        )
        val delegate = RecipientPickerDelegateImpl(
            contactDestinationFormatter = contactDestinationFormatter,
            phoneNumberFormatter = phoneNumberFormatter,
            contactUiModelMapper = ContactUiModelMapperImpl(),
            contactsRepository = contactsRepositoryFinding(query = query, contact = contact),
            isReadContactsPermissionGranted = grantedReadContactsPermission(),
            savedStateHandle = SavedStateHandle(initialState = mapOf(SEARCH_QUERY_KEY to query)),
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )

        delegate.bind(scope = backgroundScope)
        testScheduler.advanceTimeBy(delayTimeMillis = 1_000L)
        testScheduler.runCurrent()
        return delegate.state.value
    }

    private fun phoneUtilsWithCountryCandidates(countryCandidates: List<String>): PhoneUtils {
        return object : PhoneUtils(SUB_ID) {
            override fun getCountryCandidatesForEnteredPhoneNumber(): List<String> {
                return countryCandidates
            }
        }
    }

    private fun contactWithNumbers(
        numbers: List<String>,
        contactDestinationFormatter: ContactDestinationFormatter,
    ): Contact {
        val destinations = numbers.mapIndexed { index, number ->
            ContactDestination(
                dataId = index.toLong(),
                contactId = CONTACT_ID,
                value = number,
                normalizedValue = contactDestinationFormatter.canonicalize(value = number),
                displayValue = number,
                kind = ContactDestination.Kind.PHONE,
                type = 1,
                customLabel = null,
                isPrimary = false,
                isSuperPrimary = false,
            )
        }

        return Contact(
            id = CONTACT_ID,
            lookupKey = "lookup_$CONTACT_ID",
            displayName = "Several Countries",
            photoUri = null,
            destinations = destinations.toImmutableList(),
        )
    }

    private fun contactsRepositoryFinding(query: String, contact: Contact): ContactsRepository {
        val contactsRepository = mockk<ContactsRepository>()
        every {
            contactsRepository.searchContacts(query = query, offset = 0)
        } returns flowOf(
            ContactsPage(
                contacts = persistentListOf(contact),
                nextOffset = null,
            ),
        )

        return contactsRepository
    }

    private fun grantedReadContactsPermission(): IsReadContactsPermissionGranted {
        val isReadContactsPermissionGranted = mockk<IsReadContactsPermissionGranted>()
        every { isReadContactsPermissionGranted() } returns true

        return isReadContactsPermissionGranted
    }

    private companion object {
        private const val SUB_ID = 2
        private const val CONTACT_ID = 52L
        private const val SEARCH_QUERY_KEY = "search_query"
        private const val US = "US"
        private const val INDIA = "IN"
        private const val ESTONIA = "EE"
        private const val INVALID_US_NATIONAL_NUMBER = "3371234567"
        private const val INVALID_US_NUMBER = "+1 337-123-4567"
        private const val OTHER_INVALID_US_NUMBER = "+1 337-123-4568"
        private const val VALID_US_NATIONAL_NUMBER = "2025550123"
        private const val VALID_US_NUMBER = "+12025550123"
        private const val VALID_INDIAN_NUMBER = "+912025550123"
    }
}
