package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Directory
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.core.extension.typedFlow
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal interface ConversationRecipientsRepository {

    fun searchRecipients(
        query: String,
        offset: Int,
    ): Flow<ConversationRecipientsPage>
}

internal class ConversationRecipientsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationRecipientsRepository {

    override fun searchRecipients(
        query: String,
        offset: Int,
    ): Flow<ConversationRecipientsPage> {
        return typedFlow {
            queryRecipients(
                query = query,
                offset = offset,
            )
        }.flowOn(ioDispatcher)
    }

    private fun queryRecipients(
        query: String,
        offset: Int,
    ): ConversationRecipientsPage {
        val recipients = when {
            query.isBlank() -> queryPhoneRecipients(query = query)
            else -> queryMergedRecipients(query = query)
        }

        return paginateRecipients(
            recipients = recipients,
            offset = offset,
        )
    }

    private fun queryMergedRecipients(query: String): ImmutableList<RecipientSearchEntry> {
        val phoneRecipients = queryPhoneRecipients(query = query)
        val emailRecipients = queryEmailRecipients(query = query)

        val mergedRecipients = mergeRecipients(
            phoneRecipients = phoneRecipients,
            emailRecipients = emailRecipients,
        )

        val shouldUseFallback = mergedRecipients.isEmpty() &&
            shouldUsePhoneDigitsFallback(query = query)

        return when {
            shouldUseFallback -> queryPhoneRecipients(
                query = "",
                matchesRecipient = createPhoneDigitsMatcher(query = query),
            )
            else -> mergedRecipients
        }
    }

    private fun queryPhoneRecipients(
        query: String,
        matchesRecipient: (ConversationRecipient) -> Boolean = { true },
    ): ImmutableList<RecipientSearchEntry> {
        val uri = when {
            query.isBlank() -> createDefaultPhoneQueryUri()
            else -> createPhoneQueryUri(query = query)
        }

        return queryRecipientEntries(
            uri = uri,
            projection = phoneProjection,
            queryArgs = phoneQueryArgs,
            destinationColumnName = Phone.NUMBER,
            matchesRecipient = matchesRecipient,
        )
    }

    private fun queryEmailRecipients(query: String): ImmutableList<RecipientSearchEntry> {
        return when {
            query.isNotBlank() -> {
                queryRecipientEntries(
                    uri = createEmailQueryUri(query = query),
                    projection = emailProjection,
                    queryArgs = emailQueryArgs,
                    destinationColumnName = Email.ADDRESS,
                    matchesRecipient = { true },
                )
            }

            else -> persistentListOf()
        }
    }

    private fun queryRecipientEntries(
        uri: Uri,
        projection: Array<String>,
        queryArgs: Bundle,
        destinationColumnName: String,
        matchesRecipient: (ConversationRecipient) -> Boolean,
    ): ImmutableList<RecipientSearchEntry> {
        return contentResolver
            .query(
                uri,
                projection,
                queryArgs,
                null,
            )
            ?.use { cursor ->
                val recipientCursorColumns = resolveRecipientCursorColumns(
                    cursor = cursor,
                    destinationColumnName = destinationColumnName,
                )

                mapRecipientEntries(
                    cursor = cursor,
                    recipientCursorColumns = recipientCursorColumns,
                    matchesRecipient = matchesRecipient,
                )
            }
            ?: persistentListOf()
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

    private fun createEmailQueryUri(query: String): Uri {
        return Email.CONTENT_FILTER_URI
            .buildUpon()
            .appendPath(query)
            .appendQueryParameter(
                ContactsContract.DIRECTORY_PARAM_KEY,
                Directory.DEFAULT.toString(),
            )
            .build()
    }

    private fun createPhoneQueryUri(query: String): Uri {
        return Phone.CONTENT_FILTER_URI
            .buildUpon()
            .appendPath(query)
            .appendQueryParameter(
                ContactsContract.DIRECTORY_PARAM_KEY,
                Directory.DEFAULT.toString(),
            )
            .build()
    }

    private fun mapRecipientEntries(
        cursor: Cursor,
        recipientCursorColumns: RecipientCursorColumns,
        matchesRecipient: (ConversationRecipient) -> Boolean,
    ): ImmutableList<RecipientSearchEntry> {
        val recipients = persistentListOf<RecipientSearchEntry>().builder()

        while (cursor.moveToNext()) {
            val recipientEntry = mapRecipientEntry(
                cursor = cursor,
                recipientCursorColumns = recipientCursorColumns,
            ) ?: continue

            if (!matchesRecipient(recipientEntry.recipient)) {
                continue
            }

            recipients.add(recipientEntry)
        }

        return recipients.build()
    }

    private fun resolveRecipientCursorColumns(
        cursor: Cursor,
        destinationColumnName: String,
    ): RecipientCursorColumns {
        return RecipientCursorColumns(
            dataIdIndex = cursor.getColumnIndexOrThrow(Phone._ID),
            destinationIndex = cursor.getColumnIndexOrThrow(destinationColumnName),
            displayNameIndex = cursor.getColumnIndexOrThrow(Phone.DISPLAY_NAME_PRIMARY),
            photoUriIndex = cursor.getColumnIndexOrThrow(Phone.PHOTO_THUMBNAIL_URI),
            sortKeyIndex = cursor.getColumnIndexOrThrow(Phone.SORT_KEY_PRIMARY),
        )
    }

    private fun mapRecipientEntry(
        cursor: Cursor,
        recipientCursorColumns: RecipientCursorColumns,
    ): RecipientSearchEntry? {
        val destination = cursor
            .getString(recipientCursorColumns.destinationIndex)
            ?.trim()
            .orEmpty()

        if (destination.isBlank()) {
            return null
        }

        val displayName = cursor
            .getString(recipientCursorColumns.displayNameIndex)
            ?.trim()
            .orEmpty()
            .ifBlank { destination }

        val photoUri = cursor
            .getString(recipientCursorColumns.photoUriIndex)
            ?.takeIf { it.isNotBlank() }

        val secondaryText = when {
            displayName == destination -> null
            else -> destination
        }

        return RecipientSearchEntry(
            recipient = ConversationRecipient(
                id = cursor.getLong(recipientCursorColumns.dataIdIndex).toString(),
                displayName = displayName,
                destination = destination,
                photoUri = photoUri,
                secondaryText = secondaryText,
            ),
            sortKey = cursor
                .getString(recipientCursorColumns.sortKeyIndex)
                ?.trim()
                .orEmpty(),
        )
    }

    private fun mergeRecipients(
        phoneRecipients: List<RecipientSearchEntry>,
        emailRecipients: List<RecipientSearchEntry>,
    ): ImmutableList<RecipientSearchEntry> {
        val sortedRecipients = (phoneRecipients + emailRecipients).sortedWith(
            compareBy<RecipientSearchEntry> { it.sortKey }
                .thenBy { it.recipient.displayName }
                .thenBy { it.recipient.destination },
        )

        val seenDestinations = LinkedHashSet<String>()

        return sortedRecipients
            .asSequence()
            .filter { recipient ->
                seenDestinations.add(recipient.recipient.destination)
            }
            .toPersistentList()
    }

    private fun paginateRecipients(
        recipients: List<RecipientSearchEntry>,
        offset: Int,
    ): ConversationRecipientsPage {
        if (offset >= recipients.size) {
            return emptyRecipientsPage()
        }

        val pagedRecipients = persistentListOf<ConversationRecipient>().builder()

        for (index in offset until recipients.size) {
            if (pagedRecipients.size == PAGE_SIZE) {
                return ConversationRecipientsPage(
                    recipients = pagedRecipients.build(),
                    nextOffset = index,
                )
            }

            pagedRecipients.add(recipients[index].recipient)
        }

        return ConversationRecipientsPage(
            recipients = pagedRecipients.build(),
            nextOffset = null,
        )
    }

    private fun shouldUsePhoneDigitsFallback(query: String): Boolean {
        return query.any { character -> character.isDigit() }
    }

    private fun createPhoneDigitsMatcher(query: String): (ConversationRecipient) -> Boolean {
        val queryDigits = extractDigits(value = query)

        return { recipient ->
            val destinationDigits = extractDigits(value = recipient.destination)
            destinationDigits.contains(queryDigits)
        }
    }

    private fun extractDigits(value: String): String {
        return value.filter { character -> character.isDigit() }
    }

    private fun emptyRecipientsPage(): ConversationRecipientsPage {
        return ConversationRecipientsPage(
            recipients = persistentListOf(),
            nextOffset = null,
        )
    }

    private companion object {
        private const val PAGE_SIZE = 200

        private val phoneProjection by lazy {
            arrayOf(
                Phone.CONTACT_ID,
                Phone.DISPLAY_NAME_PRIMARY,
                Phone.PHOTO_THUMBNAIL_URI,
                Phone.NUMBER,
                Phone.TYPE,
                Phone.LABEL,
                Phone.LOOKUP_KEY,
                Phone._ID,
                Phone.SORT_KEY_PRIMARY,
            )
        }

        private val emailProjection by lazy {
            arrayOf(
                Email.CONTACT_ID,
                Email.DISPLAY_NAME_PRIMARY,
                Email.PHOTO_THUMBNAIL_URI,
                Email.ADDRESS,
                Email.TYPE,
                Email.LABEL,
                Email.LOOKUP_KEY,
                Email._ID,
                Email.SORT_KEY_PRIMARY,
            )
        }

        private val phoneQueryArgs by lazy {
            Bundle().apply {
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(Phone.SORT_KEY_PRIMARY),
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_ASCENDING,
                )
            }
        }

        private val emailQueryArgs by lazy {
            Bundle().apply {
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(Email.SORT_KEY_PRIMARY),
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_ASCENDING,
                )
            }
        }
    }
}

private data class RecipientCursorColumns(
    val dataIdIndex: Int,
    val destinationIndex: Int,
    val displayNameIndex: Int,
    val photoUriIndex: Int,
    val sortKeyIndex: Int,
)

private data class RecipientSearchEntry(
    val recipient: ConversationRecipient,
    val sortKey: String,
)
