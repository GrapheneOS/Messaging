package com.android.messaging.data.vcarddetail.mapper

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.android.messaging.R
import com.android.messaging.data.vcard.mapper.VCardEntrySummarizer
import com.android.messaging.data.vcarddetail.model.VCardContact
import com.android.messaging.data.vcarddetail.model.VCardField
import com.android.messaging.data.vcarddetail.model.VCardFieldAction
import com.android.messaging.datamodel.media.CustomVCardEntry
import com.android.vcard.VCardConfig
import com.android.vcard.VCardEntry.EmailData
import com.android.vcard.VCardEntry.ImData
import com.android.vcard.VCardEntry.OrganizationData
import com.android.vcard.VCardEntry.PhoneData
import com.android.vcard.VCardEntry.PostalData
import com.android.vcard.VCardEntry.WebsiteData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface VCardDetailMapper {
    fun map(entries: List<CustomVCardEntry>): ImmutableList<VCardContact>
}

internal class VCardDetailMapperImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val entrySummarizer: VCardEntrySummarizer,
) : VCardDetailMapper {

    override fun map(entries: List<CustomVCardEntry>): ImmutableList<VCardContact> {
        return entries
            .map(::mapContact)
            .toImmutableList()
    }

    private fun mapContact(entry: CustomVCardEntry): VCardContact {
        return VCardContact(
            displayName = entrySummarizer.displayName(entry),
            avatarPhoto = entrySummarizer.avatarPhoto(entry),
            fields = contactFields(entry).toImmutableList(),
        )
    }

    private fun contactFields(entry: CustomVCardEntry): List<VCardField> {
        return buildList {
            addAll(phoneFields(entry))
            addAll(emailFields(entry))
            addAll(postalFields(entry))
            addAll(imFields(entry))
            addAll(organizationFields(entry))
            addAll(websiteFields(entry))
            addAll(simpleFields(entry))
        }
    }

    private fun phoneFields(entry: CustomVCardEntry): List<VCardField> {
        return entry.phoneList.orEmpty().mapNotNull(::phoneField)
    }

    private fun emailFields(entry: CustomVCardEntry): List<VCardField> {
        return entry.emailList.orEmpty().mapNotNull(::emailField)
    }

    private fun postalFields(entry: CustomVCardEntry): List<VCardField> {
        return entry.postalList.orEmpty().mapNotNull(::postalField)
    }

    private fun imFields(entry: CustomVCardEntry): List<VCardField> {
        return entry.imList.orEmpty().mapNotNull(::imField)
    }

    private fun organizationFields(entry: CustomVCardEntry): List<VCardField> {
        return entry.organizationList.orEmpty().mapNotNull(::organizationField)
    }

    private fun websiteFields(entry: CustomVCardEntry): List<VCardField> {
        return entry.websiteList.orEmpty().mapNotNull(::websiteField)
    }

    private fun phoneField(phone: PhoneData): VCardField? {
        val number = phone.number.asNonBlank() ?: return null
        val label = Phone.getTypeLabel(context.resources, phone.type, phone.label)?.toString()

        return VCardField(
            value = number,
            label = label,
            action = VCardFieldAction.Dial(number),
        )
    }

    private fun emailField(email: EmailData): VCardField? {
        val address = email.address.asNonBlank() ?: return null
        val label = Email.getTypeLabel(context.resources, email.type, email.label)?.toString()

        return VCardField(
            value = address,
            label = label,
            action = VCardFieldAction.Email(address),
        )
    }

    private fun postalField(postal: PostalData): VCardField? {
        val address = postal
            .getFormattedAddress(VCardConfig.VCARD_TYPE_UNKNOWN)
            .asNonBlank()
            ?: return null

        return VCardField(
            value = address,
            label = postalLabel(postal),
            action = VCardFieldAction.OpenMap(address),
        )
    }

    private fun imField(im: ImData): VCardField? {
        val address = im.address.asNonBlank() ?: return null

        return VCardField(
            value = address,
            label = imProtocolLabel(im),
            action = VCardFieldAction.None,
        )
    }

    private fun organizationField(organization: OrganizationData): VCardField? {
        val value = organization.formattedString.asNonBlank() ?: return null

        return VCardField(
            value = value,
            label = organizationLabel(organization),
            action = VCardFieldAction.None,
        )
    }

    private fun websiteField(website: WebsiteData): VCardField? {
        val value = website.website.asNonBlank() ?: return null

        return VCardField(
            value = value,
            label = null,
            action = VCardFieldAction.OpenUrl(normalizedWebsiteUrl(value)),
        )
    }

    private fun simpleFields(entry: CustomVCardEntry): List<VCardField> {
        return buildList {
            entry.birthday.asNonBlank()?.let { birthday ->
                add(plainField(birthday, R.string.vcard_detail_birthday_label))
            }

            entry.notes.orEmpty().forEach { note ->
                note.note.asNonBlank()?.let { text ->
                    add(plainField(text, R.string.vcard_detail_notes_label))
                }
            }

            propertyValue(entry, PROPERTY_NICKNAME)?.let { nickname ->
                add(plainField(nickname, R.string.vcard_detail_nickname_label))
            }

            propertyValue(entry, PROPERTY_ANNIVERSARY)?.let { anniversary ->
                add(plainField(anniversary, R.string.vcard_detail_anniversary_label))
            }
        }
    }

    private fun propertyValue(
        entry: CustomVCardEntry,
        propertyName: String,
    ): String? {
        return entry.getProperty(propertyName)
            ?.rawValue
            .asNonBlank()
    }

    private fun postalLabel(postal: PostalData): String? {
        val postalTypes = context.resources.getStringArray(android.R.array.postalAddressTypes)

        return postalTypes.getOrNull(postal.type - 1)
            ?: postalTypes.getOrNull(FALLBACK_POSTAL_TYPE_INDEX)
    }

    private fun organizationLabel(organization: OrganizationData): String? {
        val organizationTypes = context.resources.getStringArray(android.R.array.organizationTypes)

        return try {
            context.resources.getString(Organization.getTypeLabelResource(organization.type))
        } catch (_: NotFoundException) {
            organizationTypes.getOrNull(FALLBACK_ORGANIZATION_TYPE_INDEX)
        }
    }

    private fun imProtocolLabel(im: ImData): String? {
        return when (im.protocol) {
            PROTOCOL_CUSTOM -> im.customProtocol.asNonBlank() ?: IM_PROTOCOL_CUSTOM_LABEL
            PROTOCOL_AIM -> "AIM"
            PROTOCOL_MSN -> "MSN"
            PROTOCOL_YAHOO -> "Yahoo"
            PROTOCOL_SKYPE -> "Skype"
            PROTOCOL_QQ -> "QQ"
            PROTOCOL_GOOGLE_TALK -> "Google Talk"
            PROTOCOL_ICQ -> "ICQ"
            PROTOCOL_JABBER -> "Jabber"
            PROTOCOL_NETMEETING -> "NetMeeting"
            else -> im.customProtocol.asNonBlank() ?: IM_PROTOCOL_CUSTOM_LABEL
        }
    }

    private fun normalizedWebsiteUrl(value: String): String {
        return when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            else -> URL_SCHEME_PREFIX + value
        }
    }

    private fun plainField(
        value: String,
        labelResId: Int,
    ): VCardField {
        return VCardField(
            value = value,
            label = context.resources.getString(labelResId),
            action = VCardFieldAction.None,
        )
    }

    private fun String?.asNonBlank(): String? {
        return this?.takeIf(String::isNotBlank)
    }

    private companion object {
        private const val PROPERTY_NICKNAME = "NICKNAME"
        private const val PROPERTY_ANNIVERSARY = "ANNIVERSARY"
        private const val FALLBACK_POSTAL_TYPE_INDEX = 2
        private const val FALLBACK_ORGANIZATION_TYPE_INDEX = 1
        private const val URL_SCHEME_PREFIX = "http://"
        private const val IM_PROTOCOL_CUSTOM_LABEL = "Custom"
        private const val PROTOCOL_CUSTOM = -1
        private const val PROTOCOL_AIM = 0
        private const val PROTOCOL_MSN = 1
        private const val PROTOCOL_YAHOO = 2
        private const val PROTOCOL_SKYPE = 3
        private const val PROTOCOL_QQ = 4
        private const val PROTOCOL_GOOGLE_TALK = 5
        private const val PROTOCOL_ICQ = 6
        private const val PROTOCOL_JABBER = 7
        private const val PROTOCOL_NETMEETING = 8
    }
}
