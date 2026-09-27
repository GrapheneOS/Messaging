package com.android.messaging.ui.conversation.messages.ui.message.rendering

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.conversationMessageSelectionRowTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.screen.model.ConversationMessageAction
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageAccessibilityActionsTest :
    BaseConversationMessageRenderingTest() {

    @Test
    fun nodeHoldingTheTextOffersTheMessageActions() {
        setConversationMessageContent(message = message())

        assertEquals(
            labelsOf(
                ConversationMessageAction.Copy,
                ConversationMessageAction.Delete,
                ConversationMessageAction.Share,
                ConversationMessageAction.Forward,
                ConversationMessageAction.Details,
            ),
            customActionLabels(config = bodyTextNodeConfig()),
        )
    }

    @Test
    fun invokingAnActionReportsItForTheMessage() {
        setConversationMessageContent(message = message())
        val copyLabel = targetContext.getString(R.string.message_context_menu_copy_text)

        bodyTextNodeConfig()[SemanticsActions.CustomActions]
            .single { it.label == copyLabel }
            .action()

        verify(exactly = 1) {
            onMessageActionClick(ConversationMessageAction.Copy)
        }
    }

    @Test
    fun downloadableMessageLeadsWithDownloadAndLabelsTheTap() {
        setConversationMessageContent(message = message(canDownloadMessage = true))

        val config = bodyTextNodeConfig()

        assertEquals(
            targetContext.getString(R.string.action_download),
            customActionLabels(config = config).first(),
        )
        assertEquals(
            targetContext.getString(R.string.action_download),
            config[SemanticsActions.OnClick].label,
        )
    }

    @Test
    fun messageWhoseTapDoesNothingOffersNoTap() {
        setConversationMessageContent(message = message())

        assertNull(bodyTextNodeConfig().getOrNull(SemanticsActions.OnClick))
    }

    @Test
    fun selectionModeOffersTheTapThatTogglesTheMessage() {
        setConversationMessageContent(message = message(), isSelectionMode = true)

        assertNotNull(bodyTextNodeConfig().getOrNull(SemanticsActions.OnClick))
    }

    @Test
    fun longPressIsLabelledAsSelect() {
        setConversationMessageContent(message = message())

        assertEquals(
            targetContext.getString(R.string.conversation_message_select),
            bodyTextNodeConfig()[SemanticsActions.OnLongClick].label,
        )
    }

    @Test
    fun imageOnlyBubbleStillOffersTheMessageActions() {
        setConversationMessageContent(
            message = message(text = null, parts = persistentListOf(imagePart())),
        )

        val config = composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageSelectionRowTestTag(
                    messageId = MessageId(DEFAULT_MESSAGE_ID),
                ),
            )
            .fetchSemanticsNode()
            .config

        assertEquals(
            targetContext.getString(R.string.message_context_menu_view_details),
            customActionLabels(config = config).last(),
        )
    }

    @Test
    fun statusIsReadWithTheMessageThatOffersTheActions() {
        setConversationMessageContent(
            message = message(status = ConversationMessageUiModel.Status.Outgoing.Delivered),
        )
        val deliveredStatus = targetContext.getString(R.string.delivered_status_content_description)

        val config = composeTestRule
            .onNodeWithText(text = deliveredStatus, substring = true)
            .fetchSemanticsNode()
            .config

        assertTrue(
            config[SemanticsProperties.Text].contains(AnnotatedString(text = DEFAULT_BODY_TEXT)),
        )
        assertEquals(
            targetContext.getString(R.string.message_context_menu_view_details),
            customActionLabels(config = config).last(),
        )
    }

    @Test
    fun selectionModeOffersNoMessageActions() {
        setConversationMessageContent(message = message(), isSelectionMode = true)

        assertNull(bodyTextNodeConfig().getOrNull(SemanticsActions.CustomActions))
    }

    @Test
    fun simIsReadWithTheMessageWithoutItsLinkTakingTheDoubleTap() {
        setConversationMessageContent(message = message(), simDisplayName = SIM_DISPLAY_NAME)

        val texts = bodyTextNodeConfig()[SemanticsProperties.Text]

        assertTrue(texts.any { it.text.contains(SIM_DISPLAY_NAME) })
        assertTrue(texts.all { it.getLinkAnnotations(start = 0, end = it.length).isEmpty() })
    }

    @Test
    fun imageIsNamedAndOffersToOpenIt() {
        setConversationMessageContent(
            message = message(text = null, parts = persistentListOf(imagePart())),
        )

        val config = imageNodeConfig()

        assertEquals(
            targetContext.getString(R.string.conversation_attachment_open),
            config[SemanticsActions.OnClick].label,
        )
        assertEquals(
            targetContext.getString(R.string.conversation_message_select),
            config[SemanticsActions.OnLongClick].label,
        )
    }

    @Test
    fun imageOfAMessageWithItsOwnTapIsReadWithTheMessageThatTakesTheTap() {
        setConversationMessageContent(
            message = message(
                parts = persistentListOf(imagePart()),
                status = ConversationMessageUiModel.Status.Outgoing.Failed,
                canResendMessage = true,
            ),
        )

        val config = imageNodeConfig()

        assertTrue(
            config[SemanticsProperties.Text].contains(AnnotatedString(text = DEFAULT_BODY_TEXT)),
        )
        assertEquals(
            targetContext.getString(R.string.action_send),
            config[SemanticsActions.OnClick].label,
        )
    }

    @Test
    fun imageInSelectionModeIsReadWithTheMessageThatTakesTheTap() {
        setConversationMessageContent(
            message = message(parts = persistentListOf(imagePart())),
            isSelectionMode = true,
        )

        assertTrue(
            imageNodeConfig()[SemanticsProperties.Text]
                .contains(AnnotatedString(text = DEFAULT_BODY_TEXT)),
        )
    }

    @Test
    fun attachmentWithNothingToOpenOffersNoTap() {
        setConversationMessageContent(
            message = message(
                parts = persistentListOf(
                    ConversationMessagePartUiModel.Attachment.File(
                        text = null,
                        contentType = FILE_CONTENT_TYPE,
                        contentUri = null,
                        width = 0,
                        height = 0,
                    ),
                ),
            ),
        )

        val config = composeTestRule
            .onNodeWithText(text = FILE_CONTENT_TYPE)
            .fetchSemanticsNode()
            .config

        assertNull(config.getOrNull(SemanticsActions.OnClick))
    }

    private fun imageNodeConfig(): SemanticsConfiguration {
        return composeTestRule
            .onNodeWithContentDescription(
                label = targetContext.getString(R.string.conversation_list_snippet_picture),
            )
            .fetchSemanticsNode()
            .config
    }

    private fun bodyTextNodeConfig(): SemanticsConfiguration {
        return composeTestRule
            .onNodeWithText(text = DEFAULT_BODY_TEXT)
            .fetchSemanticsNode()
            .config
    }

    private fun customActionLabels(config: SemanticsConfiguration): List<String> {
        return config[SemanticsActions.CustomActions].map { it.label }
    }

    private fun labelsOf(vararg actions: ConversationMessageAction): List<String> {
        return actions.map { targetContext.getString(it.labelRes) }
    }

    private companion object {
        private const val FILE_CONTENT_TYPE = "application/pdf"
        private const val SIM_DISPLAY_NAME = "Work"
    }
}
