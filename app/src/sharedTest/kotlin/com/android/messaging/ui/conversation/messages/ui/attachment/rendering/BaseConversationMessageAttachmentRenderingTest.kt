package com.android.messaging.ui.conversation.messages.ui.attachment.rendering

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentSections
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.messages.ui.attachment.ConversationGenericInlineAttachmentRow
import com.android.messaging.ui.conversation.messages.ui.attachment.ConversationInlineAttachmentRow
import com.android.messaging.ui.conversation.messages.ui.attachment.ConversationInlineAudioAttachmentRowContent
import com.android.messaging.ui.conversation.messages.ui.attachment.ConversationMessageAttachments
import com.android.messaging.ui.conversation.messages.ui.attachment.ConversationStandaloneVisualAttachment
import com.android.messaging.ui.conversation.messages.ui.attachment.rememberConversationInlineAudioAttachmentColors
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule

internal abstract class BaseConversationMessageAttachmentRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    protected val onAttachmentClick = mockk<(String, String) -> Unit>(relaxed = true)
    protected val onExternalUriClick = mockk<(String) -> Unit>(relaxed = true)
    protected val onMessageLongClick = mockk<() -> Unit>(relaxed = true)
    protected val onAudioRowClick = mockk<() -> Unit>(relaxed = true)

    @Before
    fun setUp() {
        clearAllMocks()
    }

    protected fun setMessageAttachmentsContent(
        attachmentSections: ConversationAttachmentSections,
        hasTextAboveVisualAttachments: Boolean = false,
        hasTextBelowVisualAttachments: Boolean = false,
        isIncoming: Boolean = true,
        isSelectionMode: Boolean = false,
        useStandaloneAudioAttachmentBackground: Boolean = false,
        width: Dp = ATTACHMENT_WIDTH,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationMessageAttachments(
                    modifier = Modifier
                        .width(width = width)
                        .testTag(tag = MESSAGE_ATTACHMENTS_TAG),
                    attachmentSections = attachmentSections,
                    hasTextAboveVisualAttachments = hasTextAboveVisualAttachments,
                    hasTextBelowVisualAttachments = hasTextBelowVisualAttachments,
                    isIncoming = isIncoming,
                    isSelectionMode = isSelectionMode,
                    useStandaloneAudioAttachmentBg = useStandaloneAudioAttachmentBackground,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onMessageLongClick = onMessageLongClick,
                )
            }
        }
    }

    protected fun setStandaloneVisualAttachmentContent(
        attachment: ConversationMessageAttachment,
        hasTextAboveVisualAttachments: Boolean = false,
        hasTextBelowVisualAttachments: Boolean = false,
        width: Dp = ATTACHMENT_WIDTH,
    ) {
        composeTestRule.setContent {
            AppTheme {
                Box(
                    modifier = Modifier
                        .width(width = width)
                        .testTag(tag = STANDALONE_VISUAL_TAG),
                ) {
                    ConversationStandaloneVisualAttachment(
                        attachment = attachment,
                        hasTextAboveVisualAttachments = hasTextAboveVisualAttachments,
                        hasTextBelowVisualAttachments = hasTextBelowVisualAttachments,
                        onAttachmentClick = onAttachmentClick,
                        onExternalUriClick = onExternalUriClick,
                        onMessageLongClick = onMessageLongClick,
                    )
                }
            }
        }
    }

    protected fun setGenericInlineAttachmentContent(
        attachment: ConversationInlineAttachment.File,
        width: Dp = ATTACHMENT_WIDTH,
    ) {
        composeTestRule.setContent {
            AppTheme {
                Box(
                    modifier = Modifier
                        .width(width = width)
                        .testTag(tag = INLINE_ROW_TAG),
                ) {
                    ConversationGenericInlineAttachmentRow(
                        attachment = attachment,
                        onAttachmentClick = onAttachmentClick,
                        onExternalUriClick = onExternalUriClick,
                        onLongClick = onMessageLongClick,
                    )
                }
            }
        }
    }

    protected fun setInlineAttachmentRowContent(
        attachment: ConversationInlineAttachment,
        isIncoming: Boolean = true,
        isSelectionMode: Boolean = false,
        useStandaloneAudioAttachmentBackground: Boolean = false,
        width: Dp = ATTACHMENT_WIDTH,
    ) {
        composeTestRule.setContent {
            AppTheme {
                Box(
                    modifier = Modifier
                        .width(width = width)
                        .testTag(tag = INLINE_ROW_TAG),
                ) {
                    ConversationInlineAttachmentRow(
                        attachment = attachment,
                        isIncoming = isIncoming,
                        isSelectionMode = isSelectionMode,
                        useStandaloneAudioAttachmentBackground =
                        useStandaloneAudioAttachmentBackground,
                        onAttachmentClick = onAttachmentClick,
                        onExternalUriClick = onExternalUriClick,
                        onLongClick = onMessageLongClick,
                    )
                }
            }
        }
    }

    protected fun setAudioRowContent(
        isSelectionMode: Boolean,
        isIncoming: Boolean = true,
        useStandaloneAudioAttachmentBackground: Boolean = false,
        width: Dp = ATTACHMENT_WIDTH,
    ) {
        composeTestRule.setContent {
            AppTheme {
                Box(
                    modifier = Modifier
                        .width(width = width)
                        .testTag(tag = INLINE_ROW_TAG),
                ) {
                    ConversationInlineAudioAttachmentRowContent(
                        colors = rememberConversationInlineAudioAttachmentColors(
                            isIncoming = isIncoming,
                            isSelectionMode = isSelectionMode,
                            useStandaloneAudioAttachmentBackground =
                            useStandaloneAudioAttachmentBackground,
                        ),
                        isSelectionMode = isSelectionMode,
                        isPlaying = false,
                        title = AUDIO_TITLE,
                        durationLabel = AUDIO_DURATION,
                        progress = 0f,
                        onClick = onAudioRowClick,
                        onLongClick = onMessageLongClick,
                    )
                }
            }
        }
    }

    protected fun clickMessageAttachmentsAt(
        xFraction: Float = 0.5f,
        yFraction: Float = 0.5f,
    ) {
        composeTestRule
            .onNodeWithTag(testTag = MESSAGE_ATTACHMENTS_TAG)
            .performFractionalClick(
                xFraction = xFraction,
                yFraction = yFraction,
            )
    }

    protected fun longClickMessageAttachmentsAt(
        xFraction: Float = 0.5f,
        yFraction: Float = 0.5f,
    ) {
        composeTestRule
            .onNodeWithTag(testTag = MESSAGE_ATTACHMENTS_TAG)
            .performFractionalLongClick(
                xFraction = xFraction,
                yFraction = yFraction,
            )
    }

    protected fun clickInlineRow() {
        composeTestRule
            .onNodeWithTag(testTag = INLINE_ROW_TAG)
            .performFractionalClick()
    }

    protected fun longClickInlineRow() {
        composeTestRule
            .onNodeWithTag(testTag = INLINE_ROW_TAG)
            .performFractionalLongClick()
    }

    protected fun clickStandaloneVisual() {
        composeTestRule
            .onNodeWithTag(testTag = STANDALONE_VISUAL_TAG)
            .performFractionalClick()
    }

    protected fun longClickStandaloneVisual() {
        composeTestRule
            .onNodeWithTag(testTag = STANDALONE_VISUAL_TAG)
            .performFractionalLongClick()
    }

    private fun SemanticsNodeInteraction.performFractionalClick(
        xFraction: Float = 0.5f,
        yFraction: Float = 0.5f,
    ) {
        performTouchInput {
            click(
                position = Offset(
                    x = width * xFraction,
                    y = height * yFraction,
                ),
            )
        }
    }

    private fun SemanticsNodeInteraction.performFractionalLongClick(
        xFraction: Float = 0.5f,
        yFraction: Float = 0.5f,
    ) {
        performTouchInput {
            longClick(
                position = Offset(
                    x = width * xFraction,
                    y = height * yFraction,
                ),
            )
        }
    }
}
