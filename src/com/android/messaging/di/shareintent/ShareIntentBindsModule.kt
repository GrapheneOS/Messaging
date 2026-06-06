package com.android.messaging.di.shareintent

import com.android.messaging.data.shareintent.repository.ShareTargetsRepository
import com.android.messaging.data.shareintent.repository.ShareTargetsRepositoryImpl
import com.android.messaging.data.shareintent.repository.SharedAttachmentRepository
import com.android.messaging.data.shareintent.repository.SharedAttachmentRepositoryImpl
import com.android.messaging.domain.shareintent.usecase.BuildSharedConversationDraft
import com.android.messaging.domain.shareintent.usecase.BuildSharedConversationDraftImpl
import com.android.messaging.domain.shareintent.usecase.ResolveShareTargetsToConversationIds
import com.android.messaging.domain.shareintent.usecase.ResolveShareTargetsToConversationIdsImpl
import com.android.messaging.domain.shareintent.usecase.ResolveSharedContentType
import com.android.messaging.domain.shareintent.usecase.ResolveSharedContentTypeImpl
import com.android.messaging.domain.shareintent.usecase.SendSharedContentToConversations
import com.android.messaging.domain.shareintent.usecase.SendSharedContentToConversationsImpl
import com.android.messaging.domain.shareintent.usecase.SendSharedContentToTargets
import com.android.messaging.domain.shareintent.usecase.SendSharedContentToTargetsImpl
import com.android.messaging.ui.shareintent.screen.mapper.ShareContactSectionMapper
import com.android.messaging.ui.shareintent.screen.mapper.ShareContactSectionMapperImpl
import com.android.messaging.ui.shareintent.screen.mapper.ShareContactUiStateMapper
import com.android.messaging.ui.shareintent.screen.mapper.ShareContactUiStateMapperImpl
import com.android.messaging.ui.shareintent.screen.mapper.ShareTargetUiStateMapper
import com.android.messaging.ui.shareintent.screen.mapper.ShareTargetUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ShareIntentBindsModule {

    @Binds
    @Reusable
    abstract fun bindShareTargetUiStateMapper(
        impl: ShareTargetUiStateMapperImpl,
    ): ShareTargetUiStateMapper

    @Binds
    @Reusable
    abstract fun bindShareContactUiStateMapper(
        impl: ShareContactUiStateMapperImpl,
    ): ShareContactUiStateMapper

    @Binds
    @Reusable
    abstract fun bindShareContactSectionMapper(
        impl: ShareContactSectionMapperImpl,
    ): ShareContactSectionMapper

    @Binds
    @Reusable
    abstract fun bindShareTargetsRepository(
        impl: ShareTargetsRepositoryImpl,
    ): ShareTargetsRepository

    @Binds
    @Reusable
    abstract fun bindSharedAttachmentRepository(
        impl: SharedAttachmentRepositoryImpl,
    ): SharedAttachmentRepository

    @Binds
    @Reusable
    abstract fun bindResolveSharedContentType(
        impl: ResolveSharedContentTypeImpl,
    ): ResolveSharedContentType

    @Binds
    @Reusable
    abstract fun bindBuildSharedConversationDraft(
        impl: BuildSharedConversationDraftImpl,
    ): BuildSharedConversationDraft

    @Binds
    @Reusable
    abstract fun bindSendSharedContentToConversations(
        impl: SendSharedContentToConversationsImpl,
    ): SendSharedContentToConversations

    @Binds
    @Reusable
    abstract fun bindResolveShareTargetsToConversationIds(
        impl: ResolveShareTargetsToConversationIdsImpl,
    ): ResolveShareTargetsToConversationIds

    @Binds
    @Reusable
    abstract fun bindSendSharedContentToTargets(
        impl: SendSharedContentToTargetsImpl,
    ): SendSharedContentToTargets
}
