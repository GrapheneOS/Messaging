package com.android.messaging.ui.recipientselection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.navigation.RecipientPickerMode
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecipientPickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_createGroupMode_showsCreateGroupTitle() {
        setContent(mode = RecipientPickerMode.CREATE_GROUP)

        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.conversation_new_group))
            .assertIsDisplayed()
    }

    @Test
    fun screen_addParticipantsMode_showsAddPeopleTitle() {
        setContent(mode = RecipientPickerMode.ADD_PARTICIPANTS)

        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.conversation_add_people))
            .assertIsDisplayed()
    }

    private fun setContent(mode: RecipientPickerMode) {
        composeTestRule.setContent {
            AppTheme {
                RecipientPickerScreen(mode = mode)
            }
        }
    }
}
