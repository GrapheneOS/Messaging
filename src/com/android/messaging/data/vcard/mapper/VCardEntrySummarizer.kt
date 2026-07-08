package com.android.messaging.data.vcard.mapper

import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.vcard.model.VCardAvatarPhoto
import com.android.messaging.data.vcard.photo.VCardPhotoDownscaler
import com.android.messaging.datamodel.media.CustomVCardEntry
import com.android.vcard.VCardConfig
import javax.inject.Inject

internal interface VCardEntrySummarizer {
    fun displayName(entry: CustomVCardEntry): String?
    fun avatarPhoto(entry: CustomVCardEntry): VCardAvatarPhoto?
    fun normalizedDestination(entry: CustomVCardEntry): String?
    fun isLocation(entry: CustomVCardEntry): Boolean
    fun firstPostalAddress(entry: CustomVCardEntry): String?
}

internal class VCardEntrySummarizerImpl @Inject constructor(
    private val photoDownscaler: VCardPhotoDownscaler,
    private val destinationFormatter: ContactDestinationFormatter,
) : VCardEntrySummarizer {

    override fun displayName(entry: CustomVCardEntry): String? {
        val name = entry.displayName ?: run {
            entry.consolidateFields()
            entry.displayName
        }

        return name?.takeIf(String::isNotBlank)
    }

    override fun avatarPhoto(entry: CustomVCardEntry): VCardAvatarPhoto? {
        val photoBytes = entry.photoList
            ?.firstNotNullOfOrNull { photo -> photo.bytes }
            ?: return null

        return photoDownscaler.downscale(photoBytes)?.let(::VCardAvatarPhoto)
    }

    override fun normalizedDestination(entry: CustomVCardEntry): String? {
        return firstNormalizedPhoneNumber(entry)
            ?: firstNormalizedEmailAddress(entry)
    }

    override fun isLocation(entry: CustomVCardEntry): Boolean {
        return entry.getProperty(PROPERTY_KIND)
            ?.rawValue
            ?.equals(KIND_LOCATION, ignoreCase = true) == true
    }

    override fun firstPostalAddress(entry: CustomVCardEntry): String? {
        return entry.postalList
            ?.firstOrNull()
            ?.getFormattedAddress(VCardConfig.VCARD_TYPE_UNKNOWN)
            ?.takeIf(String::isNotBlank)
    }

    private fun firstNormalizedPhoneNumber(entry: CustomVCardEntry): String? {
        val phones = entry.phoneList.orEmpty()
        if (phones.isEmpty()) return null

        val countryCandidates = destinationFormatter.countryCandidates()

        return phones.firstNotNullOfOrNull { phone ->
            phone.number
                .asNonBlank()
                ?.let { number ->
                    destinationFormatter.canonicalize(
                        value = number,
                        countryCandidates = countryCandidates,
                    )
                }
                .asNonBlank()
        }
    }

    private fun firstNormalizedEmailAddress(entry: CustomVCardEntry): String? {
        return entry.emailList
            .orEmpty()
            .firstNotNullOfOrNull { email ->
                email.address
                    .asNonBlank()
                    ?.let(destinationFormatter::canonicalize)
                    .asNonBlank()
            }
    }

    private fun String?.asNonBlank(): String? {
        return this
            ?.trim()
            ?.takeIf(String::isNotBlank)
    }

    private companion object {
        private const val PROPERTY_KIND = "KIND"
        private const val KIND_LOCATION = "location"
    }
}
