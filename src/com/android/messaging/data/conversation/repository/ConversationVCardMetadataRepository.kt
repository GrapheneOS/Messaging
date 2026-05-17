package com.android.messaging.data.conversation.repository

import android.content.Context
import androidx.core.net.toUri
import com.android.messaging.data.conversation.mapper.ConversationVCardMetadataMapper
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
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

internal interface ConversationVCardMetadataRepository {
    fun observeAttachmentMetadata(
        contentUri: String?,
    ): Flow<ConversationVCardAttachmentMetadata>
}

internal class ConversationVCardMetadataRepositoryImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val conversationVCardMetadataMapper: ConversationVCardMetadataMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationVCardMetadataRepository {

    private val dataModel = DataModel.get()

    override fun observeAttachmentMetadata(
        contentUri: String?,
    ): Flow<ConversationVCardAttachmentMetadata> {
        if (contentUri.isNullOrBlank()) {
            return flowOf(ConversationVCardAttachmentMetadata.Missing)
        }

        return callbackFlow {
            trySend(ConversationVCardAttachmentMetadata.Loading)

            val vCardData = dataModel.createVCardContactItemData(
                context,
                contentUri.toUri(),
            )
            val bindingId = "conversation-vcard-inline:$contentUri"
            val listener = object : PersonItemData.PersonItemDataListener {
                override fun onPersonDataUpdated(data: PersonItemData) {
                    val typedData = data as? VCardContactItemData ?: return
                    trySend(
                        conversationVCardMetadataMapper.map(
                            vCardContactItemData = typedData,
                        ),
                    )
                }

                override fun onPersonDataFailed(
                    data: PersonItemData,
                    exception: Exception,
                ) {
                    trySend(ConversationVCardAttachmentMetadata.Failed)
                }
            }

            vCardData.bind(bindingId)
            vCardData.setListener(listener)

            awaitClose {
                vCardData.unbind(bindingId)
            }
        }.flowOn(defaultDispatcher)
    }
}
