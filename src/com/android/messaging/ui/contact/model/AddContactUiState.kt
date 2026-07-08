package com.android.messaging.ui.contact.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class AddContactUiState(
    val avatarUri: String?,
    val destination: String,
    val vocalizedDestination: String,
)
