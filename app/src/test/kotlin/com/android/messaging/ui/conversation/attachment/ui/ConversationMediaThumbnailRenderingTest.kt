package com.android.messaging.ui.conversation.attachment.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.common.test.helpers.targetContext
import com.android.messaging.ui.core.AppTheme
import java.io.ByteArrayInputStream
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaThumbnailRenderingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun invalidImageUriWithBitmapLoader_keepsPlaceholderVisible() {
        setThumbnailContent(
            contentType = IMAGE_CONTENT_TYPE,
            size = IntSize(width = THUMBNAIL_SIZE_PX, height = THUMBNAIL_SIZE_PX),
            backgroundColor = Color.Red,
            useBitmapLoader = true,
        )

        assertThumbnailCenterColor(expectedColor = Color.Red)
    }

    @Test
    fun nonImageContent_usesBitmapPathPlaceholder() {
        setThumbnailContent(
            contentType = FILE_CONTENT_TYPE,
            size = IntSize(width = THUMBNAIL_SIZE_PX, height = THUMBNAIL_SIZE_PX),
            backgroundColor = Color.Blue,
            useBitmapLoader = false,
        )

        assertThumbnailCenterColor(expectedColor = Color.Blue)
    }

    @Test
    fun zeroRequestedSize_sanitizesAndRendersPlaceholder() {
        setThumbnailContent(
            contentType = IMAGE_CONTENT_TYPE,
            size = IntSize.Zero,
            backgroundColor = Color.Green,
            useBitmapLoader = true,
        )

        assertThumbnailCenterColor(expectedColor = Color.Green)
    }

    private fun setThumbnailContent(
        contentType: String,
        size: IntSize,
        backgroundColor: Color,
        useBitmapLoader: Boolean,
    ) {
        Shadows
            .shadowOf(targetContext.contentResolver)
            .registerInputStreamSupplier(Uri.parse(INVALID_CONTENT_URI)) {
                ByteArrayInputStream(byteArrayOf())
            }

        composeTestRule.setContent {
            AppTheme {
                ConversationMediaThumbnail(
                    modifier = Modifier
                        .size(size = THUMBNAIL_SIZE_DP.dp)
                        .testTag(tag = THUMBNAIL_TAG),
                    contentUri = INVALID_CONTENT_URI,
                    contentType = contentType,
                    size = size,
                    contentScale = ContentScale.Crop,
                    backgroundColor = backgroundColor,
                    useBitmapLoader = useBitmapLoader,
                )
            }
        }
    }

    private fun assertThumbnailCenterColor(expectedColor: Color) {
        composeTestRule.waitForIdle()

        val thumbnailNode = composeTestRule
            .onNodeWithTag(testTag = THUMBNAIL_TAG)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(expectedWidth = THUMBNAIL_SIZE_DP.dp)
            .assertHeightIsEqualTo(expectedHeight = THUMBNAIL_SIZE_DP.dp)
        val bounds = thumbnailNode
            .fetchSemanticsNode()
            .boundsInRoot

        val bitmap = composeTestRule.runOnIdle {
            captureActivityBitmap()
        }
        val centerPixel = bitmap.getPixel(
            (bounds.left + bounds.width / 2f).roundToInt(),
            (bounds.top + bounds.height / 2f).roundToInt(),
        )
        val redMatches =
            abs(AndroidColor.red(centerPixel) / COLOR_COMPONENT_MAX - expectedColor.red) <=
                COLOR_DELTA
        val greenMatches =
            abs(AndroidColor.green(centerPixel) / COLOR_COMPONENT_MAX - expectedColor.green) <=
                COLOR_DELTA
        val blueMatches =
            abs(AndroidColor.blue(centerPixel) / COLOR_COMPONENT_MAX - expectedColor.blue) <=
                COLOR_DELTA

        assertTrue(redMatches && greenMatches && blueMatches)
    }

    private fun captureActivityBitmap(): Bitmap {
        val view = composeTestRule.activity.window.decorView.rootView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        return bitmap
    }

    private companion object {
        private const val COLOR_COMPONENT_MAX = 255f
        private const val COLOR_DELTA = 0.08f
        private const val FILE_CONTENT_TYPE = "application/pdf"
        private const val IMAGE_CONTENT_TYPE = "image/jpeg"
        private const val INVALID_CONTENT_URI = "content://com.android.messaging.invalid/missing"
        private const val THUMBNAIL_SIZE_DP = 48
        private const val THUMBNAIL_SIZE_PX = 96
        private const val THUMBNAIL_TAG = "conversation-media-thumbnail-under-test"
    }
}
