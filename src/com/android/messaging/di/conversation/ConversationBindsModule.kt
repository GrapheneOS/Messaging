package com.android.messaging.di.conversation

import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepositoryImpl
import com.android.messaging.ui.conversation.v2.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.v2.messages.mapper.ConversationMessageUiModelMapperImpl
import com.android.messaging.ui.conversation.v2.metadata.mapper.ConversationMetadataUiStateMapper
import com.android.messaging.ui.conversation.v2.metadata.mapper.ConversationMetadataUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConversationBindsModule {

    @Binds
    @Reusable
    abstract fun bindConversationsRepository(
        impl: ConversationsRepositoryImpl,
    ): ConversationsRepository

    @Binds
    abstract fun bindConversationMessageUiModelMapper(
        impl: ConversationMessageUiModelMapperImpl,
    ): ConversationMessageUiModelMapper

    @Binds
    abstract fun bindConversationMetadataUiStateMapper(
        impl: ConversationMetadataUiStateMapperImpl,
    ): ConversationMetadataUiStateMapper
}
