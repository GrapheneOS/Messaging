package com.android.messaging.data.vcard.parser

import android.content.ContentResolver
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class VCardParserImplTest {

    private val contentResolver = mockk<ContentResolver>()
    private val context = mockk<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val parser = VCardParserImpl(
        context = context,
        ioDispatcher = testDispatcher,
    )

    init {
        every { context.contentResolver } returns contentResolver
    }

    @Test
    fun parse_blankUri_returnsEmpty() = runTest(testDispatcher) {
        val result = parser.parse("   ")

        assertTrue(result.isEmpty())
    }

    @Test
    fun parse_validSingleVCard_returnsEntryWithFields() = runTest(testDispatcher) {
        givenVCardContent(
            """
            BEGIN:VCARD
            VERSION:2.1
            N:Lovelace;Ada
            FN:Ada Lovelace
            TEL;CELL:+15550001
            END:VCARD
            """.trimIndent(),
        )

        val result = parser.parse("content://vcard")

        assertEquals(1, result.size)
        assertTrue(result.first().phoneList.first().number.contains("555"))
    }

    @Test
    fun parse_version30VCard_isParsedViaCascade() = runTest(testDispatcher) {
        givenVCardContent(
            """
            BEGIN:VCARD
            VERSION:3.0
            FN:Ada Lovelace
            TEL:+15550001
            END:VCARD
            """.trimIndent(),
        )

        val result = parser.parse("content://vcard")

        assertEquals(1, result.size)
        assertTrue(result.first().phoneList.first().number.contains("555"))
    }

    @Test
    fun parse_version40VCard_isParsedViaCascade() = runTest(testDispatcher) {
        givenVCardContent(
            """
            BEGIN:VCARD
            VERSION:4.0
            FN:Ada Lovelace
            TEL:+15550001
            END:VCARD
            """.trimIndent(),
        )

        val result = parser.parse("content://vcard")

        assertEquals(1, result.size)
        assertTrue(result.first().phoneList.first().number.contains("555"))
    }

    @Test
    fun parse_multipleVCards_returnsAllEntries() = runTest(testDispatcher) {
        givenVCardContent(
            """
            BEGIN:VCARD
            VERSION:2.1
            FN:Ada Lovelace
            END:VCARD
            BEGIN:VCARD
            VERSION:2.1
            FN:Alan Turing
            END:VCARD
            """.trimIndent(),
        )

        val result = parser.parse("content://vcard")

        assertEquals(2, result.size)
    }

    @Test
    fun parse_missingContent_returnsEmpty() = runTest(testDispatcher) {
        every { contentResolver.openInputStream(any()) } returns null

        val result = parser.parse("content://vcard")

        assertTrue(result.isEmpty())
    }

    @Test
    fun parse_unreadableContent_returnsEmpty() = runTest(testDispatcher) {
        every { contentResolver.openInputStream(any()) } throws IOException("boom")

        val result = parser.parse("content://vcard")

        assertTrue(result.isEmpty())
    }

    @Test
    fun parse_revokedUriGrant_returnsEmpty() = runTest(testDispatcher) {
        every { contentResolver.openInputStream(any()) } throws SecurityException("revoked")

        val result = parser.parse("content://vcard")

        assertTrue(result.isEmpty())
    }

    private fun givenVCardContent(content: String) {
        val bytes = content.replace("\n", "\r\n").toByteArray()
        every { contentResolver.openInputStream(any()) } answers { ByteArrayInputStream(bytes) }
    }
}
