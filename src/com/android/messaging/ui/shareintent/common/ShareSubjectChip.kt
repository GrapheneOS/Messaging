package com.android.messaging.ui.shareintent.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.messaging.ui.common.components.composer.MessageSubjectChip

internal const val SHARE_SUBJECT_CHIP_TEST_TAG = "share_subject_chip"
internal const val SHARE_SUBJECT_CHIP_CLEAR_BUTTON_TEST_TAG = "share_subject_chip_clear_button"

internal fun shareComposeSubjectSlot(
    subjectText: String,
    onClear: () -> Unit,
): (@Composable ColumnScope.() -> Unit)? {
    if (subjectText.isBlank()) {
        return null
    }

    return {
        MessageSubjectChip(
            modifier = Modifier.testTag(tag = SHARE_SUBJECT_CHIP_TEST_TAG),
            subjectText = subjectText,
            onClick = null,
            onClear = onClear,
            clearButtonTestTag = SHARE_SUBJECT_CHIP_CLEAR_BUTTON_TEST_TAG,
        )
    }
}
