package com.android.messaging.ui.smsstoragelow.model

internal sealed interface SmsStorageLowWarningScreenEffect {
    data object Finish : SmsStorageLowWarningScreenEffect
}
