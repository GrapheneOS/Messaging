package com.android.messaging.ui.conversation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.messaging.testutil.TEST_CALL_ACTION_PHONE_NUMBER
import com.android.messaging.ui.conversation.CONVERSATION_ADD_CONTACT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ARCHIVE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_CALL_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_DELETE_CONVERSATION_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SHOW_SUBJECT_FIELD_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_UNARCHIVE_BUTTON_TEST_TAG
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenTopAppBarActionsTest : BaseConversationScreenTest() {

    @Test
    fun addPeopleAction_isShownInOverflowWhenEnabled_andForwardsClicks() {
        val screenModel = createScreenModel()
        var addPeopleClicks = 0
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
            canAddPeople = true,
        )

        setContent(
            screenModel = screenModel.model,
            onAddPeopleClick = {
                addPeopleClicks += 1
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithTag(CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, addPeopleClicks)
        }
    }

    @Test
    fun callAction_isShownWhenEnabled_andForwardsClicks() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
            canCall = true,
            otherParticipantPhoneNumber = TEST_CALL_ACTION_PHONE_NUMBER,
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_CALL_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onCallClick()
            }
        }
    }

    @Test
    fun callAction_isHiddenWhenDisabled() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
            canCall = false,
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_CALL_BUTTON_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun overflowActions_whenConversationIsUnarchivedForwardScreenModelClicks() {
        val screenModel = createScreenModel()
        setTopBarActionContent(
            screenModel = screenModel,
            canArchive = true,
            canAddContact = true,
            canDeleteConversation = true,
            canEditSubject = true,
        )

        openOverflowMenuAndClickItem(menuItemTestTag = CONVERSATION_ARCHIVE_BUTTON_TEST_TAG)
        openOverflowMenuAndClickItem(menuItemTestTag = CONVERSATION_ADD_CONTACT_BUTTON_TEST_TAG)
        openOverflowMenuAndClickItem(
            menuItemTestTag = CONVERSATION_SHOW_SUBJECT_FIELD_MENU_ITEM_TEST_TAG,
        )
        openOverflowMenuAndClickItem(
            menuItemTestTag = CONVERSATION_DELETE_CONVERSATION_BUTTON_TEST_TAG,
        )

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onArchiveConversationClick()
            }
            verify(exactly = 1) {
                screenModel.model.onAddContactClick()
            }
            verify(exactly = 1) {
                screenModel.model.onShowSubjectFieldClick()
            }
            verify(exactly = 1) {
                screenModel.model.onDeleteConversationClick()
            }
        }
    }

    @Test
    fun overflowActions_whenConversationIsArchivedForwardsUnarchiveClick() {
        val screenModel = createScreenModel()
        setTopBarActionContent(
            screenModel = screenModel,
            canUnarchive = true,
            isArchived = true,
        )

        openOverflowMenuAndClickItem(menuItemTestTag = CONVERSATION_UNARCHIVE_BUTTON_TEST_TAG)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onUnarchiveConversationClick()
            }
        }
    }

    private fun setTopBarActionContent(
        screenModel: ScreenModelHandle,
        canArchive: Boolean = false,
        canUnarchive: Boolean = false,
        canAddContact: Boolean = false,
        canDeleteConversation: Boolean = false,
        canEditSubject: Boolean = false,
        isArchived: Boolean = false,
    ) {
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
            canArchive = canArchive,
            canUnarchive = canUnarchive,
            canAddContact = canAddContact,
            canDeleteConversation = canDeleteConversation,
            isArchived = isArchived,
        ).copy(canEditSubject = canEditSubject)

        setContent(screenModel = screenModel.model)
    }

    private fun openOverflowMenuAndClickItem(menuItemTestTag: String) {
        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithTag(menuItemTestTag)
            .assertIsDisplayed()
            .performClick()
    }
}
