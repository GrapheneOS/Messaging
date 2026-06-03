package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.messaging.ui.common.components.composer.MessageSubjectChip
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_CHIP_CLEAR_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_CHIP_TEST_TAG

internal fun conversationComposeSubjectSlot(
    subjectText: String,
    onSubjectChipClick: () -> Unit,
    onSubjectChipClear: () -> Unit,
): (@Composable ColumnScope.() -> Unit)? {
    if (subjectText.isBlank()) {
        return null
    }

    return {
        MessageSubjectChip(
            modifier = Modifier.testTag(tag = CONVERSATION_SUBJECT_CHIP_TEST_TAG),
            subjectText = subjectText,
            onClick = onSubjectChipClick,
            onClear = onSubjectChipClear,
            clearButtonTestTag = CONVERSATION_SUBJECT_CHIP_CLEAR_BUTTON_TEST_TAG,
        )
    }
}
