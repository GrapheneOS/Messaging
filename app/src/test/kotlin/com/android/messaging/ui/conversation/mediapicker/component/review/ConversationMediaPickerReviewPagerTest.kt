package com.android.messaging.ui.conversation.mediapicker.component.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerReviewPagerTest : BaseConversationMediaPickerReviewTest() {

    @Test
    fun multipleAttachments_startsAtRequestedContentUri() {
        setReviewContent(
            attachments = threeAttachments(),
            initiallyReviewedContentUri = VIDEO_CONTENT_URI,
        )

        composeTestRule
            .onNodeWithText(VIDEO_CAPTION)
            .assertIsDisplayed()
    }

    @Test
    fun initialReview_usesPhotoPickerSourceMapping() {
        setReviewContent(
            attachments = threeAttachments(),
            initiallyReviewedContentUri = PHOTO_PICKER_SOURCE_URI,
            photoPickerSourceContentUriByAttachmentContentUri = persistentMapOf(
                VIDEO_CONTENT_URI to PHOTO_PICKER_SOURCE_URI,
            ),
        )

        composeTestRule
            .onNodeWithText(VIDEO_CAPTION)
            .assertIsDisplayed()
    }

    @Test
    fun initialReview_missingUri_fallsBackToLastAttachment() {
        setReviewContent(
            attachments = threeAttachments(),
            initiallyReviewedContentUri = "content://missing/review/uri",
        )

        composeTestRule
            .onNodeWithText(LAST_IMAGE_CAPTION)
            .assertIsDisplayed()
    }

    @Test
    fun reviewRequestSequenceChange_scrollsToRequestedAttachment() {
        var initiallyReviewedContentUri by mutableStateOf(FIRST_IMAGE_CONTENT_URI)
        var reviewRequestSequence by mutableIntStateOf(1)

        setReviewContent(
            attachments = { threeAttachments() },
            initiallyReviewedContentUri = { initiallyReviewedContentUri },
            reviewRequestSequence = { reviewRequestSequence },
        )

        composeTestRule
            .onNodeWithText(FIRST_IMAGE_CAPTION)
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            initiallyReviewedContentUri = VIDEO_CONTENT_URI
            reviewRequestSequence += 1
        }

        awaitText(VIDEO_CAPTION)
    }

    @Test
    fun reviewRequestSequenceChange_usesPhotoPickerSourceMapping() {
        var initiallyReviewedContentUri by mutableStateOf(FIRST_IMAGE_CONTENT_URI)
        var reviewRequestSequence by mutableIntStateOf(1)

        setReviewContent(
            attachments = { threeAttachments() },
            initiallyReviewedContentUri = { initiallyReviewedContentUri },
            reviewRequestSequence = { reviewRequestSequence },
            photoPickerSourceContentUriByAttachmentContentUri = {
                persistentMapOf(
                    VIDEO_CONTENT_URI to PHOTO_PICKER_SOURCE_URI,
                )
            },
        )

        composeTestRule
            .onNodeWithText(FIRST_IMAGE_CAPTION)
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            initiallyReviewedContentUri = PHOTO_PICKER_SOURCE_URI
            reviewRequestSequence += 1
        }

        awaitText(VIDEO_CAPTION)
    }

    @Test
    fun initialUriChangeWithoutSequenceChange_keepsCurrentAttachment() {
        var initiallyReviewedContentUri by mutableStateOf(FIRST_IMAGE_CONTENT_URI)

        setReviewContent(
            attachments = { threeAttachments() },
            initiallyReviewedContentUri = { initiallyReviewedContentUri },
            reviewRequestSequence = { 1 },
        )

        composeTestRule
            .onNodeWithText(FIRST_IMAGE_CAPTION)
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            initiallyReviewedContentUri = LAST_IMAGE_CONTENT_URI
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(FIRST_IMAGE_CAPTION)
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(LAST_IMAGE_CAPTION)
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun attachmentsShrink_clampsCurrentPageToRemainingAttachment() {
        val firstAttachment = imageAttachment(
            key = FIRST_IMAGE_KEY,
            contentUri = FIRST_IMAGE_CONTENT_URI,
            captionText = FIRST_IMAGE_CAPTION,
        )
        var attachments by mutableStateOf(threeAttachments())

        setReviewContent(
            attachments = { attachments },
            initiallyReviewedContentUri = { LAST_IMAGE_CONTENT_URI },
        )

        composeTestRule
            .onNodeWithText(LAST_IMAGE_CAPTION)
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            attachments = persistentListOf(firstAttachment)
        }

        awaitText(FIRST_IMAGE_CAPTION)
    }
}
