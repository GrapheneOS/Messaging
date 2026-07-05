package com.android.messaging.data.vcarddetail.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface VCardDetailResult {

    @Immutable
    data object Loading : VCardDetailResult

    @Immutable
    data object Failed : VCardDetailResult

    @Immutable
    data class Loaded(
        val contacts: ImmutableList<VCardContact>,
    ) : VCardDetailResult
}
