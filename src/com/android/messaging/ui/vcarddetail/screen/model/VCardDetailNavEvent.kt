package com.android.messaging.ui.vcarddetail.screen.model

internal sealed interface VCardDetailNavEvent {
    data object Close : VCardDetailNavEvent
}
