package com.android.messaging.ui.common.components.composer

import android.text.InputType
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.android.messaging.testutil.focusMessageField
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MessageComposeBarInputTypeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun messageComposeField_reportsSentenceCapitalizationToTheKeyboard() {
        val inputType = focusedFieldInputType()

        assertTrue(inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0)
    }

    @Test
    fun messageComposeField_reportsShortMessageVariationToTheKeyboard() {
        val inputType = focusedFieldInputType()

        assertEquals(
            InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
            inputType and InputType.TYPE_MASK_VARIATION,
        )
    }

    @Test
    fun messageComposeField_staysMultiLine() {
        val inputType = focusedFieldInputType()

        assertTrue(inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0)
    }

    @Test
    fun messageComposeField_withImeSend_offersSendWhileEnterStillAddsANewLine() {
        val imeOptions = focusedFieldEditorInfo(onImeSend = {}).imeOptions

        assertEquals(EditorInfo.IME_ACTION_SEND, imeOptions and EditorInfo.IME_MASK_ACTION)
        assertTrue(imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0)
    }

    @Test
    fun messageComposeField_withImeSend_letsAccessibilityServicesSend() {
        var sendCount = 0
        val field = focusedField(onImeSend = { sendCount++ })

        field.performAccessibilityAction(AccessibilityAction.ACTION_IME_ENTER.id, null)

        assertEquals(1, sendCount)
    }

    private fun focusedFieldInputType(): Int {
        return focusedFieldEditorInfo().inputType
    }

    private fun focusedFieldEditorInfo(onImeSend: (() -> Unit)? = null): EditorInfo {
        val editorInfo = EditorInfo()
        focusedField(onImeSend = onImeSend).onCreateInputConnection(editorInfo)

        return editorInfo
    }

    private fun focusedField(onImeSend: (() -> Unit)? = null): View {
        composeTestRule.setContent {
            AppTheme {
                MessageComposeBar(
                    text = "",
                    textRevision = 0,
                    onTextChange = { _, _ -> },
                    isFieldEnabled = true,
                    isFieldContentHidden = false,
                    fieldFocusRequester = null,
                    fieldStateDescription = null,
                    fieldTestTag = MESSAGE_COMPOSE_FIELD_TEST_TAG,
                    sendAction = {},
                    onImeSend = onImeSend,
                )
            }
        }

        return composeTestRule.focusMessageField(testTag = MESSAGE_COMPOSE_FIELD_TEST_TAG)
    }
}
