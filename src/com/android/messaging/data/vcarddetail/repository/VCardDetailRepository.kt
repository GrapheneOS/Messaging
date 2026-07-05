package com.android.messaging.data.vcarddetail.repository

import com.android.messaging.data.vcard.repository.VCardEntryRepository
import com.android.messaging.data.vcarddetail.mapper.VCardDetailMapper
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal interface VCardDetailRepository {
    fun observeVCard(vCardUri: String): Flow<VCardDetailResult>
}

internal class VCardDetailRepositoryImpl @Inject constructor(
    private val vCardEntryRepository: VCardEntryRepository,
    private val vCardDetailMapper: VCardDetailMapper,
) : VCardDetailRepository {

    override fun observeVCard(vCardUri: String): Flow<VCardDetailResult> {
        if (vCardUri.isBlank()) {
            return flowOf(VCardDetailResult.Failed)
        }

        return flow {
            emit(VCardDetailResult.Loading)

            val entries = vCardEntryRepository.getEntries(vCardUri)
            val contacts = vCardDetailMapper.map(entries)
            val result = when {
                contacts.isEmpty() -> VCardDetailResult.Failed
                else -> VCardDetailResult.Loaded(contacts)
            }

            emit(result)
        }
    }
}
