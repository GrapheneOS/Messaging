package com.android.messaging.ui.vcarddetail.screen.mapper

import com.android.messaging.data.vcarddetail.model.VCardContact
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import com.android.messaging.data.vcarddetail.model.VCardField
import com.android.messaging.ui.vcarddetail.screen.model.VCardContactUiModel
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailUiState
import com.android.messaging.ui.vcarddetail.screen.model.VCardFieldUiModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

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
                    contacts = mapContacts(result.contacts),
                    canAddToContacts = result.contacts.isNotEmpty(),
                )
            }
        }
    }

    private fun mapContacts(
        contacts: ImmutableList<VCardContact>,
    ): ImmutableList<VCardContactUiModel> {
        return contacts
            .map(::mapContact)
            .toImmutableList()
    }

    private fun mapContact(contact: VCardContact): VCardContactUiModel {
        return VCardContactUiModel(
            displayName = contact.displayName,
            normalizedDestination = contact.normalizedDestination,
            avatarPhoto = contact.avatarPhoto,
            fields = contact.fields
                .map(::mapField)
                .toImmutableList(),
        )
    }

    private fun mapField(field: VCardField): VCardFieldUiModel {
        return VCardFieldUiModel(
            value = field.value,
            label = field.label,
            action = field.action,
        )
    }
}
