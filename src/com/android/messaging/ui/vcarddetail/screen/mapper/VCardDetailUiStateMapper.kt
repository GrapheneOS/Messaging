package com.android.messaging.ui.vcarddetail.screen.mapper

import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailUiState
import javax.inject.Inject

internal interface VCardDetailUiStateMapper {
    fun map(result: VCardDetailResult): VCardDetailUiState
}

internal class VCardDetailUiStateMapperImpl @Inject constructor() : VCardDetailUiStateMapper {

    override fun map(result: VCardDetailResult): VCardDetailUiState {
        return when (result) {
            VCardDetailResult.Loading -> {
                VCardDetailUiState(isLoading = true)
            }

            VCardDetailResult.Failed -> {
                VCardDetailUiState(isLoading = false)
            }

            is VCardDetailResult.Loaded -> {
                VCardDetailUiState(
                    isLoading = false,
                    contacts = result.contacts,
                    canAddToContacts = result.contacts.isNotEmpty(),
                )
            }
        }
    }
}
