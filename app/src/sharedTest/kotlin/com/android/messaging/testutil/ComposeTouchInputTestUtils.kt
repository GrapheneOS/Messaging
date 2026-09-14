package com.android.messaging.testutil

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput

internal fun SemanticsNodeInteraction.performDisabledTouchClick() {
    performCenterTouchClick()
}

internal fun SemanticsNodeInteraction.performTouchClick() {
    performCenterTouchClick()
}

private fun SemanticsNodeInteraction.performCenterTouchClick() {
    performTouchInput {
        click(position = center)
    }
}
