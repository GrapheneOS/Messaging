package com.android.messaging.ui.vcarddetail.screen.model

import com.android.messaging.data.vcarddetail.model.VCardFieldAction

internal sealed interface VCardDetailScreenEffect {

    data class OpenFieldAction(
        val action: VCardFieldAction,
    ) : VCardDetailScreenEffect

    data class LaunchSaveToContacts(
        val scratchUri: String,
    ) : VCardDetailScreenEffect

    data class CopyToClipboard(
        val text: String,
    ) : VCardDetailScreenEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : VCardDetailScreenEffect

    data object Close : VCardDetailScreenEffect
}
