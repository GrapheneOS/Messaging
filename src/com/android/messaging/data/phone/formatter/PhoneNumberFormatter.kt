package com.android.messaging.data.phone.formatter

import com.android.messaging.util.PhoneUtils
import javax.inject.Inject

internal interface PhoneNumberFormatter {
    fun formatForDisplay(number: String): String
    fun formatForDisplayUsingSimCountry(number: String): String
    fun formatNormalizedUsingSimCountry(number: String): String
    fun getCanonicalForEnteredNumber(number: String): String
    fun getCanonicalForEnteredNumber(number: String, countryCandidates: List<String>): String
    fun countryCandidates(): List<String>
}

internal class PhoneNumberFormatterImpl @Inject constructor(
    private val phoneUtils: PhoneUtils,
) : PhoneNumberFormatter {

    override fun formatForDisplay(number: String): String {
        return phoneUtils.formatForDisplay(number)
    }

    override fun formatForDisplayUsingSimCountry(number: String): String {
        return phoneUtils.formatForDisplayUsingSimCountry(number).orEmpty()
    }

    override fun formatNormalizedUsingSimCountry(number: String): String {
        return phoneUtils.formatNormalizedDestinationUsingSimCountry(number).orEmpty()
    }

    override fun getCanonicalForEnteredNumber(number: String): String {
        return phoneUtils.getCanonicalForEnteredPhoneNumber(number)
    }

    override fun getCanonicalForEnteredNumber(
        number: String,
        countryCandidates: List<String>,
    ): String {
        return phoneUtils.getCanonicalForEnteredPhoneNumber(number, countryCandidates)
    }

    override fun countryCandidates(): List<String> {
        return phoneUtils.apply { warmUp() }.countryCandidatesForEnteredPhoneNumber
    }
}
