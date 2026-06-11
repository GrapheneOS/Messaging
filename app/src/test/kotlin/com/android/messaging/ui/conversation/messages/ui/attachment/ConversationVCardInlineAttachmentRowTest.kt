package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationVCardInlineAttachmentRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadedContactUiModel_rendersDisplayNameAndDetails() {
        setContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "attachment-1",
                contentUri = "content://mms/part/vcard-1",
                openAction = null,
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                titleText = "Sam Rivera",
                titleTextResId = null,
                subtitleText = "sam@example.com",
                subtitleTextResId = null,
            ),
        )

        composeTestRule
            .onNodeWithText("Sam Rivera")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("sam@example.com")
            .assertIsDisplayed()
    }

    @Test
    fun loadedLocationUiModel_withoutName_usesLocationFallbackTitle() {
        setContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "attachment-1",
                contentUri = "content://mms/part/vcard-1",
                openAction = null,
                type = ConversationVCardAttachmentType.LOCATION,
                avatarUri = null,
                titleText = null,
                titleTextResId = R.string.notification_location,
                subtitleText = "25 11th Ave New York NY 10011 United States",
                subtitleTextResId = null,
            ),
        )

        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.notification_location))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("25 11th Ave New York NY 10011 United States")
            .assertIsDisplayed()
    }

    @Test
    fun missingUiModelDetails_rendersDefaultStringsFromResources() {
        setContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "attachment-1",
                contentUri = "content://mms/part/vcard-1",
                openAction = null,
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.vcard_tap_hint,
            ),
        )

        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.notification_vcard))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.vcard_tap_hint))
            .assertIsDisplayed()
    }

    private fun setContent(
        attachment: ConversationInlineAttachment.VCard,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationVCardInlineAttachmentRow(
                    attachment = attachment,
                    isSelectionMode = false,
                    onAttachmentClick = { _, _ -> },
                    onExternalUriClick = {},
                    onLongClick = {},
                )
            }
        }
    }
}
