package com.android.messaging.ui.conversationpicker.formatter

import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import javax.inject.Inject

internal class TargetTextFormatter @Inject constructor() {

    fun wrap(text: String): String {
        return BidiFormatter.getInstance().unicodeWrap(text, TextDirectionHeuristicsCompat.LTR)
    }

    fun detailsOrNull(
        name: String,
        value: String?,
    ): String? {
        return value
            ?.takeIf {
                it.isNotEmpty() && it != name
            }
            ?.let(::wrap)
    }
}
