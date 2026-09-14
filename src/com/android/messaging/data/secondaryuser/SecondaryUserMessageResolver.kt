package com.android.messaging.data.secondaryuser

import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import com.android.messaging.data.secondaryuser.model.SecondaryUserMessageInfo
import javax.inject.Inject

internal interface SecondaryUserMessageResolver {
    fun resolve(address: String?, body: String?): SecondaryUserMessageInfo?
}

internal class SecondaryUserMessageResolverImpl @Inject constructor(
    private val contactNameLookup: SmsContactNameLookup,
    private val phoneNumberFormatter: PhoneNumberFormatter,
) : SecondaryUserMessageResolver {

    override fun resolve(
        address: String?,
        body: String?,
    ): SecondaryUserMessageInfo? {
        if (body.isNullOrEmpty()) {
            return null
        }

        return resolveSenderDisplayName(address)?.let { sender ->
            SecondaryUserMessageInfo(
                sender = sender,
                body = body,
            )
        }
    }

    private fun resolveSenderDisplayName(address: String?): String? {
        if (address.isNullOrEmpty()) {
            return null
        }

        return contactNameLookup.lookup(address)
            ?.takeIf(String::isNotEmpty)
            ?: phoneNumberFormatter.formatForDisplay(address)
    }
}
