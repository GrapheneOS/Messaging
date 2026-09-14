package com.android.messaging.domain.photoviewer.usecase

import android.net.Uri
import com.android.messaging.util.UriUtil
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class PreparePhotoViewerSendUriImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = PreparePhotoViewerSendUriImpl(ioDispatcher = testDispatcher)

    @Before
    fun setUp() {
        mockkStatic(UriUtil::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenUriIsContentUri_emitsOriginalUri() {
        runTest(context = testDispatcher) {
            val uri = Uri.parse("content://example/image/1")

            val result = useCase(uri = uri).single()

            assertEquals(uri, result)
            verify(exactly = 0) {
                UriUtil.persistContentToScratchSpace(any<Uri>())
            }
        }
    }

    @Test
    fun invoke_whenFileUriCopySucceeds_emitsScratchUri() {
        runTest(context = testDispatcher) {
            val fileUri = Uri.parse("file:///sdcard/Pictures/photo.jpg")
            val scratchUri = Uri.parse("content://example/scratch/1")
            every { UriUtil.persistContentToScratchSpace(fileUri) } returns scratchUri

            val result = useCase(uri = fileUri).single()

            assertEquals(scratchUri, result)
        }
    }

    @Test
    fun invoke_whenFileUriCopyFails_emitsNoUri() {
        runTest(context = testDispatcher) {
            val fileUri = Uri.parse("file:///sdcard/Pictures/photo.jpg")
            every { UriUtil.persistContentToScratchSpace(fileUri) } returns null

            val result = useCase(uri = fileUri).toList()

            assertTrue(result.isEmpty())
        }
    }
}
