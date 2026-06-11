package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel.Resolved.VCard
import com.android.messaging.ui.conversation.conversationAttachmentPreviewItemTestTag
import com.android.messaging.ui.conversation.conversationAttachmentPreviewRemoveButtonTestTag
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationAttachmentPreviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyAttachments_doNotRenderList() {
        setContent(attachments = persistentListOf())

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun mixedAttachments_rendersAllItems() {
        setContent()

        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewItemTestTag(PENDING_KEY))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewItemTestTag(RESOLVED_KEY))
            .assertIsDisplayed()
    }

    @Test
    fun resolvedVisualAttachment_clickForwardsCallback() {
        val clicks = mutableListOf<ComposerAttachmentUiModel.Resolved>()
        setContent(
            onResolvedAttachmentClick = { clicks += it },
        )

        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewItemTestTag(RESOLVED_KEY))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(resolvedAttachment), clicks)
        }
    }

    @Test
    fun vCardAttachment_rendersPreparedUiModelText() {
        setContent(
            attachments = persistentListOf(vCardAttachment),
        )

        composeTestRule
            .onNodeWithText("Sam Rivera")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("555-000-8901")
            .assertIsDisplayed()
    }

    @Test
    fun audioAttachment_rendersFormattedDuration() {
        setContent(
            attachments = persistentListOf(audioAttachment),
        )

        composeTestRule
            .onNodeWithText("05:39")
            .assertIsDisplayed()
    }

    @Test
    fun pendingAttachment_removeButtonForwardsCallback() {
        val removals = mutableListOf<String>()
        setContent(
            onPendingAttachmentRemove = { removals += it },
        )

        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewRemoveButtonTestTag(PENDING_KEY))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(PENDING_KEY), removals)
        }
    }

    @Test
    fun pendingAudioFinalizingAttachment_rendersAudioPlaceholderWithoutRemoveButton() {
        setContent(
            attachments = persistentListOf(pendingAudioFinalizingAttachment),
        )

        composeTestRule
            .onNodeWithText(
                targetContext.getString(R.string.audio_recording_finalizing_attachment_label),
            )
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("00:00")
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onAllNodesWithTag(
                conversationAttachmentPreviewRemoveButtonTestTag(PENDING_AUDIO_KEY),
            )
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun resolvedAttachment_removeButtonForwardsCallback() {
        val removals = mutableListOf<String>()
        setContent(
            onResolvedAttachmentRemove = { removals += it },
        )

        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewRemoveButtonTestTag(RESOLVED_KEY))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(RESOLVED_CONTENT_URI), removals)
        }
    }

    private fun setContent(
        attachments: ImmutableList<ComposerAttachmentUiModel> =
            persistentListOf(pendingAttachment, resolvedAttachment),
        onPendingAttachmentRemove: (String) -> Unit = {},
        onResolvedAttachmentClick: (ComposerAttachmentUiModel.Resolved) -> Unit = {},
        onResolvedAttachmentRemove: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationAttachmentPreview(
                    attachments = attachments,
                    onPendingAttachmentRemove = onPendingAttachmentRemove,
                    onResolvedAttachmentClick = onResolvedAttachmentClick,
                    onResolvedAttachmentRemove = onResolvedAttachmentRemove,
                )
            }
        }
    }

    private companion object {
        private const val PENDING_KEY = "pending-1"
        private const val PENDING_AUDIO_KEY = "pending-audio-1"
        private const val RESOLVED_KEY = "resolved-1"
        private const val RESOLVED_CONTENT_URI = "content://media/resolved/1"

        private val pendingAttachment = ComposerAttachmentUiModel.Pending.Generic(
            key = PENDING_KEY,
            contentType = "image/jpeg",
            contentUri = "content://media/pending/1",
            displayName = "pending.jpg",
        )
        private val pendingAudioFinalizingAttachment =
            ComposerAttachmentUiModel.Pending.AudioFinalizing(
                key = PENDING_AUDIO_KEY,
                contentType = "audio/3gpp",
                contentUri = "pending://audio/1",
                displayName = "",
            )
        private val resolvedAttachment = ComposerAttachmentUiModel.Resolved.VisualMedia.Video(
            key = RESOLVED_KEY,
            contentType = "video/mp4",
            contentUri = RESOLVED_CONTENT_URI,
            captionText = "Caption",
            width = 640,
            height = 480,
        )
        private val vCardAttachment = VCard(
            key = "resolved-vcard-1",
            contentType = "text/x-vCard",
            contentUri = "content://contacts/as_vcard/1",
            vCardUiModel = ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = "Sam Rivera",
                subtitleText = "555-000-8901",
            ),
        )
        private val audioAttachment = ComposerAttachmentUiModel.Resolved.Audio(
            key = "resolved-audio-1",
            contentType = "audio/3gpp",
            contentUri = "content://media/audio/1",
            durationMillis = 339_000L,
        )
    }
}
