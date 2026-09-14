package com.android.messaging.data.sms

import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import com.android.messaging.util.BugleGservices
import com.android.messaging.util.BugleGservicesKeys
import com.android.messaging.util.LogUtil
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.inject.Inject

internal interface IncomingSmsParser {
    fun parse(intent: Intent): Array<SmsMessage>?
}

internal class IncomingSmsParserImpl @Inject constructor() : IncomingSmsParser {

    @Suppress("TooGenericExceptionCaught")
    override fun parse(intent: Intent): Array<SmsMessage>? {
        val messages = Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return null
        }

        // Sometimes, SmsMessage.mWrappedSmsMessage is null causing NPE when we access
        // the methods on it although the SmsMessage itself is not null. So do this check
        // before we do anything on the parsed SmsMessages.
        return try {
            when {
                isIgnored(messages.first().displayMessageBody) -> null
                else -> messages
            }
        } catch (exception: NullPointerException) {
            LogUtil.e(
                LogUtil.BUGLE_TAG,
                "shouldIgnoreMessage: NPE inside SmsMessage",
                exception,
            )
            null
        }
    }

    private fun isIgnored(messageBody: String?): Boolean {
        if (messageBody == null) {
            return false
        }

        return ignorePatterns().any { pattern ->
            pattern.matcher(messageBody).matches()
        }
    }

    private fun ignorePatterns(): List<Pattern> {
        val smsIgnoreRegex = BugleGservices.get().getString(
            BugleGservicesKeys.SMS_IGNORE_MESSAGE_REGEX,
            BugleGservicesKeys.SMS_IGNORE_MESSAGE_REGEX_DEFAULT,
        ) ?: return emptyList()

        val patterns = mutableListOf<Pattern>()
        for (expression in smsIgnoreRegex.split("\n")) {
            try {
                patterns.add(Pattern.compile(expression))
            } catch (exception: PatternSyntaxException) {
                LogUtil.e(
                    LogUtil.BUGLE_TAG,
                    "compileIgnoreSmsPatterns: Skipping bad expression: $expression",
                    exception,
                )
            }
        }

        return patterns
    }
}
