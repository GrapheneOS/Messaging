package com.android.messaging.data.shareintent.model

internal sealed interface SharedTextContentResult {
    data class Read(
        val text: String,
    ) : SharedTextContentResult

    data object Empty : SharedTextContentResult

    data object Failed : SharedTextContentResult
}
