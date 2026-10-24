package com.android.messaging.ui.common.components.participant

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ParticipantQuickActionsPopupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun copyAreaClick_withOneTarget_copiesPhoneNumber() {
        var copiedPhoneNumber: String? = null
        setPopup(
            targets = persistentListOf(target("Ada", "+1 555 0001")),
            onPhoneNumberCopy = { copiedPhoneNumber = it },
        )

        composeTestRule
            .onNodeWithTag(PARTICIPANT_QUICK_ACTIONS_COPY_AREA_TEST_TAG)
            .performClick()

        assertEquals("+1 555 0001", copiedPhoneNumber)
    }

    @Test
    fun copyAreaLongClick_withOneTarget_copiesPhoneNumber() {
        var copiedPhoneNumber: String? = null
        setPopup(
            targets = persistentListOf(target("Ada", "+1 555 0001")),
            onPhoneNumberCopy = { copiedPhoneNumber = it },
        )

        composeTestRule
            .onNodeWithTag(PARTICIPANT_QUICK_ACTIONS_COPY_AREA_TEST_TAG)
            .performSemanticsAction(SemanticsActions.OnLongClick)

        assertEquals("+1 555 0001", copiedPhoneNumber)
    }

    @Test
    fun copyAreaClick_withMultipleTargets_showsChooserAndCopiesSelection() {
        var copiedPhoneNumber: String? = null
        setPopup(
            targets = persistentListOf(
                target("Ada", "+1 555 0001"),
                target("Grace", "+1 555 0002"),
            ),
            onPhoneNumberCopy = { copiedPhoneNumber = it },
        )

        composeTestRule
            .onNodeWithTag(PARTICIPANT_QUICK_ACTIONS_COPY_AREA_TEST_TAG)
            .performClick()
        composeTestRule
            .onNodeWithText("Grace")
            .assertIsDisplayed()
            .performClick()

        assertEquals("+1 555 0002", copiedPhoneNumber)
    }

    @Test
    fun actionButtonClick_doesNotCopyPhoneNumber() {
        var messageClicks = 0
        var copyClicks = 0
        setPopup(
            targets = persistentListOf(target("Ada", "+1 555 0001")),
            onMessageClick = { messageClicks++ },
            onPhoneNumberCopy = { copyClicks++ },
        )

        composeTestRule
            .onNodeWithContentDescription(
                targetContext.getString(R.string.action_send_message),
            )
            .performClick()

        assertEquals(1, messageClicks)
        assertEquals(0, copyClicks)
    }

    private fun setPopup(
        targets: ImmutableList<PhoneNumberCopyTarget>,
        onMessageClick: () -> Unit = {},
        onPhoneNumberCopy: (String) -> Unit,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ParticipantQuickActionsPopup(
                    visible = true,
                    avatarUri = null,
                    displayName = "Conversation",
                    subtitle = null,
                    fallbackIcon = Icons.Default.Person,
                    fallbackLabel = "C",
                    onDismiss = {},
                    onMessageClick = onMessageClick,
                    onCallClick = {},
                    onContactClick = {},
                    onInfoClick = {},
                    colorSeedCode = null,
                    phoneNumberCopyTargets = targets,
                    onPhoneNumberCopy = onPhoneNumberCopy,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun target(displayName: String, phoneNumber: String) = PhoneNumberCopyTarget(
        displayName = displayName,
        phoneNumber = phoneNumber,
    )
}
