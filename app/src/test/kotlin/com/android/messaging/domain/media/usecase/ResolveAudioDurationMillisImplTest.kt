package com.android.messaging.domain.media.usecase

import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.util.UriUtil
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ResolveAudioDurationMillisImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val resolveAudioDurationMillis = ResolveAudioDurationMillisImpl(
        ioDispatcher = mainDispatcherRule.testDispatcher,
    )

    @Before
    fun setUp() {
        mockkStatic(UriUtil::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun resolveAudioDurationMillis_readsTheClipLengthFromMetadata() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            every { UriUtil.getMediaDurationMs(any()) } returns DURATION_MILLIS

            assertEquals(DURATION_MILLIS.toLong(), resolveAudioDurationMillis(AUDIO_CONTENT_URI))
        }
    }

    @Test
    fun resolveAudioDurationMillis_reusesTheResolvedDuration() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            every { UriUtil.getMediaDurationMs(any()) } returns DURATION_MILLIS

            assertEquals(DURATION_MILLIS.toLong(), resolveAudioDurationMillis(AUDIO_CONTENT_URI))
            assertEquals(DURATION_MILLIS.toLong(), resolveAudioDurationMillis(AUDIO_CONTENT_URI))

            verify(exactly = 1) { UriUtil.getMediaDurationMs(any()) }
        }
    }

    @Test
    fun resolveAudioDurationMillis_unreadableMediaReportsUnknownDurationAndIsNotCached() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            every { UriUtil.getMediaDurationMs(any()) } throws IllegalStateException("unreadable")

            assertEquals(0L, resolveAudioDurationMillis(AUDIO_CONTENT_URI))

            every { UriUtil.getMediaDurationMs(any()) } returns DURATION_MILLIS

            assertEquals(DURATION_MILLIS.toLong(), resolveAudioDurationMillis(AUDIO_CONTENT_URI))
        }
    }

    @Test
    fun resolveAudioDurationMillis_negativeMetadataReportsUnknownDuration() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            every { UriUtil.getMediaDurationMs(any()) } returns -1

            assertEquals(0L, resolveAudioDurationMillis(AUDIO_CONTENT_URI))
        }
    }

    @Test
    fun resolveAudioDurationMillis_blankUriSkipsTheMetadataRead() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            assertEquals(0L, resolveAudioDurationMillis(contentUri = ""))

            verify(exactly = 0) { UriUtil.getMediaDurationMs(any()) }
        }
    }

    private companion object {
        private const val AUDIO_CONTENT_URI = "content://mms/part/audio"
        private const val DURATION_MILLIS = 18_000
    }
}
