package com.android.messaging.data.vcarddetail.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface VCardFieldAction {

    @Immutable
    data object None : VCardFieldAction

    @Immutable
    data class Dial(
        val number: String,
    ) : VCardFieldAction

    @Immutable
    data class Email(
        val address: String,
    ) : VCardFieldAction

    @Immutable
    data class OpenMap(
        val query: String,
    ) : VCardFieldAction

    @Immutable
    data class OpenUrl(
        val url: String,
    ) : VCardFieldAction
}
