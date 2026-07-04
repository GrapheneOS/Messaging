package com.android.messaging.data.vcarddetail.repository

import android.content.Context
import androidx.core.net.toUri
import com.android.messaging.data.vcarddetail.mapper.VCardDetailMapper
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.data.PersonItemData
import com.android.messaging.datamodel.data.VCardContactItemData
import com.android.messaging.di.core.DefaultDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

internal interface VCardDetailRepository {
    fun observeVCard(vCardUri: String): Flow<VCardDetailResult>
}

internal class VCardDetailRepositoryImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val vCardDetailMapper: VCardDetailMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : VCardDetailRepository {

    private val dataModel = DataModel.get()

    override fun observeVCard(vCardUri: String): Flow<VCardDetailResult> {
        if (vCardUri.isBlank()) {
            return flowOf(VCardDetailResult.Failed)
        }

        return callbackFlow {
            trySend(VCardDetailResult.Loading)

            val vCardData = dataModel.createVCardContactItemData(
                context,
                vCardUri.toUri(),
            )
            val bindingId = "$BINDING_ID_PREFIX:$vCardUri"
            val listener = object : PersonItemData.PersonItemDataListener {
                override fun onPersonDataUpdated(data: PersonItemData) {
                    val typedData = data as? VCardContactItemData ?: return
                    trySend(
                        VCardDetailResult.Loaded(
                            contacts = vCardDetailMapper.map(typedData),
                            displayName = typedData.displayName,
                        ),
                    )
                }

                override fun onPersonDataFailed(
                    data: PersonItemData,
                    exception: Exception,
                ) {
                    trySend(VCardDetailResult.Failed)
                }
            }

            vCardData.bind(bindingId)
            vCardData.setListener(listener)

            awaitClose {
                vCardData.unbind(bindingId)
            }
        }.flowOn(defaultDispatcher)
    }

    private companion object {
        private const val BINDING_ID_PREFIX = "vcard-detail"
    }
}
