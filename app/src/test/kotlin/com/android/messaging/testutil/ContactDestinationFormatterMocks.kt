package com.android.messaging.testutil

import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import io.mockk.every
import io.mockk.mockk
import java.util.Locale

internal fun mockContactDestinationFormatter(): ContactDestinationFormatter {
    return mockk<ContactDestinationFormatter> {
        every { countryCandidates() } returns emptyList()
        every {
            canonicalize(
                value = any(),
                countryCandidates = any(),
            )
        } answers {
            firstArg<String>()
                .trim()
                .filterNot { character ->
                    character.isWhitespace() || character in PHONE_SEPARATOR_CHARS
                }
        }
        every { canonicalize(value = any()) } answers {
            firstArg<String>().trim().lowercase(Locale.ROOT)
        }
    }
}

private val PHONE_SEPARATOR_CHARS = setOf('-', '(', ')', '.', '/')
