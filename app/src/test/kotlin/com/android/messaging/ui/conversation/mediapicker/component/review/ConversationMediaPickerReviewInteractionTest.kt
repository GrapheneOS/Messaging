package com.android.messaging.ui.conversation.mediapicker.component.review

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerReviewInteractionTest :
    BaseConversationMediaPickerReviewTest() {

    @Test
    fun reviewActions_forwardCallbacks() {
        val imageAttachment = imageAttachment()

        setReviewContent(
            attachments = persistentListOf(imageAttachment),
        )

        composeTestRule
            .onNodeWithContentDescription(closeLabel())
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(addMoreLabel())
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(sendLabel())
            .performClick()
        clickReviewPageCenter()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onCloseClick.invoke()
            }
            verify(exactly = 1) {
                onAddMoreClick.invoke()
            }
            verify(exactly = 1) {
                onSendClick.invoke()
            }
            verify(exactly = 1) {
                onAttachmentPreviewClick.invoke(imageAttachment)
            }
        }
    }

    @Test
    fun captionEdit_forwardsCurrentAttachmentUriAndLatestText() {
        val capturedCaptions = mutableListOf<String>()

        setReviewContent(
            attachments = persistentListOf(
                imageAttachment(captionText = ""),
            ),
        )

        captionTextField()
            .performTextReplacement(text = "Draft caption")

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onCaptionChange.invoke(
                    IMAGE_CONTENT_URI,
                    capture(capturedCaptions),
                )
            }
            assertEquals(
                "Draft caption",
                capturedCaptions.last(),
            )
        }
    }

    @Test
    fun removingCurrentAttachment_waitsForExitAnimationBeforeCallback() {
        composeTestRule.mainClock.autoAdvance = false

        setReviewContent(
            attachments = threeAttachments(),
            initiallyReviewedContentUri = VIDEO_CONTENT_URI,
        )
        composeTestRule.mainClock.advanceTimeBy(milliseconds = REVIEW_CLOCK_SETTLE_MILLIS)

        composeTestRule
            .onNodeWithContentDescription(removeLabel())
            .performClick()
        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = PICKER_REVIEW_PAGE_REMOVE_ANIMATION_DURATION_MILLIS.toLong() - 1L,
        )

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onAttachmentRemove.invoke(any())
            }
            verify(exactly = 0) {
                onClearReview.invoke()
            }
        }

        composeTestRule.mainClock.advanceTimeBy(milliseconds = 1L)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentRemove.invoke(VIDEO_CONTENT_URI)
            }
            verify(exactly = 0) {
                onClearReview.invoke()
            }
        }
    }

    @Test
    fun pendingRemoval_disablesPreviewClick() {
        composeTestRule.mainClock.autoAdvance = false

        setReviewContent(
            attachments = persistentListOf(imageAttachment()),
        )
        composeTestRule.mainClock.advanceTimeBy(milliseconds = REVIEW_CLOCK_SETTLE_MILLIS)

        composeTestRule
            .onNodeWithContentDescription(removeLabel())
            .performClick()
        clickReviewPageCenter()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onAttachmentPreviewClick.invoke(any())
            }
        }
    }

    @Test
    fun removingOnlyAttachment_clearsReviewAfterRemoval() {
        composeTestRule.mainClock.autoAdvance = false

        setReviewContent(
            attachments = persistentListOf(imageAttachment()),
        )
        composeTestRule.mainClock.advanceTimeBy(milliseconds = REVIEW_CLOCK_SETTLE_MILLIS)

        composeTestRule
            .onNodeWithContentDescription(removeLabel())
            .performClick()
        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = PICKER_REVIEW_PAGE_REMOVE_ANIMATION_DURATION_MILLIS.toLong() +
                REVIEW_REMOVAL_CALLBACK_MARGIN_MILLIS,
        )

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentRemove.invoke(IMAGE_CONTENT_URI)
            }
            verify(exactly = 1) {
                onClearReview.invoke()
            }
        }
    }

    private fun closeLabel(): String {
        return targetContext.getString(R.string.conversation_media_picker_close_content_description)
    }

    private fun addMoreLabel(): String {
        return targetContext.getString(
            R.string.conversation_media_picker_add_more_content_description,
        )
    }

    private fun sendLabel(): String {
        return targetContext.getString(R.string.sendButtonContentDescription)
    }

    private fun removeLabel(): String {
        return targetContext.getString(
            R.string.conversation_media_picker_remove_attachment_content_description,
        )
    }

    private companion object {
        private const val REVIEW_CLOCK_SETTLE_MILLIS = 200L
        private const val REVIEW_REMOVAL_CALLBACK_MARGIN_MILLIS = 10L
    }
}
