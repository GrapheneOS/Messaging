package com.android.messaging.data.vcarddetail.repository

import com.android.messaging.data.vcard.repository.VCardEntryRepository
import com.android.messaging.data.vcarddetail.mapper.VCardDetailMapper
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import com.android.messaging.datamodel.media.CustomVCardEntry
import com.android.messaging.di.core.DefaultDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface VCardDetailRepository {
    fun observeVCard(
        vCardUri: String,
        refreshes: Flow<Unit> = emptyFlow(),
    ): Flow<VCardDetailResult>
}

internal class VCardDetailRepositoryImpl @Inject constructor(
    private val vCardEntryRepository: VCardEntryRepository,
    private val vCardDetailMapper: VCardDetailMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : VCardDetailRepository {

    override fun observeVCard(
        vCardUri: String,
        refreshes: Flow<Unit>,
    ): Flow<VCardDetailResult> {
        if (vCardUri.isBlank()) {
            return flowOf(VCardDetailResult.Failed)
        }

        return flow {
            emit(VCardDetailResult.Loading)

            vCardEntryRepository
                .observeEntries(
                    vCardUri = vCardUri,
                    refreshes = refreshes,
                )
                .map(::mapResult)
                .collect(::emit)
        }.flowOn(defaultDispatcher)
    }

    private fun mapResult(entries: List<CustomVCardEntry>): VCardDetailResult {
        val contacts = vCardDetailMapper.map(entries)

        return when {
            contacts.isEmpty() -> VCardDetailResult.Failed
            else -> VCardDetailResult.Loaded(contacts)
        }
    }
}
