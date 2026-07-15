package com.android.messaging.ui.conversation.messages.ui.attachment.rendering

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.ui.attachment.buildConversationAttachmentSections
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageAttachmentsRenderingTest :
    BaseConversationMessageAttachmentRenderingTest() {

    @Test
    fun emptySections_renderNoAttachmentContainer() {
        setMessageAttachmentsContent(attachmentSections = emptySections())

        composeTestRule
            .onAllNodesWithTag(testTag = MESSAGE_ATTACHMENTS_TAG)
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun builtMixedSections_renderGalleryAndTrailingRows() {
        val sections = buildConversationAttachmentSections(
            attachments = persistentListOf(
                imageAttachment(),
                audioAttachment(),
                fileAttachment(),
                vCardMediaAttachment(),
                videoAttachment(),
            ),
        )

        setMessageAttachmentsContent(attachmentSections = sections)

        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.audio_attachment_content_description))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(FILE_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(VCARD_TITLE)
            .assertIsDisplayed()
    }

    @Test
    fun singleImageGallery_clickForwardsContentOpen() {
        setMessageAttachmentsContent(
            attachmentSections = gallerySections(imageAttachment()),
        )

        clickMessageAttachmentsAt()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(IMAGE_CONTENT_TYPE, IMAGE_CONTENT_URI, "")
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun standaloneVideo_clickForwardsContentOpen() {
        setMessageAttachmentsContent(
            attachmentSections = trailingSections(
                standaloneVisualItem(attachment = videoAttachment()),
            ),
        )

        clickMessageAttachmentsAt()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(VIDEO_CONTENT_TYPE, VIDEO_CONTENT_URI, "")
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun inlineFile_clickForwardsContentOpen() {
        setMessageAttachmentsContent(
            attachmentSections = trailingSections(
                inlineItem(attachment = fileInlineAttachment()),
            ),
        )

        clickMessageAttachmentsAt()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(FILE_CONTENT_TYPE, FILE_CONTENT_URI, "")
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun inlineFile_longClickForwardsMessageLongClickOnly() {
        setMessageAttachmentsContent(
            attachmentSections = trailingSections(
                inlineItem(attachment = fileInlineAttachment()),
            ),
        )

        longClickMessageAttachmentsAt()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageLongClick.invoke()
            }
            verify(exactly = 0) {
                onAttachmentClick.invoke(any(), any(), any())
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun inlineFile_fallbackTitleResourceRendersLocalizedLabel() {
        setMessageAttachmentsContent(
            attachmentSections = trailingSections(
                inlineItem(
                    attachment = fileInlineAttachment(
                        titleText = null,
                        titleTextResId = R.string.notification_file,
                    ),
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.notification_file))
            .assertIsDisplayed()
    }
}
