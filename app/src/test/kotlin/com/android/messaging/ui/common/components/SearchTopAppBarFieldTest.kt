package com.android.messaging.ui.common.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SearchTopAppBarFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyField_showsTheHintAndTakesFocus() {
        setContent(state = TextFieldState())

        composeTestRule.onNodeWithText(hint()).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FIELD_TEST_TAG).assertIsFocused()
    }

    @Test
    fun typing_updatesTheStateAndHidesTheHint() {
        val state = TextFieldState()
        setContent(state = state)

        composeTestRule.onNodeWithTag(FIELD_TEST_TAG).performTextInput(QUERY)

        assertEquals(QUERY, state.text.toString())
        composeTestRule.onNodeWithText(hint()).assertDoesNotExist()
    }

    private fun setContent(state: TextFieldState) {
        composeTestRule.setContent {
            AppTheme {
                SearchTopAppBarField(
                    state = state,
                    hint = R.string.conversation_list_search_hint,
                    modifier = Modifier.testTag(FIELD_TEST_TAG),
                )
            }
        }
    }

    private fun hint(): String {
        return targetContext.getString(R.string.conversation_list_search_hint)
    }

    private companion object {
        private const val FIELD_TEST_TAG = "search_field"
        private const val QUERY = "anna"
    }
}
