package com.android.messaging.data.appsettings.model

internal data class AppSettings(
    val isDefaultSmsApp: Boolean,
    val defaultSmsAppLabel: String,
    val sendSoundEnabled: Boolean,
    val youTubeLinkPreviewsEnabled: Boolean,
    val isDebugEnabled: Boolean,
    val dumpSmsEnabled: Boolean,
    val dumpMmsEnabled: Boolean,
)
