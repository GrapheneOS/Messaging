package com.android.messaging.data.contact.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ContactsPage(
    val contacts: ImmutableList<Contact>,
    val nextOffset: Int?,
)
