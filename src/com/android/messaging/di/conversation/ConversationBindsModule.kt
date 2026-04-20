package com.android.messaging.di.conversation

import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapper
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapperImpl
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapperImpl
import com.android.messaging.data.conversation.repository.ConversationDraftStore
import com.android.messaging.data.conversation.repository.ConversationDraftStoreImpl
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.data.conversation.repository.ConversationDraftsRepositoryImpl
import com.android.messaging.data.conversation.repository.ConversationMetadataNotifier
import com.android.messaging.data.conversation.repository.ConversationMetadataNotifierImpl
import com.android.messaging.data.conversation.repository.ConversationParticipantsRepository
import com.android.messaging.data.conversation.repository.ConversationParticipantsRepositoryImpl
import com.android.messaging.data.conversation.repository.ConversationRecipientsRepository
import com.android.messaging.data.conversation.repository.ConversationRecipientsRepositoryImpl
import com.android.messaging.data.conversation.repository.ConversationSubscriptionsRepository
import com.android.messaging.data.conversation.repository.ConversationSubscriptionsRepositoryImpl
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepositoryImpl
import com.android.messaging.data.media.repository.ConversationMediaRepository
import com.android.messaging.data.media.repository.ConversationMediaRepositoryImpl
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGrantedImpl
import com.android.messaging.domain.conversation.usecase.CanAddMoreConversationParticipants
import com.android.messaging.domain.conversation.usecase.CanAddMoreConversationParticipantsImpl
import com.android.messaging.domain.conversation.usecase.CreateForwardedMessage
import com.android.messaging.domain.conversation.usecase.CreateForwardedMessageImpl
import com.android.messaging.domain.conversation.usecase.ForwardedMessageSubjectFormatter
import com.android.messaging.domain.conversation.usecase.ForwardedMessageSubjectFormatterImpl
import com.android.messaging.domain.conversation.usecase.IsConversationRecipientLimitExceeded
import com.android.messaging.domain.conversation.usecase.IsConversationRecipientLimitExceededImpl
import com.android.messaging.domain.conversation.usecase.IsDeviceVoiceCapable
import com.android.messaging.domain.conversation.usecase.IsDeviceVoiceCapableImpl
import com.android.messaging.domain.conversation.usecase.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.ResolveConversationIdImpl
import com.android.messaging.domain.conversation.usecase.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.SendConversationDraftImpl
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapperImpl
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationAttachmentBridge
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationAttachmentBridgeImpl
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
    abstract fun bindConversationDraftMessageDataMapper(
        impl: ConversationDraftMessageDataMapperImpl,
    ): ConversationDraftMessageDataMapper

    @Binds
    @Reusable
    abstract fun bindConversationMessageDataDraftMapper(
        impl: ConversationMessageDataDraftMapperImpl,
    ): ConversationMessageDataDraftMapper

    @Binds
    @Reusable
    abstract fun bindConversationDraftStore(
        impl: ConversationDraftStoreImpl,
    ): ConversationDraftStore

    @Binds
    @Reusable
    abstract fun bindConversationMetadataNotifier(
        impl: ConversationMetadataNotifierImpl,
    ): ConversationMetadataNotifier

    @Binds
    @Reusable
    abstract fun bindConversationDraftsRepository(
        impl: ConversationDraftsRepositoryImpl,
    ): ConversationDraftsRepository

    @Binds
    @Reusable
    abstract fun bindConversationParticipantsRepository(
        impl: ConversationParticipantsRepositoryImpl,
    ): ConversationParticipantsRepository

    @Binds
    @Reusable
    abstract fun bindConversationRecipientsRepository(
        impl: ConversationRecipientsRepositoryImpl,
    ): ConversationRecipientsRepository

    @Binds
    @Reusable
    abstract fun bindCanAddMoreConversationParticipants(
        impl: CanAddMoreConversationParticipantsImpl,
    ): CanAddMoreConversationParticipants

    @Binds
    @Reusable
    abstract fun bindIsDeviceVoiceCapable(
        impl: IsDeviceVoiceCapableImpl,
    ): IsDeviceVoiceCapable

    @Binds
    @Reusable
    abstract fun bindCreateForwardedMessage(
        impl: CreateForwardedMessageImpl,
    ): CreateForwardedMessage

    @Binds
    @Reusable
    abstract fun bindIsReadContactsPermissionGranted(
        impl: IsReadContactsPermissionGrantedImpl,
    ): IsReadContactsPermissionGranted

    @Binds
    @Reusable
    abstract fun bindForwardedMessageSubjectFormatter(
        impl: ForwardedMessageSubjectFormatterImpl,
    ): ForwardedMessageSubjectFormatter

    @Binds
    @Reusable
    abstract fun bindResolveConversationId(
        impl: ResolveConversationIdImpl,
    ): ResolveConversationId

    @Binds
    @Reusable
    abstract fun bindIsConversationRecipientLimitExceeded(
        impl: IsConversationRecipientLimitExceededImpl,
    ): IsConversationRecipientLimitExceeded

    @Binds
    @Reusable
    abstract fun bindConversationsRepository(
        impl: ConversationsRepositoryImpl,
    ): ConversationsRepository

    @Binds
    @Reusable
    abstract fun bindConversationSubscriptionsRepository(
        impl: ConversationSubscriptionsRepositoryImpl,
    ): ConversationSubscriptionsRepository

    @Binds
    abstract fun bindConversationAttachmentBridge(
        impl: ConversationAttachmentBridgeImpl,
    ): ConversationAttachmentBridge

    @Binds
    abstract fun bindConversationComposerUiStateMapper(
        impl: ConversationComposerUiStateMapperImpl,
    ): ConversationComposerUiStateMapper

    @Binds
    abstract fun bindConversationMessageUiModelMapper(
        impl: ConversationMessageUiModelMapperImpl,
    ): ConversationMessageUiModelMapper

    @Binds
    @Reusable
    abstract fun bindConversationMediaRepository(
        impl: ConversationMediaRepositoryImpl,
    ): ConversationMediaRepository

    @Binds
    abstract fun bindConversationMetadataUiStateMapper(
        impl: ConversationMetadataUiStateMapperImpl,
    ): ConversationMetadataUiStateMapper

    @Binds
    @Reusable
    abstract fun bindSendConversationDraft(
        impl: SendConversationDraftImpl,
    ): SendConversationDraft
}
