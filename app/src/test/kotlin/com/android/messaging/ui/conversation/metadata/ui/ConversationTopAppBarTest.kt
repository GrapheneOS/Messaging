package com.android.messaging.ui.conversation.metadata.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.ui.conversation.CONVERSATION_ADD_CONTACT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ARCHIVE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_CALL_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_DELETE_CONVERSATION_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SHOW_SUBJECT_FIELD_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_UNARCHIVE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationTopAppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun title_whenMetadataPresent_forwardsClick() {
        var clicks = 0

        setContent(onTitleClick = { clicks += 1 })

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    @Test
    fun title_whenMetadataNotPresent_doesNotForwardClick() {
        var clicks = 0

        setContent(
            metadata = ConversationMetadataUiState.Loading,
            onTitleClick = { clicks += 1 },
        )

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(0, clicks)
        }
    }

    @Test
    fun title_whenTitleIsBlank_fallsBackToAppName() {
        val appName = targetContext.getString(R.string.app_name)

        setContent(
            metadata = presentMetadata.copy(title = ""),
        )

        composeTestRule
            .onNodeWithText(appName)
            .assertIsDisplayed()
    }

    @Test
    fun loadingMetadata_showsFallbackTitleAndLoadingSubtitle() {
        setContent(metadata = ConversationMetadataUiState.Loading)

        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.app_name))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.loading_messages))
            .assertIsDisplayed()
    }

    @Test
    fun unavailableMetadata_showsFallbackTitleWithoutLoadingSubtitle() {
        setContent(metadata = ConversationMetadataUiState.Unavailable)

        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.app_name))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.loading_messages))
            .assertDoesNotExist()
    }

    @Test
    fun oneOnOneContact_hidesDestinationSubtitle() {
        val displayDestination = checkNotNull(presentMetadata.otherParticipantDisplayDestination)

        setContent(
            metadata = presentMetadata.copy(
                otherParticipantContactLookupKey = "lookup-key",
            ),
        )

        composeTestRule
            .onNodeWithText(text = displayDestination)
            .assertDoesNotExist()
    }

    @Test
    fun singleAvatarWithPhotoUri_rendersConversationTitle() {
        setContent(
            metadata = presentMetadata.copy(
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = "content://conversation/avatar",
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(text = presentMetadata.title)
            .assertIsDisplayed()
    }

    @Test
    fun navigationIcon_forwardsBackClick() {
        var clicks = 0
        val backDescription = targetContext.getString(R.string.back)

        setContent(onNavigateBack = { clicks += 1 })

        composeTestRule
            .onNodeWithContentDescription(backDescription)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    @Test
    fun callButton_whenVisible_forwardsClick() {
        var clicks = 0

        setContent(
            isCallVisible = true,
            onCallClick = { clicks += 1 },
        )

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_CALL_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    @Test
    fun callButton_whenNotVisible_isHidden() {
        setContent(isCallVisible = false)

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_CALL_BUTTON_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun overflowMenu_isHiddenWhenNoSecondaryActionsAvailable() {
        setContent()

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun addPeopleMenuItem_isShownInOverflow_andForwardsClickAndDismissesMenu() {
        var clicks = 0

        setContent(
            isAddPeopleVisible = true,
            onAddPeopleClick = { clicks += 1 },
        )

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun addContactMenuItem_forwardsClick() {
        var clicks = 0

        setContent(
            isAddContactVisible = true,
            onAddContactClick = { clicks += 1 },
        )

        openOverflowMenuAndClickItem(menuItemTestTag = CONVERSATION_ADD_CONTACT_BUTTON_TEST_TAG)

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    @Test
    fun showSubjectFieldMenuItem_forwardsClick() {
        var clicks = 0

        setContent(
            isShowSubjectFieldVisible = true,
            onShowSubjectFieldClick = { clicks += 1 },
        )

        openOverflowMenuAndClickItem(
            menuItemTestTag = CONVERSATION_SHOW_SUBJECT_FIELD_MENU_ITEM_TEST_TAG,
        )

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    @Test
    fun archiveMenuItem_forwardsClick() {
        var clicks = 0

        setContent(
            isArchiveVisible = true,
            onArchiveClick = { clicks += 1 },
        )

        openOverflowMenuAndClickItem(menuItemTestTag = CONVERSATION_ARCHIVE_BUTTON_TEST_TAG)

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    @Test
    fun unarchiveMenuItem_forwardsClick() {
        var clicks = 0

        setContent(
            isUnarchiveVisible = true,
            onUnarchiveClick = { clicks += 1 },
        )

        openOverflowMenuAndClickItem(menuItemTestTag = CONVERSATION_UNARCHIVE_BUTTON_TEST_TAG)

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    @Test
    fun deleteConversationMenuItem_forwardsClick() {
        var clicks = 0

        setContent(
            isDeleteConversationVisible = true,
            onDeleteConversationClick = { clicks += 1 },
        )

        openOverflowMenuAndClickItem(
            menuItemTestTag = CONVERSATION_DELETE_CONVERSATION_BUTTON_TEST_TAG,
        )

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    private fun openOverflowMenuAndClickItem(menuItemTestTag: String) {
        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule
            .onNodeWithTag(testTag = menuItemTestTag)
            .performClick()
    }

    private fun setContent(
        metadata: ConversationMetadataUiState = presentMetadata,
        isCallVisible: Boolean = false,
        isAddPeopleVisible: Boolean = false,
        isArchiveVisible: Boolean = false,
        isUnarchiveVisible: Boolean = false,
        isAddContactVisible: Boolean = false,
        isDeleteConversationVisible: Boolean = false,
        isShowSubjectFieldVisible: Boolean = false,
        onCallClick: () -> Unit = {},
        onAddPeopleClick: () -> Unit = {},
        onArchiveClick: () -> Unit = {},
        onUnarchiveClick: () -> Unit = {},
        onAddContactClick: () -> Unit = {},
        onDeleteConversationClick: () -> Unit = {},
        onShowSubjectFieldClick: () -> Unit = {},
        onTitleClick: () -> Unit = {},
        onNavigateBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationTopAppBar(
                    metadata = metadata,
                    isCallVisible = isCallVisible,
                    isAddPeopleVisible = isAddPeopleVisible,
                    isArchiveVisible = isArchiveVisible,
                    isUnarchiveVisible = isUnarchiveVisible,
                    isAddContactVisible = isAddContactVisible,
                    isDeleteConversationVisible = isDeleteConversationVisible,
                    isShowSubjectFieldVisible = isShowSubjectFieldVisible,
                    onCallClick = onCallClick,
                    onAddPeopleClick = onAddPeopleClick,
                    onArchiveClick = onArchiveClick,
                    onUnarchiveClick = onUnarchiveClick,
                    onAddContactClick = onAddContactClick,
                    onDeleteConversationClick = onDeleteConversationClick,
                    onShowSubjectFieldClick = onShowSubjectFieldClick,
                    onTitleClick = onTitleClick,
                    onNavigateBack = onNavigateBack,
                )
            }
        }
    }

    private companion object {
        private val presentMetadata = ConversationMetadataUiState.Present(
            title = "Carol",
            selfParticipantId = "self-participant-id",
            avatar = ConversationMetadataUiState.Avatar.Single(photoUri = null),
            participantCount = 1,
            otherParticipantDisplayDestination = "+372 5440 0024",
            otherParticipantPhoneNumber = "+37254400024",
            otherParticipantContactLookupKey = null,
            isArchived = false,
            composerAvailability = ConversationComposerAvailability.Editable,
        )
    }
}
