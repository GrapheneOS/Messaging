package com.android.messaging.ui.conversation.mediapicker.component.review

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.conversationMediaReviewPreviewTestTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w500dp-h400dp")
internal class ConversationMediaPickerReviewSafeAreaTest : BaseConversationMediaPickerReviewTest() {

    @Test
    fun startSafeInset_previousPageDrawsUnderTheInset() {
        setReviewContentWithStartSafeInset()

        val previousPageBounds = reviewPageBounds(contentUri = FIRST_IMAGE_CONTENT_URI)

        assertTrue(
            "Previous page is clipped at the safe inset: $previousPageBounds",
            previousPageBounds.left < START_SAFE_INSET,
        )
    }

    @Test
    fun startSafeInset_keepsCurrentPageAndBarsInTheSafeArea() {
        setReviewContentWithStartSafeInset()

        val currentPageBounds = reviewPageBounds(contentUri = VIDEO_CONTENT_URI)
        val closeButtonBounds = composeTestRule
            .onNodeWithContentDescription(label = closeLabel())
            .getBoundsInRoot()
        val captionFieldBounds = captionTextField().getBoundsInRoot()

        assertTrue(currentPageBounds.left >= START_SAFE_INSET)
        assertTrue(closeButtonBounds.left >= START_SAFE_INSET)
        assertTrue(captionFieldBounds.left >= START_SAFE_INSET)
    }

    @Test
    fun startSafeInset_centersCurrentPageInTheSafeArea() {
        setReviewContentWithStartSafeInset()

        val rootBounds = composeTestRule.onRoot().getBoundsInRoot()
        val currentPageBounds = reviewPageBounds(contentUri = VIDEO_CONTENT_URI)
        val safeAreaCenter = (START_SAFE_INSET + rootBounds.right) / 2
        val currentPageCenter = (currentPageBounds.left + currentPageBounds.right) / 2

        assertEquals(safeAreaCenter.value, currentPageCenter.value, DP_TOLERANCE)
    }

    @Test
    fun rtlStartSafeInset_previousPageDrawsUnderTheInsetOnTheRight() {
        setReviewContentWithStartSafeInset(layoutDirection = LayoutDirection.Rtl)

        val safeAreaRight = composeTestRule.onRoot().getBoundsInRoot().right - START_SAFE_INSET
        val previousPageBounds = reviewPageBounds(contentUri = FIRST_IMAGE_CONTENT_URI)
        val currentPageBounds = reviewPageBounds(contentUri = VIDEO_CONTENT_URI)

        assertTrue(
            "Previous page is clipped at the safe inset: $previousPageBounds",
            previousPageBounds.right > safeAreaRight,
        )
        assertTrue(currentPageBounds.right <= safeAreaRight)
    }

    private fun setReviewContentWithStartSafeInset(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        setReviewContent(
            attachments = threeAttachments(),
            initiallyReviewedContentUri = VIDEO_CONTENT_URI,
            contentPadding = PaddingValues(start = START_SAFE_INSET),
            layoutDirection = layoutDirection,
        )
        composeTestRule.waitForIdle()
    }

    private fun reviewPageBounds(contentUri: String): DpRect {
        return composeTestRule
            .onNodeWithTag(testTag = conversationMediaReviewPreviewTestTag(contentUri = contentUri))
            .getBoundsInRoot()
    }

    private fun closeLabel(): String {
        return targetContext.getString(R.string.conversation_media_picker_close_content_description)
    }

    private companion object {
        private val START_SAFE_INSET = 48.dp
        private const val DP_TOLERANCE = 0.5f
    }
}
