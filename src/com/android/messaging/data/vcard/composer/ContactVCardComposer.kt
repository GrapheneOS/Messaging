package com.android.messaging.data.vcard.composer

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Entity
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import android.provider.ContactsContract.RawContactsEntity
import android.telephony.PhoneNumberUtils
import androidx.core.database.getLongOrNull
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.util.LogUtil
import com.android.vcard.VCardComposer
import com.android.vcard.VCardConfig
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.MatchType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

internal interface ContactVCardComposer {
    fun composeToScratchSpace(contactUris: List<Uri>, includePhotos: Boolean): Uri?
}

internal class ContactVCardComposerImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val contentResolver: ContentResolver,
) : ContactVCardComposer {

    override fun composeToScratchSpace(contactUris: List<Uri>, includePhotos: Boolean): Uri? {
        val vCardType = vCardType(includePhotos = includePhotos)
        val vCardText = contactUris
            .mapNotNull { contactUri ->
                composeContactVCard(contactUri = contactUri, vCardType = vCardType)
            }
            .joinToString(separator = "")

        return when {
            vCardText.isEmpty() -> null
            else -> writeToScratchSpace(vCardText = vCardText)
        }
    }

    private fun vCardType(includePhotos: Boolean): Int {
        return when {
            includePhotos -> VCardConfig.VCARD_TYPE_DEFAULT
            else -> VCardConfig.VCARD_TYPE_DEFAULT or VCardConfig.FLAG_REFRAIN_IMAGE_EXPORT
        }
    }

    private fun composeContactVCard(contactUri: Uri, vCardType: Int): String? {
        val contact = queryContact(contactUri = contactUri)
        val rawContacts = contact?.let(::queryRawContacts).orEmpty()

        return when {
            contact == null || rawContacts.isEmpty() -> {
                LogUtil.w(TAG, "No contact data to compose a vCard for $contactUri")
                null
            }

            else -> {
                VCardComposer(context, vCardType, false).buildVCard(
                    mergeRawContacts(contact = contact, rawContacts = rawContacts),
                )
            }
        }
    }

    private fun queryContact(contactUri: Uri): ContactRecord? {
        return contentResolver.query(
            contactUri,
            CONTACT_PROJECTION,
            null,
            null,
            null,
        )?.use { cursor ->
            when {
                cursor.moveToFirst() -> {
                    ContactRecord(
                        contactId = cursor.getLong(cursor.getColumnIndexOrThrow(Contacts._ID)),
                        nameRawContactId = cursor.getLongOrNull(
                            cursor.getColumnIndexOrThrow(Contacts.NAME_RAW_CONTACT_ID),
                        ),
                        photoId = cursor.getLongOrNull(
                            cursor.getColumnIndexOrThrow(Contacts.PHOTO_ID),
                        ),
                    )
                }

                else -> null
            }
        }
    }

    private fun queryRawContacts(contact: ContactRecord): List<Entity> {
        val rawContactsEntityUri = when {
            ContactsContract.isProfileId(contact.contactId) -> {
                RawContactsEntity.PROFILE_CONTENT_URI
            }

            else -> RawContactsEntity.CONTENT_URI
        }

        val cursor = contentResolver.query(
            rawContactsEntityUri,
            null,
            RAW_CONTACTS_SELECTION,
            arrayOf(contact.contactId.toString()),
            RAW_CONTACTS_SORT_ORDER,
        ) ?: return emptyList()

        val entityIterator = RawContacts.newEntityIterator(cursor)

        return try {
            entityIterator.asSequence().toList()
        } finally {
            entityIterator.close()
        }
    }

    private fun mergeRawContacts(
        contact: ContactRecord,
        rawContacts: List<Entity>,
    ): Map<String, List<ContentValues>> {
        return rawContacts
            .sortedBy { rawContact ->
                rawContact.entityValues.getAsLong(RawContacts._ID) != contact.nameRawContactId
            }
            .flatMap { rawContact -> rawContact.subValues }
            .mapNotNull { dataRow ->
                dataRow.values.getAsString(Data.MIMETYPE)?.let { mimeType ->
                    mimeType to dataRow.values
                }
            }
            .groupBy(
                keySelector = { (mimeType, _) -> mimeType },
                valueTransform = { (_, values) -> values },
            )
            .mapValues { (mimeType, dataRows) ->
                distinctDataRows(
                    mimeType = mimeType,
                    dataRows = dataRows,
                    photoId = contact.photoId,
                )
            }
    }

    private fun distinctDataRows(
        mimeType: String,
        dataRows: List<ContentValues>,
        photoId: Long?,
    ): List<ContentValues> {
        return when (mimeType) {
            StructuredName.CONTENT_ITEM_TYPE -> {
                listOfNotNull(selectName(dataRows = dataRows))
            }

            Photo.CONTENT_ITEM_TYPE -> {
                listOfNotNull(selectPhoto(dataRows = dataRows, photoId = photoId))
            }

            Phone.CONTENT_ITEM_TYPE -> {
                distinctPhones(dataRows = dataRows)
            }

            else -> {
                dataRows.distinctBy(::dataColumnsKey)
            }
        }
    }

    private fun selectName(dataRows: List<ContentValues>): ContentValues? {
        return dataRows
            .firstOrNull { dataRow ->
                !dataRow.getAsString(StructuredName.DISPLAY_NAME).isNullOrBlank()
            }
            ?: dataRows.firstOrNull()
    }

    private fun selectPhoto(dataRows: List<ContentValues>, photoId: Long?): ContentValues? {
        val photos = dataRows.filter { dataRow ->
            dataRow.getAsByteArray(Photo.PHOTO) != null
        }

        return photos
            .firstOrNull { photo -> photo.getAsLong(Data._ID) == photoId }
            ?: photos.firstOrNull()
    }

    private fun distinctPhones(dataRows: List<ContentValues>): List<ContentValues> {
        return dataRows.fold(initial = emptyList()) { keptRows, dataRow ->
            val isDuplicate = keptRows.any { keptRow ->
                isSamePhone(first = keptRow, second = dataRow)
            }

            when {
                isDuplicate -> keptRows
                else -> keptRows + dataRow
            }
        }
    }

    private fun isSamePhone(first: ContentValues, second: ContentValues): Boolean {
        val firstNumber = first.getAsString(Phone.NUMBER).orEmpty()
        val secondNumber = second.getAsString(Phone.NUMBER).orEmpty()
        val firstNormalizedNumber = first.getAsString(Phone.NORMALIZED_NUMBER)
        val secondNormalizedNumber = second.getAsString(Phone.NORMALIZED_NUMBER)

        val isSameNumber = when {
            firstNormalizedNumber.isNullOrBlank() || secondNormalizedNumber.isNullOrBlank() -> {
                isPhoneNumberMatch(firstNumber = firstNumber, secondNumber = secondNumber)
            }

            else -> firstNormalizedNumber == secondNormalizedNumber
        }

        return isSameNumber &&
            PhoneNumberUtils.extractPostDialPortion(firstNumber) ==
            PhoneNumberUtils.extractPostDialPortion(secondNumber)
    }

    private fun isPhoneNumberMatch(firstNumber: String, secondNumber: String): Boolean {
        return when (PhoneNumberUtil.getInstance().isNumberMatch(firstNumber, secondNumber)) {
            MatchType.EXACT_MATCH, MatchType.NSN_MATCH -> true
            else -> false
        }
    }

    private fun dataColumnsKey(dataRow: ContentValues): List<String?> {
        return DATA_COLUMNS.map(dataRow::getAsString)
    }

    private fun writeToScratchSpace(vCardText: String): Uri? {
        val scratchUri = MediaScratchFileProvider.buildMediaScratchSpaceUri(VCARD_FILE_EXTENSION)

        return try {
            val outputStream = contentResolver
                .openOutputStream(scratchUri)
                ?: throw IOException("Unable to open output stream for $scratchUri")

            outputStream.use { stream ->
                stream.write(vCardText.toByteArray(Charsets.UTF_8))
            }

            scratchUri
        } catch (e: IOException) {
            LogUtil.w(TAG, "Failed to write the contact vCard to $scratchUri", e)
            runCatching { contentResolver.delete(scratchUri, null, null) }
            null
        }
    }

    private data class ContactRecord(
        val contactId: Long,
        val nameRawContactId: Long?,
        val photoId: Long?,
    )

    private companion object {
        private const val TAG = "ContactVCardComposer"

        private const val VCARD_FILE_EXTENSION = "vcf"

        private const val RAW_CONTACTS_SELECTION =
            "${RawContactsEntity.CONTACT_ID} = ? AND ${RawContactsEntity.DELETED} = 0"

        private const val RAW_CONTACTS_SORT_ORDER =
            "${RawContactsEntity._ID}, ${RawContactsEntity.DATA_ID}"

        private val CONTACT_PROJECTION = arrayOf(
            Contacts._ID,
            Contacts.NAME_RAW_CONTACT_ID,
            Contacts.PHOTO_ID,
        )

        private val DATA_COLUMNS = listOf(
            Data.DATA1,
            Data.DATA2,
            Data.DATA3,
            Data.DATA4,
            Data.DATA5,
            Data.DATA6,
            Data.DATA7,
            Data.DATA8,
            Data.DATA9,
            Data.DATA10,
            Data.DATA11,
            Data.DATA12,
            Data.DATA13,
            Data.DATA14,
            Data.DATA15,
        )
    }
}
