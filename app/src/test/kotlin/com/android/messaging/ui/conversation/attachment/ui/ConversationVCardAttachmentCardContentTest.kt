package com.android.messaging.ui.conversation.attachment.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationVCardAttachmentCardContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun contactTitle_rendersFallbackInitialAndSubtitle() {
        setContent(
            type = ConversationVCardAttachmentType.CONTACT,
            avatarUri = null,
            titleText = CONTACT_TITLE,
            titleTextResId = null,
            subtitleText = CONTACT_SUBTITLE,
            subtitleTextResId = null,
        )

        composeTestRule
            .onNodeWithText(text = CONTACT_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = CONTACT_SUBTITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = CONTACT_INITIAL)
            .assertIsDisplayed()
    }

    @Test
    fun blankContactTitle_usesResourceTitleAndOmitsInitialLabel() {
        val fallbackTitle = targetContext.getString(R.string.notification_vcard)

        setContent(
            type = ConversationVCardAttachmentType.CONTACT,
            avatarUri = null,
            titleText = null,
            titleTextResId = R.string.notification_vcard,
            subtitleText = null,
            subtitleTextResId = null,
        )

        composeTestRule
            .onNodeWithText(text = fallbackTitle)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = CONTACT_INITIAL)
            .assertDoesNotExist()
    }

    @Test
    fun remoteAvatarUri_keepsTextFallbackInitial() {
        setContent(
            type = ConversationVCardAttachmentType.CONTACT,
            avatarUri = REMOTE_AVATAR_URI,
            titleText = REMOTE_CONTACT_TITLE,
            titleTextResId = null,
            subtitleText = null,
            subtitleTextResId = R.string.vcard_tap_hint,
        )

        composeTestRule
            .onNodeWithText(text = REMOTE_CONTACT_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = REMOTE_CONTACT_INITIAL)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.vcard_tap_hint))
            .assertIsDisplayed()
    }

    @Test
    fun locationContent_usesLocationTitleAndSubtitleResources() {
        setContent(
            type = ConversationVCardAttachmentType.LOCATION,
            avatarUri = null,
            titleText = null,
            titleTextResId = R.string.notification_location,
            subtitleText = null,
            subtitleTextResId = R.string.vcard_tap_hint,
        )

        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.notification_location))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.vcard_tap_hint))
            .assertIsDisplayed()
    }

    private fun setContent(
        type: ConversationVCardAttachmentType,
        avatarUri: String?,
        titleText: String?,
        titleTextResId: Int?,
        subtitleText: String?,
        subtitleTextResId: Int?,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationVCardAttachmentCardContent(
                    type = type,
                    avatarUri = avatarUri,
                    titleText = titleText,
                    titleTextResId = titleTextResId,
                    subtitleText = subtitleText,
                    subtitleTextResId = subtitleTextResId,
                )
            }
        }
    }

    private companion object {
        private const val CONTACT_INITIAL = "S"
        private const val CONTACT_SUBTITLE = "sam@example.com"
        private const val CONTACT_TITLE = "Sam Rivera"
        private const val REMOTE_AVATAR_URI = "https://example.com/avatar.jpg"
        private const val REMOTE_CONTACT_INITIAL = "R"
        private const val REMOTE_CONTACT_TITLE = "Remote Person"
    }
}
