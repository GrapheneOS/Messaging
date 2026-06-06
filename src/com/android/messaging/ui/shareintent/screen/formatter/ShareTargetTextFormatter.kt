package com.android.messaging.ui.shareintent.screen.formatter

import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import javax.inject.Inject

internal class ShareTargetTextFormatter @Inject constructor() {

    fun wrap(text: String): String {
        return BidiFormatter.getInstance().unicodeWrap(text, TextDirectionHeuristicsCompat.LTR)
    }

    fun detailsOrNull(
        name: String,
        value: String?,
    ): String? {
        val details = value?.takeIf {
            it.isNotEmpty() && it != name
        } ?: return null

        return wrap(details)
    }
}
