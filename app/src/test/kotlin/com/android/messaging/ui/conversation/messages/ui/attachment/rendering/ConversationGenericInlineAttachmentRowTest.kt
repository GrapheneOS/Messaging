package com.android.messaging.ui.conversation.messages.ui.attachment.rendering

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentOpenAction
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationGenericInlineAttachmentRowTest :
    BaseConversationMessageAttachmentRenderingTest() {

    @Test
    fun explicitTitle_rendersTitleText() {
        setGenericInlineAttachmentContent(
            attachment = fileInlineAttachment(titleText = "Quarterly report.pdf"),
        )

        composeTestRule
            .onNodeWithText("Quarterly report.pdf")
            .assertIsDisplayed()
    }

    @Test
    fun fallbackTitleAndSubtitleResources_renderLocalizedText() {
        setGenericInlineAttachmentContent(
            attachment = fileInlineAttachment(
                subtitleTextResId = R.string.copy_to_clipboard,
                titleText = null,
                titleTextResId = R.string.notification_file,
            ),
        )

        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.notification_file))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.copy_to_clipboard))
            .assertIsDisplayed()
    }

    @Test
    fun openExternalAction_forwardsExternalUri() {
        setGenericInlineAttachmentContent(
            attachment = fileInlineAttachment(
                openAction = ConversationAttachmentOpenAction.OpenExternal(
                    uri = YOUTUBE_SOURCE_URI,
                ),
            ),
        )

        clickInlineRow()

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
    fun nullOpenAction_clickDoesNotDispatchOpenCallbacks() {
        setGenericInlineAttachmentContent(
            attachment = fileInlineAttachment(openAction = null),
        )

        clickInlineRow()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onAttachmentClick.invoke(any(), any())
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun longClickForwardsCallbackWithoutOpeningAttachment() {
        setGenericInlineAttachmentContent(
            attachment = fileInlineAttachment(),
        )

        longClickInlineRow()

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
