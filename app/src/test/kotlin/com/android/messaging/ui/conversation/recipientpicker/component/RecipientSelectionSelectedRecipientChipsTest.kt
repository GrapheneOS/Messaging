package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import com.android.messaging.ui.core.AppTheme
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecipientSelectionSelectedRecipientChipsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unarmedChip_clickForwardsRecipientAndShowsFallbackAvatar() {
        val recipient = selectedRecipient(label = "Ada")
        val onRecipientClick = mockk<(SelectedRecipient) -> Unit>(relaxed = true)

        setContent(
            recipients = persistentListOf(recipient),
            onRecipientClick = onRecipientClick,
        )

        composeTestRule
            .onNodeWithContentDescription(selectedRecipientDescription(label = "Ada"))
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText(text = "A")
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onRecipientClick.invoke(recipient)
            }
        }
    }

    @Test
    fun armedChip_usesRemoveDescriptionAndForwardsClick() {
        val recipient = selectedRecipient(label = "Grace")
        val onRecipientClick = mockk<(SelectedRecipient) -> Unit>(relaxed = true)

        setContent(
            recipients = persistentListOf(recipient),
            armedRecipientDestination = recipient.destination,
            onRecipientClick = onRecipientClick,
        )

        composeTestRule
            .onNodeWithContentDescription(removeRecipientDescription(label = "Grace"))
            .assertIsSelected()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onRecipientClick.invoke(recipient)
            }
        }
    }

    @Test
    fun disabledChip_isNotClickable() {
        val recipient = selectedRecipient(label = "Katherine")
        val onRecipientClick = mockk<(SelectedRecipient) -> Unit>(relaxed = true)

        setContent(
            recipients = persistentListOf(recipient),
            enabled = false,
            onRecipientClick = onRecipientClick,
        )

        composeTestRule
            .onNodeWithContentDescription(selectedRecipientDescription(label = "Katherine"))
            .assertIsNotEnabled()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onRecipientClick.invoke(any())
            }
        }
    }

    @Test
    fun optionalLeadingAndTrailingContent_areRenderedAroundChips() {
        setContent(
            recipients = persistentListOf(selectedRecipient(label = "Lin")),
            leadingContent = {
                Text(
                    modifier = Modifier.testTag(tag = LEADING_CONTENT_TAG),
                    text = "Leading",
                )
            },
            trailingContent = { modifier ->
                Text(
                    modifier = modifier.testTag(tag = TRAILING_CONTENT_TAG),
                    text = "Trailing",
                )
            },
        )

        composeTestRule
            .onNodeWithTag(testTag = LEADING_CONTENT_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(testTag = TRAILING_CONTENT_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun remoteAvatarUri_usesImageAvatarInsteadOfTextFallback() {
        setContent(
            recipients = persistentListOf(
                selectedRecipient(
                    label = "Mira",
                    photoUri = "content://avatars/mira",
                ),
            ),
        )

        composeTestRule
            .onNodeWithContentDescription(selectedRecipientDescription(label = "Mira"))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "M")
            .assertDoesNotExist()
    }

    private fun setContent(
        recipients: ImmutableList<SelectedRecipient>,
        armedRecipientDestination: String? = null,
        enabled: Boolean = true,
        onRecipientClick: (SelectedRecipient) -> Unit = mockk(relaxed = true),
        leadingContent: (@Composable () -> Unit)? = null,
        trailingContent: (@Composable (Modifier) -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionSelectedRecipientChips(
                    recipients = recipients,
                    armedRecipientDestination = armedRecipientDestination,
                    enabled = enabled,
                    onRecipientClick = onRecipientClick,
                    leadingContent = leadingContent,
                    trailingContent = trailingContent,
                )
            }
        }
    }

    private fun selectedRecipient(label: String, photoUri: String? = null): SelectedRecipient {
        return SelectedRecipient(
            destination = "+15550100-$label",
            label = label,
            displayDestination = "+1 555 0100",
            photoUri = photoUri,
        )
    }

    private fun selectedRecipientDescription(label: String): String {
        return targetContext.getString(
            R.string.recipient_selection_selected_recipient_content_description,
            label,
        )
    }

    @Suppress("SameParameterValue")
    private fun removeRecipientDescription(label: String): String {
        return targetContext.getString(
            R.string.recipient_selection_remove_selected_recipient_content_description,
            label,
        )
    }

    private companion object {
        private const val LEADING_CONTENT_TAG = "selected_recipient_chips_leading"
        private const val TRAILING_CONTENT_TAG = "selected_recipient_chips_trailing"
    }
}
