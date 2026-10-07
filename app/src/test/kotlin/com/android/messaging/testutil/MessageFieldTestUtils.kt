package com.android.messaging.testutil

import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.view.descendants

/** Returns the platform text view behind the message field tagged [testTag]. */
internal fun ComposeContentTestRule.messageFieldView(testTag: String): EditText {
    val root = onNodeWithTag(testTag = testTag).fetchSemanticsNode().root as ViewRootForTest
    val composeView = root.view as ViewGroup

    return composeView.descendants.filterIsInstance<EditText>().single()
}

/** Taps the message field tagged [testTag] and returns it once it has the keyboard's focus. */
internal fun ComposeContentTestRule.focusMessageField(testTag: String): EditText {
    val field = messageFieldView(testTag = testTag)

    onNodeWithTag(testTag = testTag).performClick()
    waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) { field.isFocused }

    return field
}

/** Types [text] into the message field tagged [testTag] the way a keyboard commits it. */
internal fun ComposeContentTestRule.typeIntoMessageField(testTag: String, text: String) {
    focusMessageField(testTag = testTag)
        .onCreateInputConnection(EditorInfo())
        .commitText(text, 1)
    waitForIdle()
}
