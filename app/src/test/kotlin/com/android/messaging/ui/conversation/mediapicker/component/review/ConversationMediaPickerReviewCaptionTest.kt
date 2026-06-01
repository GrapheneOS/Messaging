package com.android.messaging.ui.conversation.mediapicker.component.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerReviewCaptionTest : BaseConversationMediaPickerReviewTest() {

    @Test
    fun captionExternalUpdate_whenNotFocusedRefreshesText() {
        var attachments by mutableStateOf(
            persistentListOf(
                imageAttachment(captionText = ""),
            ),
        )

        setReviewContent(
            attachments = { attachments },
        )

        composeTestRule.runOnIdle {
            attachments = persistentListOf(
                imageAttachment(captionText = REMOTE_CAPTION),
            )
        }

        awaitText(REMOTE_CAPTION)

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onCaptionChange.invoke(
                    IMAGE_CONTENT_URI,
                    REMOTE_CAPTION,
                )
            }
        }
    }

    @Test
    fun captionExternalUpdate_doesNotOverwriteFocusedEdit() {
        var attachments by mutableStateOf(
            persistentListOf(
                imageAttachment(captionText = ""),
            ),
        )

        setReviewContent(
            attachments = { attachments },
        )

        captionTextField()
            .performTextInput(LOCAL_CAPTION)

        composeTestRule.runOnIdle {
            attachments = persistentListOf(
                imageAttachment(captionText = REMOTE_CAPTION),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(LOCAL_CAPTION)
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(REMOTE_CAPTION)
            .assertCountEquals(expectedSize = 0)
    }

    private companion object {
        const val LOCAL_CAPTION = "Local caption draft"
        const val REMOTE_CAPTION = "Remote caption"
    }
}
