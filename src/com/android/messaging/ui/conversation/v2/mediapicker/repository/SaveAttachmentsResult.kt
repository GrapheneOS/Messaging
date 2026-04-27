package com.android.messaging.ui.conversation.v2.mediapicker.repository

internal data class SaveAttachmentsResult(
    val imageCount: Int,
    val videoCount: Int,
    val otherCount: Int,
    val failCount: Int,
)
