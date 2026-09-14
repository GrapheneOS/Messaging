package com.android.messaging.ui.conversationpicker.formatter

internal fun targetDetailsTextOrNull(
    displayName: String,
    value: String?,
): String? {
    return value
        ?.takeIf {
            it.isNotEmpty() && it != displayName
        }
}
