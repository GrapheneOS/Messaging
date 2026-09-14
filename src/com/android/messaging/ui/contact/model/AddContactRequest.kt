package com.android.messaging.ui.contact.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
internal data class AddContactRequest(
    val destination: String,
    val avatarUri: String?,
)
