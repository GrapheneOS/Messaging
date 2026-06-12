package com.android.messaging.ui.common.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

internal fun Modifier.optionalTestTag(tag: String?): Modifier {
    return when {
        tag != null -> then(Modifier.testTag(tag = tag))
        else -> this
    }
}
