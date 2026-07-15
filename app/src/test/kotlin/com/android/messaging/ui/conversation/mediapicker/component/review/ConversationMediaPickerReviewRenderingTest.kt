package com.android.messaging.ui.conversation.mediapicker.component.review

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.testutil.performDisabledTouchClick
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerReviewRenderingTest :
    BaseConversationMediaPickerReviewTest() {

    @Test
    fun emptyAttachments_rendersNoReviewControls() {
        setReviewContent(
            attachments = persistentListOf(),
        )

        composeTestRule
            .onAllNodesWithContentDescription(closeLabel())
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onAllNodesWithContentDescription(addMoreLabel())
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onAllNodesWithContentDescription(sendLabel())
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onAllNodesWithText(captionHint())
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun singleImage_rendersTitleCaptionAndActions() {
        setReviewContent(
            attachments = persistentListOf(imageAttachment()),
            conversationTitle = CONVERSATION_TITLE,
        )

        composeTestRule
            .onNodeWithText(CONVERSATION_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(IMAGE_CAPTION)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(closeLabel())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(addMoreLabel())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(sendLabel())
            .assertIsDisplayed()
    }

    @Test
    fun nullConversationTitle_keepsActionsVisible() {
        setReviewContent(
            attachments = persistentListOf(imageAttachment(captionText = "")),
            conversationTitle = null,
        )

        composeTestRule
            .onNodeWithContentDescription(closeLabel())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(addMoreLabel())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(captionHint())
            .assertIsDisplayed()
    }

    @Test
    fun videoAttachment_forwardsPreviewClickWithVideoModel() {
        val videoAttachment = videoAttachment()

        setReviewContent(
            attachments = persistentListOf(videoAttachment),
        )

        clickReviewPageCenter(contentUri = videoAttachment.contentUri)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentPreviewClick.invoke(videoAttachment)
            }
        }
    }

    @Test
    fun disabledSendAction_ignoresTouchClick() {
        setReviewContent(
            attachments = persistentListOf(imageAttachment()),
            isSendActionEnabled = false,
        )

        composeTestRule
            .onNodeWithContentDescription(
                label = sendLabel(),
                useUnmergedTree = true,
            )
            .performDisabledTouchClick()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onSendClick.invoke()
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

    private fun captionHint(): String {
        return targetContext.getString(R.string.conversation_media_picker_caption_hint)
    }
}
