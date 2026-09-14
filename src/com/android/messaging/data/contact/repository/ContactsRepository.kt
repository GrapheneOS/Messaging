package com.android.messaging.data.contact.repository

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Directory
import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.contact.model.Contact
import com.android.messaging.data.contact.model.ContactDestination
import com.android.messaging.data.contact.model.ContactsPage
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.core.extension.typedFlow
import javax.inject.Inject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal interface ContactsRepository {

    fun searchContacts(
        query: String,
        offset: Int,
    ): Flow<ContactsPage>
}

internal class ContactsRepositoryImpl @Inject constructor(
    private val formatter: ContactDestinationFormatter,
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ContactsRepository {

    override fun searchContacts(
        query: String,
        offset: Int,
    ): Flow<ContactsPage> {
        return typedFlow {
            queryContactsPage(
                query = query,
                offset = offset,
            )
        }.flowOn(ioDispatcher)
    }

    private fun queryContactsPage(
        query: String,
        offset: Int,
    ): ContactsPage {
        return when {
            query.isBlank() -> queryAllContacts(offset = offset)
            else -> queryMatchedContacts(query = query, offset = offset)
        }
    }

    private fun queryAllContacts(offset: Int): ContactsPage {
        val phoneRows = readDestinationRows(
            uri = createDefaultPhoneQueryUri(),
            kind = ContactDestination.Kind.PHONE,
            sortOrder = SORT_BY_SORT_KEY_PRIMARY_ASC,
        )

        val emailRows = readDestinationRows(
            uri = createDefaultEmailQueryUri(),
            kind = ContactDestination.Kind.EMAIL,
            sortOrder = SORT_BY_SORT_KEY_PRIMARY_ASC,
        )

        val mergedRows = (phoneRows + emailRows).sortedBy { row -> row.sortKey }

        val contactGroups = when {
            mergedRows.isEmpty() -> emptyList()
            else -> groupRowsByContactPreservingOrder(rows = mergedRows)
        }

        return paginateAndBuildContacts(
            contactGroups = contactGroups,
            offset = offset,
        )
    }

    private fun queryMatchedContacts(query: String, offset: Int): ContactsPage {
        val phoneMatches = readDestinationRows(
            uri = createPhoneFilterUri(query = query),
            kind = ContactDestination.Kind.PHONE,
            sortOrder = SORT_BY_SORT_KEY_PRIMARY_ASC,
        )

        val emailMatches = readDestinationRows(
            uri = createEmailFilterUri(query = query),
            kind = ContactDestination.Kind.EMAIL,
            sortOrder = SORT_BY_SORT_KEY_PRIMARY_ASC,
        )

        val mergedMatches = (phoneMatches + emailMatches).sortedBy { it.sortKey }

        if (mergedMatches.isEmpty()) {
            val hasDigits = query.any { character -> character.isDigit() }

            return when {
                hasDigits -> queryDigitsFallback(query = query, offset = offset)
                else -> emptyContactsPage()
            }
        }

        return paginateMatchedContactIds(matchRows = mergedMatches, offset = offset)
    }

    private fun queryDigitsFallback(query: String, offset: Int): ContactsPage {
        val queryDigits = extractDigits(value = query)

        val phoneRows = readDestinationRows(
            uri = createDefaultPhoneQueryUri(),
            kind = ContactDestination.Kind.PHONE,
            sortOrder = SORT_BY_SORT_KEY_PRIMARY_ASC,
        )

        val matchingRows = phoneRows.filter { row ->
            extractDigits(value = row.value).contains(queryDigits)
        }

        return when {
            matchingRows.isEmpty() -> emptyContactsPage()
            else -> paginateMatchedContactIds(matchRows = matchingRows, offset = offset)
        }
    }

    private fun paginateMatchedContactIds(
        matchRows: List<DestinationRow>,
        offset: Int,
    ): ContactsPage {
        val contactSortKeys = LinkedHashMap<Long, String>()

        matchRows.forEach { row ->
            contactSortKeys.putIfAbsent(row.contactId, row.sortKey)
        }

        val orderedContactIds = contactSortKeys.keys.toList()

        val pageStart = offset.coerceAtMost(maximumValue = orderedContactIds.size)
        val pageEndExclusive = (pageStart + PAGE_SIZE)
            .coerceAtMost(maximumValue = orderedContactIds.size)

        val pageContactIds = orderedContactIds.subList(
            fromIndex = pageStart,
            toIndex = pageEndExclusive,
        )

        val pageGroups = when {
            pageContactIds.isEmpty() -> emptyList()
            else -> {
                loadAndGroupDestinationsForContacts(
                    contactIds = pageContactIds,
                    sortKeysByContactId = contactSortKeys,
                )
            }
        }

        val pagedContacts = when {
            pageGroups.isEmpty() -> persistentListOf()
            else -> buildPageContacts(pageGroups = pageGroups)
        }

        val nextOffset = pageEndExclusive.takeIf { it < orderedContactIds.size }

        return ContactsPage(
            contacts = pagedContacts,
            nextOffset = nextOffset,
        )
    }

    private fun loadAndGroupDestinationsForContacts(
        contactIds: List<Long>,
        sortKeysByContactId: Map<Long, String>,
    ): List<List<DestinationRow>> {
        val phoneRows = readDestinationRowsForContacts(
            uri = Phone.CONTENT_URI,
            kind = ContactDestination.Kind.PHONE,
            contactIds = contactIds,
        )

        val emailRows = readDestinationRowsForContacts(
            uri = Email.CONTENT_URI,
            kind = ContactDestination.Kind.EMAIL,
            contactIds = contactIds,
        )

        val rowsByContact = contactIds.associateWith {
            mutableListOf<DestinationRow>()
        }

        (phoneRows + emailRows).forEach { row ->
            rowsByContact[row.contactId]?.add(row)
        }

        return rowsByContact.entries
            .asSequence()
            .filter { (_, rows) -> rows.isNotEmpty() }
            .sortedWith(
                compareBy<Map.Entry<Long, List<DestinationRow>>> {
                    sortKeysByContactId[it.key].orEmpty()
                }
                    .thenBy { it.value.first().displayName }
                    .thenBy { it.key },
            )
            .map { it.value }
            .toList()
    }

    private fun emptyContactsPage(): ContactsPage {
        return ContactsPage(
            contacts = persistentListOf(),
            nextOffset = null,
        )
    }

    private fun groupRowsByContactPreservingOrder(
        rows: List<DestinationRow>,
    ): List<List<DestinationRow>> {
        return rows
            .groupBy(DestinationRow::contactId)
            .values
            .toList()
    }

    private fun readDestinationRowsForContacts(
        uri: Uri,
        kind: ContactDestination.Kind,
        contactIds: Collection<Long>,
    ): List<DestinationRow> {
        if (contactIds.isEmpty()) {
            return emptyList()
        }

        val rows = mutableListOf<DestinationRow>()

        contactIds.chunked(size = CONTACT_ID_CHUNK_SIZE).forEach { chunk ->
            val placeholders = chunk.joinToString(separator = ",") { "?" }
            val selection = "${Phone.CONTACT_ID} IN ($placeholders)"
            val selectionArgs = chunk.map { it.toString() }.toTypedArray()

            rows.addAll(
                readDestinationRows(
                    uri = uri,
                    kind = kind,
                    selection = selection,
                    selectionArgs = selectionArgs,
                    sortOrder = null,
                ),
            )
        }

        return rows
    }

    private fun readDestinationRows(
        uri: Uri,
        kind: ContactDestination.Kind,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String?,
    ): List<DestinationRow> {
        val projection = when (kind) {
            ContactDestination.Kind.PHONE -> phoneProjection
            ContactDestination.Kind.EMAIL -> emailProjection
        }

        val destinationColumnName = when (kind) {
            ContactDestination.Kind.PHONE -> Phone.NUMBER
            ContactDestination.Kind.EMAIL -> Email.ADDRESS
        }

        return contentResolver
            .query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )
            ?.use { cursor ->
                mapDestinationRows(
                    cursor = cursor,
                    kind = kind,
                    destinationColumnName = destinationColumnName,
                )
            }.orEmpty()
    }

    private fun mapDestinationRows(
        cursor: Cursor,
        kind: ContactDestination.Kind,
        destinationColumnName: String,
    ): List<DestinationRow> {
        val columns = DestinationCursorColumns(
            dataIdIndex = cursor.getColumnIndexOrThrow(Phone._ID),
            contactIdIndex = cursor.getColumnIndexOrThrow(Phone.CONTACT_ID),
            destinationIndex = cursor.getColumnIndexOrThrow(destinationColumnName),
            displayNameIndex = cursor.getColumnIndexOrThrow(Phone.DISPLAY_NAME_PRIMARY),
            sortKeyIndex = cursor.getColumnIndexOrThrow(Phone.SORT_KEY_PRIMARY),
            photoUriIndex = cursor.getColumnIndexOrThrow(Phone.PHOTO_THUMBNAIL_URI),
            lookupKeyIndex = cursor.getColumnIndexOrThrow(Phone.LOOKUP_KEY),
            typeIndex = cursor.getColumnIndexOrThrow(Phone.TYPE),
            labelIndex = cursor.getColumnIndexOrThrow(Phone.LABEL),
            isPrimaryIndex = cursor.getColumnIndexOrThrow(Phone.IS_PRIMARY),
            isSuperPrimaryIndex = cursor.getColumnIndexOrThrow(Phone.IS_SUPER_PRIMARY),
        )

        val rows = mutableListOf<DestinationRow>()

        while (cursor.moveToNext()) {
            mapDestinationRow(
                cursor = cursor,
                kind = kind,
                columns = columns,
            )?.let(rows::add)
        }

        return rows
    }

    private fun mapDestinationRow(
        cursor: Cursor,
        kind: ContactDestination.Kind,
        columns: DestinationCursorColumns,
    ): DestinationRow? {
        val value = cursor
            .getString(columns.destinationIndex)
            ?.trim()
            .orEmpty()

        val contactId = cursor.getLong(columns.contactIdIndex)

        val isInvalidRow = value.isBlank() || contactId <= 0L

        if (isInvalidRow) {
            return null
        }

        val displayName = cursor
            .getString(columns.displayNameIndex)
            ?.trim()
            .orEmpty()
            .ifBlank { value }

        val photoUri = cursor
            .getString(columns.photoUriIndex)
            ?.takeIf { it.isNotBlank() }

        return DestinationRow(
            dataId = cursor.getLong(columns.dataIdIndex),
            contactId = contactId,
            lookupKey = cursor.getString(columns.lookupKeyIndex).orEmpty(),
            displayName = displayName,
            sortKey = cursor.getString(columns.sortKeyIndex)?.trim().orEmpty(),
            photoUri = photoUri,
            kind = kind,
            value = value,
            type = cursor.getInt(columns.typeIndex),
            customLabel = cursor.getString(columns.labelIndex)?.takeIf { it.isNotBlank() },
            isPrimary = cursor.getInt(columns.isPrimaryIndex) != 0,
            isSuperPrimary = cursor.getInt(columns.isSuperPrimaryIndex) != 0,
        )
    }

    private fun buildContact(
        rows: List<DestinationRow>,
        canonicalize: (String) -> String,
        formatPhoneForDisplay: (String) -> String,
    ): Contact {
        val first = rows.first()
        val orderedRows = rows.sortedWith(
            compareByDescending<DestinationRow> { it.isSuperPrimary }
                .thenByDescending { it.isPrimary }
                .thenBy { it.kind.ordinal }
                .thenBy { it.dataId },
        )

        val destinations = persistentListOf<ContactDestination>().builder()
        val seenNormalizedValues = LinkedHashSet<String>()
        orderedRows.forEach { row ->
            val normalizedValue = canonicalize(row.value)
            if (seenNormalizedValues.add(normalizedValue)) {
                val displayValue = when (row.kind) {
                    ContactDestination.Kind.EMAIL -> row.value
                    ContactDestination.Kind.PHONE -> formatPhoneForDisplay(row.value)
                }

                destinations.add(
                    ContactDestination(
                        dataId = row.dataId,
                        contactId = row.contactId,
                        value = row.value,
                        normalizedValue = normalizedValue,
                        displayValue = displayValue,
                        kind = row.kind,
                        type = row.type,
                        customLabel = row.customLabel,
                        isPrimary = row.isPrimary,
                        isSuperPrimary = row.isSuperPrimary,
                    ),
                )
            }
        }

        return Contact(
            id = first.contactId,
            lookupKey = first.lookupKey,
            displayName = first.displayName,
            photoUri = first.photoUri,
            destinations = destinations.build(),
        )
    }

    private fun paginateAndBuildContacts(
        contactGroups: List<List<DestinationRow>>,
        offset: Int,
    ): ContactsPage {
        val pageStart = offset.coerceAtMost(maximumValue = contactGroups.size)
        val pageEndExclusive = (pageStart + PAGE_SIZE)
            .coerceAtMost(maximumValue = contactGroups.size)

        val pageGroups = contactGroups.subList(fromIndex = pageStart, toIndex = pageEndExclusive)

        val pagedContacts = when {
            pageGroups.isEmpty() -> persistentListOf()
            else -> buildPageContacts(pageGroups = pageGroups)
        }

        val nextOffset = pageEndExclusive.takeIf { it < contactGroups.size }

        return ContactsPage(
            contacts = pagedContacts,
            nextOffset = nextOffset,
        )
    }

    private fun buildPageContacts(
        pageGroups: List<List<DestinationRow>>,
    ): PersistentList<Contact> {
        val countryCandidates = formatter.countryCandidates()
        val canonicalCache = HashMap<String, String>()
        val displayCache = HashMap<String, String>()

        val canonicalize: (String) -> String = { value ->
            canonicalCache.getOrPut(value) {
                formatter.canonicalize(
                    value = value,
                    countryCandidates = countryCandidates,
                )
            }
        }

        val formatPhoneForDisplay: (String) -> String = { value ->
            displayCache.getOrPut(value) { formatter.formatPhoneForDisplay(value) }
        }

        return pageGroups
            .map { rows ->
                buildContact(
                    rows = rows,
                    canonicalize = canonicalize,
                    formatPhoneForDisplay = formatPhoneForDisplay,
                )
            }
            .toPersistentList()
    }

    private fun createDefaultPhoneQueryUri(): Uri {
        return Phone.CONTENT_URI
            .buildUpon()
            .appendQueryParameter(
                ContactsContract.DIRECTORY_PARAM_KEY,
                Directory.DEFAULT.toString(),
            )
            .build()
    }

    private fun createDefaultEmailQueryUri(): Uri {
        return Email.CONTENT_URI
            .buildUpon()
            .appendQueryParameter(
                ContactsContract.DIRECTORY_PARAM_KEY,
                Directory.DEFAULT.toString(),
            )
            .build()
    }

    private fun createPhoneFilterUri(query: String): Uri {
        return Phone.CONTENT_FILTER_URI
            .buildUpon()
            .appendPath(query)
            .appendQueryParameter(
                ContactsContract.DIRECTORY_PARAM_KEY,
                Directory.DEFAULT.toString(),
            )
            .build()
    }

    private fun createEmailFilterUri(query: String): Uri {
        return Email.CONTENT_FILTER_URI
            .buildUpon()
            .appendPath(query)
            .appendQueryParameter(
                ContactsContract.DIRECTORY_PARAM_KEY,
                Directory.DEFAULT.toString(),
            )
            .build()
    }

    private fun extractDigits(value: String): String {
        return value.filter { character -> character.isDigit() }
    }

    private data class DestinationRow(
        val dataId: Long,
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val sortKey: String,
        val photoUri: String?,
        val kind: ContactDestination.Kind,
        val value: String,
        val type: Int,
        val customLabel: String?,
        val isPrimary: Boolean,
        val isSuperPrimary: Boolean,
    )

    private data class DestinationCursorColumns(
        val dataIdIndex: Int,
        val contactIdIndex: Int,
        val destinationIndex: Int,
        val displayNameIndex: Int,
        val sortKeyIndex: Int,
        val photoUriIndex: Int,
        val lookupKeyIndex: Int,
        val typeIndex: Int,
        val labelIndex: Int,
        val isPrimaryIndex: Int,
        val isSuperPrimaryIndex: Int,
    )

    private companion object {
        private const val PAGE_SIZE = 200
        private const val CONTACT_ID_CHUNK_SIZE = 500

        private const val SORT_BY_SORT_KEY_PRIMARY_ASC = "${Phone.SORT_KEY_PRIMARY} ASC"

        private val phoneProjection by lazy {
            arrayOf(
                Phone._ID,
                Phone.CONTACT_ID,
                Phone.LOOKUP_KEY,
                Phone.DISPLAY_NAME_PRIMARY,
                Phone.SORT_KEY_PRIMARY,
                Phone.PHOTO_THUMBNAIL_URI,
                Phone.NUMBER,
                Phone.TYPE,
                Phone.LABEL,
                Phone.IS_PRIMARY,
                Phone.IS_SUPER_PRIMARY,
            )
        }

        private val emailProjection by lazy {
            arrayOf(
                Email._ID,
                Email.CONTACT_ID,
                Email.LOOKUP_KEY,
                Email.DISPLAY_NAME_PRIMARY,
                Email.SORT_KEY_PRIMARY,
                Email.PHOTO_THUMBNAIL_URI,
                Email.ADDRESS,
                Email.TYPE,
                Email.LABEL,
                Email.IS_PRIMARY,
                Email.IS_SUPER_PRIMARY,
            )
        }
    }
}
