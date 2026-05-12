package com.android.messaging.data.contact.formatter

import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.util.PhoneUtils
import java.util.Locale
import javax.inject.Inject

internal interface ContactDestinationFormatter {

    fun canonicalize(value: String): String

    fun canonicalize(value: String, countryCandidates: List<String>): String

    fun formatPhoneForDisplay(value: String): String

    fun countryCandidates(): List<String>
}

internal class ContactDestinationFormatterImpl @Inject constructor() : ContactDestinationFormatter {

    override fun canonicalize(value: String): String {
        return canonicalize(value = value) { trimmed ->
            PhoneUtils
                .getDefault()
                .getCanonicalForEnteredPhoneNumber(trimmed)
        }
    }

    override fun canonicalize(
        value: String,
        countryCandidates: List<String>,
    ): String {
        return canonicalize(value = value) { trimmed ->
            PhoneUtils
                .getDefault()
                .getCanonicalForEnteredPhoneNumber(trimmed, countryCandidates)
        }
    }

    override fun formatPhoneForDisplay(value: String): String {
        return PhoneUtils.getDefault().formatForDisplay(value)
    }

    override fun countryCandidates(): List<String> {
        val phoneUtils = PhoneUtils
            .getDefault()
            .apply {
                warmUp()
            }

        return phoneUtils.countryCandidatesForEnteredPhoneNumber
    }

    private inline fun canonicalize(
        value: String,
        canonicalizePhoneNumber: (trimmed: String) -> String,
    ): String {
        val trimmed = value.trim()

        return when {
            trimmed.isEmpty() -> trimmed
            MmsSmsUtils.isEmailAddress(trimmed) -> trimmed.lowercase(Locale.ROOT)
            else -> canonicalizePhoneNumber(trimmed)
        }
    }
}
