package com.android.messaging.ui.conversation.messages.ui.attachment.rendering

import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationVisualAttachmentsInteractionTest :
    BaseConversationMessageAttachmentRenderingTest() {

    @Test
    fun twoImageGallery_clicksEachCellForwardMatchingContent() {
        setMessageAttachmentsContent(
            attachmentSections = gallerySections(
                imageAttachment(),
                imageAttachment(
                    key = SECOND_IMAGE_KEY,
                    contentUri = SECOND_IMAGE_CONTENT_URI,
                ),
            ),
        )

        clickMessageAttachmentsAt(xFraction = 0.25f)
        clickMessageAttachmentsAt(xFraction = 0.75f)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(IMAGE_CONTENT_TYPE, IMAGE_CONTENT_URI)
            }
            verify(exactly = 1) {
                onAttachmentClick.invoke(IMAGE_CONTENT_TYPE, SECOND_IMAGE_CONTENT_URI)
            }
        }
    }

    @Test
    fun threeImageGallery_clicksOddTrailingCellAndIgnoresFiller() {
        setMessageAttachmentsContent(
            attachmentSections = gallerySections(
                imageAttachment(),
                imageAttachment(
                    key = SECOND_IMAGE_KEY,
                    contentUri = SECOND_IMAGE_CONTENT_URI,
                ),
                imageAttachment(
                    key = THIRD_IMAGE_KEY,
                    contentUri = THIRD_IMAGE_CONTENT_URI,
                ),
            ),
            hasTextAboveVisualAttachments = true,
            hasTextBelowVisualAttachments = true,
        )

        clickMessageAttachmentsAt(xFraction = 0.25f, yFraction = 0.25f)
        clickMessageAttachmentsAt(xFraction = 0.75f, yFraction = 0.25f)
        clickMessageAttachmentsAt(xFraction = 0.25f, yFraction = 0.75f)
        clickMessageAttachmentsAt(xFraction = 0.75f, yFraction = 0.75f)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(IMAGE_CONTENT_TYPE, IMAGE_CONTENT_URI)
            }
            verify(exactly = 1) {
                onAttachmentClick.invoke(IMAGE_CONTENT_TYPE, SECOND_IMAGE_CONTENT_URI)
            }
            verify(exactly = 1) {
                onAttachmentClick.invoke(IMAGE_CONTENT_TYPE, THIRD_IMAGE_CONTENT_URI)
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun youTubePreview_clickForwardsExternalUri() {
        setMessageAttachmentsContent(
            attachmentSections = gallerySections(youTubeAttachment()),
        )

        clickMessageAttachmentsAt()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onExternalUriClick.invoke(YOUTUBE_SOURCE_URI)
            }
            verify(exactly = 0) {
                onAttachmentClick.invoke(any(), any())
            }
        }
    }

    @Test
    fun standaloneUnsupportedAttachmentWithContentUri_opensContent() {
        setStandaloneVisualAttachmentContent(
            attachment = unsupportedAttachment(),
            hasTextAboveVisualAttachments = true,
            hasTextBelowVisualAttachments = true,
        )

        clickStandaloneVisual()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(UNSUPPORTED_CONTENT_TYPE, UNSUPPORTED_CONTENT_URI)
            }
        }
    }

    @Test
    fun imageAttachmentWithMissingDimensions_usesDefaultSizingAndOpensContent() {
        setMessageAttachmentsContent(
            attachmentSections = gallerySections(
                imageAttachment(
                    width = 0,
                    height = 0,
                ),
            ),
        )

        clickMessageAttachmentsAt()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(IMAGE_CONTENT_TYPE, IMAGE_CONTENT_URI)
            }
        }
    }

    @Test
    fun videoAttachmentWithMissingDimensions_usesDefaultSizingAndOpensContent() {
        setStandaloneVisualAttachmentContent(
            attachment = videoAttachment(
                width = 0,
                height = 0,
            ),
        )

        clickStandaloneVisual()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(VIDEO_CONTENT_TYPE, VIDEO_CONTENT_URI)
            }
        }
    }

    @Test
    fun standaloneVisual_longClickForwardsMessageLongClickOnly() {
        setStandaloneVisualAttachmentContent(attachment = videoAttachment())

        longClickStandaloneVisual()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageLongClick.invoke()
            }
            verify(exactly = 0) {
                onAttachmentClick.invoke(any(), any())
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }
}
