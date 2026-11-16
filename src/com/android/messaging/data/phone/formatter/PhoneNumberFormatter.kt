package com.android.messaging.data.phone.formatter

import com.android.messaging.util.PhoneUtils
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.MatchType
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import javax.inject.Inject

internal interface PhoneNumberFormatter {
    fun formatForDisplay(number: String): String
    fun formatForDisplayUsingSimCountry(number: String): String
    fun formatNormalizedUsingSimCountry(number: String): String
    fun getCanonicalForEnteredNumber(number: String): String
    fun getCanonicalForEnteredNumber(number: String, countryCandidates: List<String>): String
    fun countryCandidates(): List<String>
    fun isSameNumber(first: String, second: String, country: String): Boolean
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

    override fun isSameNumber(first: String, second: String, country: String): Boolean {
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        val firstNumber = phoneNumberUtil.parseOrNull(number = first, country = country)
        val secondNumber = phoneNumberUtil.parseOrNull(number = second, country = country)

        return firstNumber != null &&
            secondNumber != null &&
            phoneNumberUtil.isNumberMatch(firstNumber, secondNumber) == MatchType.EXACT_MATCH
    }

    private fun PhoneNumberUtil.parseOrNull(number: String, country: String): PhoneNumber? {
        return try {
            parse(number, country)
        } catch (_: NumberParseException) {
            null
        }
    }
}
