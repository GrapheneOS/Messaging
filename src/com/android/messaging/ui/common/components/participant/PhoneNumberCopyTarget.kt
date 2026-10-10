package com.android.messaging.ui.common.components.participant

import androidx.compose.runtime.Immutable

@Immutable
internal data class PhoneNumberCopyTarget(
    val displayName: String,
    val phoneNumber: String,
)
