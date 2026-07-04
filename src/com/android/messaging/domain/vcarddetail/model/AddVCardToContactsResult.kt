package com.android.messaging.domain.vcarddetail.model

internal sealed interface AddVCardToContactsResult {

    data class Prepared(
        val scratchUri: String,
    ) : AddVCardToContactsResult

    data object Failed : AddVCardToContactsResult
}
