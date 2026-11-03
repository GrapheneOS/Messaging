package com.android.messaging.data.appsettings.model

internal enum class LinkOpeningMode(
    val prefValue: String,
) {
    AskBeforeOpening(prefValue = "ask"),
    OpenOnTap(prefValue = "open"),
    DoNotOpen(prefValue = "off"),
}
