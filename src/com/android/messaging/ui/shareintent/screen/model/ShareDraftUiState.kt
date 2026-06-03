package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ShareDraftUiState(
    val isLoading: Boolean = true,
    val isReviewing: Boolean = false,
    val text: String = "",
    val subjectText: String = "",
    val attachments: ImmutableList<ShareAttachmentUiModel> = persistentListOf(),
)
