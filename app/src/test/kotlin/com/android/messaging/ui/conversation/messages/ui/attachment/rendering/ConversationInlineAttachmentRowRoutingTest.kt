package com.android.messaging.ui.conversation.messages.ui.attachment.rendering

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationInlineAttachmentRowRoutingTest :
    BaseConversationMessageAttachmentRenderingTest() {

    @Test
    fun audioAttachmentRow_rendersAudioTitle() {
        setInlineAttachmentRowContent(
            attachment = audioInlineAttachment(),
            isSelectionMode = true,
        )

        composeTestRule
            .onNodeWithText(AUDIO_TITLE)
            .assertIsDisplayed()
    }

    @Test
    fun vCardAttachmentRow_rendersVCardDetailsAndOpensContent() {
        setInlineAttachmentRowContent(
            attachment = vCardInlineAttachment(),
        )

        composeTestRule
            .onNodeWithText(VCARD_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(VCARD_SUBTITLE)
            .assertIsDisplayed()

        clickInlineRow()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentClick.invoke(VCARD_CONTENT_TYPE, VCARD_CONTENT_URI, "")
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun vCardSelectionMode_clickAndLongClickDoNotDispatchCallbacks() {
        setInlineAttachmentRowContent(
            attachment = vCardInlineAttachment(),
            isSelectionMode = true,
        )

        clickInlineRow()
        longClickInlineRow()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onAttachmentClick.invoke(any(), any(), any())
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
            verify(exactly = 0) {
                onMessageLongClick.invoke()
            }
        }
    }

    @Test
    fun vCardWithoutOpenAction_clickDoesNotDispatchOpenCallbacks() {
        setInlineAttachmentRowContent(
            attachment = vCardInlineAttachment(openAction = null),
        )

        clickInlineRow()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onAttachmentClick.invoke(any(), any(), any())
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun fileAttachmentRow_rendersFileTitleAndOpensContent() {
        setInlineAttachmentRowContent(
            attachment = fileInlineAttachment(),
        )

        composeTestRule
            .onNodeWithText(FILE_TITLE)
            .assertIsDisplayed()

        clickInlineRow()

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
    fun audioContentSelectionMode_clickDoesNotStartPlayback() {
        setAudioRowContent(isSelectionMode = true)

        clickInlineRow()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onAudioRowClick.invoke()
            }
        }
    }

    @Test
    fun audioContentNonSelectionMode_clickAndLongClickForwardCallbacks() {
        setAudioRowContent(isSelectionMode = false)

        clickInlineRow()
        longClickInlineRow()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAudioRowClick.invoke()
            }
            verify(exactly = 1) {
                onMessageLongClick.invoke()
            }
        }
    }

    @Test
    fun audioContent_labelsTheTapAsPlayAndTheLongPressAsSelect() {
        setAudioRowContent(isSelectionMode = false)

        val playDescription = targetContext.getString(R.string.audio_play_content_description)
        val config = composeTestRule
            .onNodeWithContentDescription(label = playDescription)
            .fetchSemanticsNode()
            .config

        assertEquals(playDescription, config[SemanticsActions.OnClick].label)
        assertEquals(
            targetContext.getString(R.string.conversation_message_select),
            config[SemanticsActions.OnLongClick].label,
        )
    }

    @Test
    fun incomingAudioContentWithStandaloneBackground_remainsClickable() {
        setAudioRowContent(
            isSelectionMode = false,
            isIncoming = true,
            useStandaloneAudioAttachmentBackground = true,
        )

        composeTestRule
            .onNodeWithText(AUDIO_TITLE)
            .assertIsDisplayed()

        clickInlineRow()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAudioRowClick.invoke()
            }
        }
    }

    @Test
    fun outgoingAudioContentWithStandaloneBackground_remainsClickable() {
        setAudioRowContent(
            isSelectionMode = false,
            isIncoming = false,
            useStandaloneAudioAttachmentBackground = true,
        )

        composeTestRule
            .onNodeWithText(AUDIO_TITLE)
            .assertIsDisplayed()

        clickInlineRow()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAudioRowClick.invoke()
            }
        }
    }
}
