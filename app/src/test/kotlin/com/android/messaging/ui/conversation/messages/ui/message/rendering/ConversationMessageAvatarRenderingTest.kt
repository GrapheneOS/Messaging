package com.android.messaging.ui.conversation.messages.ui.message.rendering

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageAvatarRenderingTest : BaseConversationMessageRenderingTest() {

    @Test
    fun nameDisplayName_rendersUppercaseInitial() {
        setAvatarContent(
            message = message(
                isIncoming = true,
                senderDisplayName = "  alex  ",
            ),
        )

        composeTestRule
            .onNodeWithText(text = "A")
            .assertIsDisplayed()
    }

    @Test
    fun nullDisplayName_keepsClickableFallbackWithoutInitial() {
        setAvatarContent(
            message = message(
                isIncoming = true,
                senderDisplayName = null,
            ),
        )

        composeTestRule
            .onNodeWithTag(testTag = AVATAR_TAG)
            .assertIsDisplayed()
        clickAvatar()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAvatarClick.invoke()
            }
        }
    }

    @Test
    fun blankDisplayName_omitsInitial() {
        setAvatarContent(
            message = message(
                isIncoming = true,
                senderDisplayName = "   ",
            ),
        )

        composeTestRule
            .onNodeWithTag(testTag = AVATAR_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = " ")
            .assertDoesNotExist()
    }

    @Test
    fun phoneNumberDisplayName_omitsInitial() {
        setAvatarContent(
            message = message(
                isIncoming = true,
                senderDisplayName = "+1 650 555 0101",
            ),
        )

        composeTestRule
            .onNodeWithTag(testTag = AVATAR_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "+")
            .assertDoesNotExist()
    }

    @Test
    fun avatarUri_keepsFallbackInitialWhileImageLoads() {
        setAvatarContent(
            message = message(
                isIncoming = true,
                senderDisplayName = "Morgan",
                senderAvatarUri = Uri.parse("content://contacts/avatar/morgan"),
            ),
        )

        composeTestRule
            .onNodeWithText(text = "M")
            .assertIsDisplayed()
    }

    @Test
    fun longClick_forwardsMessageLongClick() {
        setAvatarContent(
            message = message(
                isIncoming = true,
                senderDisplayName = "Riley",
            ),
        )

        longClickAvatar()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageLongClick.invoke()
            }
            verify(exactly = 0) {
                onAvatarClick.invoke()
            }
        }
    }
}
