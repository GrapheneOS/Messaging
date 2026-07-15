package com.android.messaging.domain.vcarddetail.usecase

import android.net.Uri
import androidx.core.net.toUri
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.domain.vcarddetail.model.AddVCardToContactsResult
import com.android.messaging.util.UriUtil
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class AddVCardToContactsImplTest {

    private val scratchUri = "content://scratch/vcard".toUri()
    private val testDispatcher = StandardTestDispatcher()

    private val useCase = AddVCardToContactsImpl(
        ioDispatcher = testDispatcher,
    )

    @Before
    fun setUp() {
        mockkStatic(UriUtil::class)
        mockkStatic(MediaScratchFileProvider::class)
        every { MediaScratchFileProvider.addUriToDisplayNameEntry(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_persistSucceedsWithDisplayName_registersNameAndReturnsPrepared() =
        runTest(testDispatcher) {
            every { UriUtil.persistContentToScratchSpace(any<Uri>()) } returns scratchUri

            val result = useCase(
                vCardUri = "content://vcard",
                displayName = "Ada Lovelace",
            )

            assertEquals(AddVCardToContactsResult.Prepared(scratchUri.toString()), result)
            verify { MediaScratchFileProvider.addUriToDisplayNameEntry(scratchUri, "Ada Lovelace") }
        }

    @Test
    fun invoke_persistSucceedsWithNullDisplayName_doesNotRegisterName() = runTest(testDispatcher) {
        every { UriUtil.persistContentToScratchSpace(any<Uri>()) } returns scratchUri

        val result = useCase(
            vCardUri = "content://vcard",
            displayName = null,
        )

        assertEquals(AddVCardToContactsResult.Prepared(scratchUri.toString()), result)
        verify(exactly = 0) { MediaScratchFileProvider.addUriToDisplayNameEntry(any(), any()) }
    }

    @Test
    fun invoke_persistSucceedsWithBlankDisplayName_doesNotRegisterName() = runTest(testDispatcher) {
        every { UriUtil.persistContentToScratchSpace(any<Uri>()) } returns scratchUri

        val result = useCase(
            vCardUri = "content://vcard",
            displayName = "   ",
        )

        assertEquals(AddVCardToContactsResult.Prepared(scratchUri.toString()), result)
        verify(exactly = 0) { MediaScratchFileProvider.addUriToDisplayNameEntry(any(), any()) }
    }

    @Test
    fun invoke_persistFails_returnsFailedAndDoesNotRegisterName() = runTest(testDispatcher) {
        every { UriUtil.persistContentToScratchSpace(any<Uri>()) } returns null

        val result = useCase(
            vCardUri = "content://vcard",
            displayName = "Ada Lovelace",
        )

        assertEquals(AddVCardToContactsResult.Failed, result)
        verify(exactly = 0) { MediaScratchFileProvider.addUriToDisplayNameEntry(any(), any()) }
    }
}
