package com.android.messaging.di.conversationpicker

import com.android.messaging.data.conversationpicker.repository.TargetsRepository
import com.android.messaging.data.conversationpicker.repository.TargetsRepositoryImpl
import com.android.messaging.domain.conversationpicker.usecase.BuildConversationDraftFromMessage
import com.android.messaging.domain.conversationpicker.usecase.BuildConversationDraftFromMessageImpl
import com.android.messaging.domain.conversationpicker.usecase.BuildMessageDataFromDraft
import com.android.messaging.domain.conversationpicker.usecase.BuildMessageDataFromDraftImpl
import com.android.messaging.domain.conversationpicker.usecase.ResolveTargetsToConversationIds
import com.android.messaging.domain.conversationpicker.usecase.ResolveTargetsToConversationIdsImpl
import com.android.messaging.domain.conversationpicker.usecase.SendContentToConversations
import com.android.messaging.domain.conversationpicker.usecase.SendContentToConversationsImpl
import com.android.messaging.domain.conversationpicker.usecase.SendContentToTargets
import com.android.messaging.domain.conversationpicker.usecase.SendContentToTargetsImpl
import com.android.messaging.ui.conversationpicker.mapper.ContactTargetMapper
import com.android.messaging.ui.conversationpicker.mapper.ContactTargetMapperImpl
import com.android.messaging.ui.conversationpicker.mapper.TargetUiStateMapper
import com.android.messaging.ui.conversationpicker.mapper.TargetUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConversationPickerBindsModule {

    @Binds
    @Reusable
    abstract fun bindTargetUiStateMapper(
        impl: TargetUiStateMapperImpl,
    ): TargetUiStateMapper

    @Binds
    @Reusable
    abstract fun bindContactTargetMapper(
        impl: ContactTargetMapperImpl,
    ): ContactTargetMapper

    @Binds
    @Reusable
    abstract fun bindTargetsRepository(
        impl: TargetsRepositoryImpl,
    ): TargetsRepository

    @Binds
    @Reusable
    abstract fun bindBuildConversationDraftFromMessage(
        impl: BuildConversationDraftFromMessageImpl,
    ): BuildConversationDraftFromMessage

    @Binds
    @Reusable
    abstract fun bindBuildMessageDataFromDraft(
        impl: BuildMessageDataFromDraftImpl,
    ): BuildMessageDataFromDraft

    @Binds
    @Reusable
    abstract fun bindSendContentToConversations(
        impl: SendContentToConversationsImpl,
    ): SendContentToConversations

    @Binds
    @Reusable
    abstract fun bindResolveTargetsToConversationIds(
        impl: ResolveTargetsToConversationIdsImpl,
    ): ResolveTargetsToConversationIds

    @Binds
    @Reusable
    abstract fun bindSendContentToTargets(
        impl: SendContentToTargetsImpl,
    ): SendContentToTargets
}
