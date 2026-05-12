package com.android.messaging.data.contact.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ContactDestination(
    val dataId: Long,
    val contactId: Long,
    val value: String,
    val normalizedValue: String,
    val displayValue: String,
    val kind: Kind,
    val type: Int,
    val customLabel: String?,
    val isPrimary: Boolean,
    val isSuperPrimary: Boolean,
) {
    enum class Kind {
        PHONE,
        EMAIL,
    }
}
