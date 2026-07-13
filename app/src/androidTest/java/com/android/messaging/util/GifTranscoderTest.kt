package com.android.messaging.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class GifTranscoderTest {

    private lateinit var context: Context
    private lateinit var input: File
    private lateinit var output: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        input = File(context.cacheDir, "gif_transcoder_in.gif")
        output = File(context.cacheDir, "gif_transcoder_out.gif")

        val testAssets = InstrumentationRegistry.getInstrumentation().context.assets
        testAssets.open(ASSET_PATH).use { asset ->
            input.outputStream().use { asset.copyTo(it) }
        }
    }

    @After
    fun tearDown() {
        input.delete()
        output.delete()
    }

    @Test
    fun transcode_preservesColorChannelsAbove0x7f() {
        val success = GifTranscoder.transcode(context, input.absolutePath, output.absolutePath)
        assertTrue("transcode() should succeed", success)
        assertTrue("output GIF should be written", output.length() > 0)

        val bitmap = requireNotNull(BitmapFactory.decodeFile(output.absolutePath)) {
            "transcoded GIF should decode to a bitmap"
        }

        assertTrue(
            "output should be 2x1, was ${bitmap.width}x${bitmap.height}",
            bitmap.width == 2 && bitmap.height == 1,
        )

        val left = bitmap.getPixel(0, 0)
        val right = bitmap.getPixel(1, 0)

        assertTrue(
            "left pixel should stay grey, was #${Integer.toHexString(left)}",
            channelsNear(left, expected = 200),
        )
        assertTrue(
            "right pixel should stay white, was #${Integer.toHexString(right)}",
            channelsNear(right, expected = 255),
        )
    }

    private fun channelsNear(color: Int, expected: Int, tolerance: Int = 24): Boolean =
        abs(Color.red(color) - expected) <= tolerance &&
            abs(Color.green(color) - expected) <= tolerance &&
            abs(Color.blue(color) - expected) <= tolerance

    private companion object {
        private const val ASSET_PATH = "gif_transcoder/high_color_channels_4x2.gif"
    }
}
