package com.android.messaging.ui.conversation.mediapicker

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.viewinterop.AndroidView
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w900dp-h900dp")
internal class ConversationMediaPickerSheetScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val hostSize = mutableStateOf(value = PORTRAIT_SIZE)
    private var contentInstanceCount = 0
    private var attachedPhotoPickerSurface: View? = null

    @Test
    fun tallLayout_keepsThePhotoPickerInTheSheet() {
        setSheetScaffoldContent()

        composeTestRule
            .onNodeWithTag(PHOTO_PICKER_TAG)
            .assertTextEquals(false.toString())
        assertEquals(
            PORTRAIT_SIZE.width,
            composeTestRule.onNodeWithTag(CONTENT_TAG).getBoundsInRoot().width,
        )
    }

    @Test
    fun wideLayout_placesThePhotoPickerBesideTheContent() {
        hostSize.value = LANDSCAPE_SIZE
        setSheetScaffoldContent()

        val contentBounds = composeTestRule.onNodeWithTag(CONTENT_TAG).getBoundsInRoot()
        val photoPickerBounds = composeTestRule
            .onNodeWithTag(PHOTO_PICKER_TAG)
            .assertTextEquals(true.toString())
            .getBoundsInRoot()

        assertEquals(contentBounds.right, photoPickerBounds.left)
        assertEquals(
            (LANDSCAPE_SIZE.width * 0.45f).value,
            photoPickerBounds.width.value,
            DP_TOLERANCE,
        )
        assertEquals(LANDSCAPE_SIZE.height, photoPickerBounds.height)
    }

    @Test
    fun rotating_keepsTheContentState() {
        setSheetScaffoldContent()

        hostSize.value = LANDSCAPE_SIZE
        composeTestRule.onNodeWithTag(PHOTO_PICKER_TAG).assertTextEquals(true.toString())
        hostSize.value = PORTRAIT_SIZE
        composeTestRule.onNodeWithTag(PHOTO_PICKER_TAG).assertTextEquals(false.toString())

        assertEquals(1, contentInstanceCount)
    }

    @Test
    fun rotating_detachesTheOldPhotoPickerBeforeTheNewOneAttaches() {
        setSheetScaffoldContent()

        hostSize.value = LANDSCAPE_SIZE
        composeTestRule.onNodeWithTag(PHOTO_PICKER_TAG).assertTextEquals(true.toString())
        assertNotNull(attachedPhotoPickerSurface)

        hostSize.value = PORTRAIT_SIZE
        composeTestRule.onNodeWithTag(PHOTO_PICKER_TAG).assertTextEquals(false.toString())
        assertNotNull(attachedPhotoPickerSurface)
    }

    private fun setSheetScaffoldContent() {
        composeTestRule.setContent {
            AppTheme {
                Box(modifier = Modifier.size(size = hostSize.value)) {
                    ConversationMediaPickerSheetScaffold(
                        scaffoldState = rememberBottomSheetScaffoldState(),
                        photoPickerContent = { isBesideContent ->
                            AndroidView(factory = ::createPhotoPickerSurface)
                            Text(
                                modifier = Modifier
                                    .testTag(PHOTO_PICKER_TAG)
                                    .fillMaxSize(),
                                text = isBesideContent.toString(),
                            )
                        },
                    ) { _ ->
                        remember { ++contentInstanceCount }
                        Box(
                            modifier = Modifier
                                .testTag(CONTENT_TAG)
                                .fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    // Like the embedded photo picker, which shares one surface token between its pickers.
    private fun createPhotoPickerSurface(context: Context): View {
        return View(context).apply {
            addOnAttachStateChangeListener(
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) {
                        attachedPhotoPickerSurface = view
                    }

                    override fun onViewDetachedFromWindow(view: View) {
                        attachedPhotoPickerSurface = null
                    }
                },
            )
        }
    }

    private companion object {
        private const val CONTENT_TAG = "content"
        private const val DP_TOLERANCE = 0.5f
        private const val PHOTO_PICKER_TAG = "photo_picker"
        private val LANDSCAPE_SIZE = DpSize(width = 800.dp, height = 400.dp)
        private val PORTRAIT_SIZE = DpSize(width = 400.dp, height = 800.dp)
    }
}
