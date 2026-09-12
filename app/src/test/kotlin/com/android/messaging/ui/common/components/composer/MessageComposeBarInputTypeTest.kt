package com.android.messaging.ui.common.components.composer

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.core.AppTheme
import kotlinx.coroutines.awaitCancellation
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

    private fun focusedFieldInputType(): Int {
        var editorInfo: EditorInfo? = null

        composeTestRule.setContent {
            RecordingEditorInfo(onEditorInfo = { editorInfo = it }) {
                AppTheme {
                    MessageComposeBar(
                        text = "",
                        onTextChange = {},
                        isFieldEnabled = true,
                        isFieldContentHidden = false,
                        fieldFocusRequester = null,
                        fieldStateDescription = null,
                        fieldTestTag = MESSAGE_COMPOSE_FIELD_TEST_TAG,
                        sendAction = {},
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(testTag = MESSAGE_COMPOSE_FIELD_TEST_TAG)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            editorInfo != null
        }

        return requireNotNull(editorInfo).inputType
    }
}

/**
 * Captures the [EditorInfo] the focused text field below would hand to the keyboard, instead of
 * letting the request reach the real input method.
 */
@Composable
private fun RecordingEditorInfo(
    onEditorInfo: (EditorInfo) -> Unit,
    content: @Composable () -> Unit,
) {
    InterceptPlatformTextInput(
        interceptor = { request, _ ->
            val editorInfo = EditorInfo()
            request.createInputConnection(outAttributes = editorInfo)
            onEditorInfo(editorInfo)
            awaitCancellation()
        },
        content = content,
    )
}
