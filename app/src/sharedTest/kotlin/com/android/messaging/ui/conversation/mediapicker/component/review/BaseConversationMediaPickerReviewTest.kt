package com.android.messaging.ui.conversation.mediapicker.component.review

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.conversationMediaReviewPreviewTestTag
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Before
import org.junit.Rule

private typealias ReviewVisualMedia = ComposerAttachmentUiModel.Resolved.VisualMedia
private typealias ReviewImageAttachment = ComposerAttachmentUiModel.Resolved.VisualMedia.Image
private typealias ReviewVideoAttachment = ComposerAttachmentUiModel.Resolved.VisualMedia.Video

internal abstract class BaseConversationMediaPickerReviewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    protected val onAttachmentPreviewClick = mockk<(ReviewVisualMedia) -> Unit>(relaxed = true)
    protected val onCaptionChange = mockk<(String, String) -> Unit>(relaxed = true)
    protected val onAttachmentRemove = mockk<(String) -> Unit>(relaxed = true)
    protected val onAddMoreClick = mockk<() -> Unit>(relaxed = true)
    protected val onClearReview = mockk<() -> Unit>(relaxed = true)
    protected val onCloseClick = mockk<() -> Unit>(relaxed = true)
    protected val onSendClick = mockk<() -> Unit>(relaxed = true)

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()
    }

    protected fun setReviewContent(
        attachments: ImmutableList<ReviewVisualMedia> = persistentListOf(imageAttachment()),
        conversationTitle: String? = CONVERSATION_TITLE,
        initiallyReviewedContentUri: String? = null,
        reviewRequestSequence: Int = 0,
        isSendActionEnabled: Boolean = true,
        photoPickerSourceContentUriByAttachmentContentUri:
        ImmutableMap<String, String> = persistentMapOf(),
        contentPadding: PaddingValues = PaddingValues(),
    ) {
        setReviewContent(
            attachments = { attachments },
            conversationTitle = { conversationTitle },
            initiallyReviewedContentUri = { initiallyReviewedContentUri },
            reviewRequestSequence = { reviewRequestSequence },
            isSendActionEnabled = { isSendActionEnabled },
            photoPickerSourceContentUriByAttachmentContentUri = {
                photoPickerSourceContentUriByAttachmentContentUri
            },
            contentPadding = contentPadding,
        )
    }

    protected fun setReviewContent(
        attachments: () -> ImmutableList<ReviewVisualMedia>,
        conversationTitle: () -> String? = { CONVERSATION_TITLE },
        initiallyReviewedContentUri: () -> String? = { null },
        reviewRequestSequence: () -> Int = { 0 },
        isSendActionEnabled: () -> Boolean = { true },
        photoPickerSourceContentUriByAttachmentContentUri:
        () -> ImmutableMap<String, String> = { persistentMapOf() },
        contentPadding: PaddingValues = PaddingValues(),
    ) {
        composeTestRule.setContent {
            ReviewContent {
                ConversationMediaReviewScene(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    attachments = attachments(),
                    conversationTitle = conversationTitle(),
                    initiallyReviewedContentUri = initiallyReviewedContentUri(),
                    reviewRequestSequence = reviewRequestSequence(),
                    isSendActionEnabled = isSendActionEnabled(),
                    photoPickerSourceContentUriByAttachmentContentUri =
                        photoPickerSourceContentUriByAttachmentContentUri(),
                    onAttachmentPreviewClick = onAttachmentPreviewClick,
                    onCaptionChange = onCaptionChange,
                    onAttachmentRemove = onAttachmentRemove,
                    onAddMoreClick = onAddMoreClick,
                    onClearReview = onClearReview,
                    onCloseClick = onCloseClick,
                    onSendClick = onSendClick,
                )
            }
        }
    }

    @Composable
    protected fun ReviewContent(content: @Composable () -> Unit) {
        AppTheme(content = content)
    }

    protected fun captionTextField(): SemanticsNodeInteraction {
        return composeTestRule.onNode(hasSetTextAction())
    }

    protected fun clickReviewPageCenter(contentUri: String = IMAGE_CONTENT_URI) {
        composeTestRule
            .onNodeWithTag(
                testTag = conversationMediaReviewPreviewTestTag(contentUri = contentUri),
            )
            .performTouchInput {
                click(position = center)
            }
    }

    protected fun awaitText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            composeTestRule
                .onAllNodesWithText(text = text)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    protected fun imageAttachment(
        key: String = IMAGE_KEY,
        contentUri: String = IMAGE_CONTENT_URI,
        captionText: String = IMAGE_CAPTION,
        width: Int? = 640,
        height: Int? = 480,
    ): ReviewImageAttachment {
        return ReviewImageAttachment(
            key = key,
            contentType = IMAGE_CONTENT_TYPE,
            contentUri = contentUri,
            captionText = captionText,
            width = width,
            height = height,
        )
    }

    protected fun videoAttachment(
        key: String = VIDEO_KEY,
        contentUri: String = VIDEO_CONTENT_URI,
        captionText: String = VIDEO_CAPTION,
        width: Int? = 1280,
        height: Int? = 720,
    ): ReviewVideoAttachment {
        return ReviewVideoAttachment(
            key = key,
            contentType = VIDEO_CONTENT_TYPE,
            contentUri = contentUri,
            captionText = captionText,
            width = width,
            height = height,
        )
    }

    protected fun threeAttachments(): ImmutableList<ReviewVisualMedia> {
        return persistentListOf(
            imageAttachment(
                key = FIRST_IMAGE_KEY,
                contentUri = FIRST_IMAGE_CONTENT_URI,
                captionText = FIRST_IMAGE_CAPTION,
            ),
            videoAttachment(
                key = VIDEO_KEY,
                contentUri = VIDEO_CONTENT_URI,
                captionText = VIDEO_CAPTION,
            ),
            imageAttachment(
                key = LAST_IMAGE_KEY,
                contentUri = LAST_IMAGE_CONTENT_URI,
                captionText = LAST_IMAGE_CAPTION,
            ),
        )
    }

    protected companion object {
        const val CONVERSATION_TITLE = "Weekend plan"
        const val IMAGE_KEY = "image-1"
        const val IMAGE_CONTENT_URI = "content://media/review/image/1"
        const val IMAGE_CONTENT_TYPE = "image/jpeg"
        const val IMAGE_CAPTION = "Image caption"
        const val FIRST_IMAGE_KEY = "image-first"
        const val FIRST_IMAGE_CONTENT_URI = "content://media/review/image/first"
        const val FIRST_IMAGE_CAPTION = "First image caption"
        const val LAST_IMAGE_KEY = "image-last"
        const val LAST_IMAGE_CONTENT_URI = "content://media/review/image/last"
        const val LAST_IMAGE_CAPTION = "Last image caption"
        const val VIDEO_KEY = "video-1"
        const val VIDEO_CONTENT_URI = "content://media/review/video/1"
        const val VIDEO_CONTENT_TYPE = "video/mp4"
        const val VIDEO_CAPTION = "Video caption"
        const val PHOTO_PICKER_SOURCE_URI = "content://photo-picker/source/1"
    }
}
