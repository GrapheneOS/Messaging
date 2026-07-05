package com.android.messaging.data.vcarddetail.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.vcard.model.VCardAvatarPhoto
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class VCardContact(
    val displayName: String?,
    val avatarPhoto: VCardAvatarPhoto?,
    val fields: ImmutableList<VCardField>,
)
