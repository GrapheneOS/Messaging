package com.android.messaging.ui.vcarddetail.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data class VCardDetailNavKey(
    val uri: String,
) : NavKey
