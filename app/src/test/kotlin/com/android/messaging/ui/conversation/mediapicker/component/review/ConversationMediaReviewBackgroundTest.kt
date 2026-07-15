package com.android.messaging.ui.conversation.mediapicker.component.review

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.android.common.test.helpers.targetContext
import com.android.messaging.ui.common.components.mediapreview.MediaPreviewBackground
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.toMediaPreviewItem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaReviewBackgroundTest : BaseConversationMediaPickerReviewTest() {

    @Test
    fun emptyAttachments_rendersFallbackBackground() {
        setBackgroundContent(
            attachments = persistentListOf(),
        )

        composeTestRule
            .onNodeWithTag(BACKGROUND_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun loadableImage_rendersBitmapBackground() {
        registerMagentaImageUri()

        setBackgroundContent(
            attachments = persistentListOf(
                imageAttachment(
                    contentUri = IMAGE_CONTENT_URI,
                ),
            ),
        )

        composeTestRule.waitUntil(timeoutMillis = BITMAP_LOAD_TIMEOUT_MILLIS) {
            capturedBackgroundHasMagentaPixel()
        }
    }

    private fun setBackgroundContent(
        attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    ) {
        composeTestRule.setContent {
            ReviewContent {
                val pagerState = rememberPagerState(
                    pageCount = { maxOf(attachments.size, 1) },
                )

                MediaPreviewBackground(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(tag = BACKGROUND_TAG),
                    pagerState = pagerState,
                    items = attachments.map { attachment ->
                        attachment.toMediaPreviewItem()
                    }.toImmutableList(),
                )
            }
        }
    }

    private fun registerMagentaImageUri() {
        val imageBytes = createMagentaImageBytes()
        Shadows
            .shadowOf(targetContext.contentResolver)
            .registerInputStreamSupplier(Uri.parse(IMAGE_CONTENT_URI)) {
                ByteArrayInputStream(imageBytes)
            }
    }

    private fun createMagentaImageBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(
            24,
            24,
            Bitmap.Config.ARGB_8888,
        )
        bitmap.eraseColor(AndroidColor.MAGENTA)

        val outputStream = ByteArrayOutputStream()
        assertTrue(
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                outputStream,
            ),
        )
        return outputStream.toByteArray()
    }

    private fun capturedBackgroundHasMagentaPixel(): Boolean {
        val bounds = composeTestRule
            .onNodeWithTag(BACKGROUND_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val bitmap = composeTestRule.runOnIdle { captureActivityBitmap() }
        val centerPixel = bitmap.getPixel(
            (bounds.left + bounds.width / 2f).roundToInt(),
            (bounds.top + bounds.height / 2f).roundToInt(),
        )

        return AndroidColor.red(centerPixel) > MAGENTA_MINIMUM_COLOR_COMPONENT &&
            AndroidColor.blue(centerPixel) > MAGENTA_MINIMUM_COLOR_COMPONENT &&
            AndroidColor.green(centerPixel) < MAGENTA_MAXIMUM_GREEN_COMPONENT
    }

    private fun captureActivityBitmap(): Bitmap {
        val view = composeTestRule.activity.window.decorView.rootView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private companion object {
        const val BACKGROUND_TAG = "conversation_media_review_background"
        const val BITMAP_LOAD_TIMEOUT_MILLIS = 5_000L
        const val MAGENTA_MAXIMUM_GREEN_COMPONENT = 80
        const val MAGENTA_MINIMUM_COLOR_COMPONENT = 80
    }
}
