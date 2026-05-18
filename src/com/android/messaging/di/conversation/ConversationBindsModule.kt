package com.android.messaging.di.conversation

import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.contact.formatter.ContactDestinationFormatterImpl
import com.android.messaging.data.contact.repository.ContactsRepository
import com.android.messaging.data.contact.repository.ContactsRepositoryImpl
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapper
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapperImpl
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapperImpl
import com.android.messaging.data.conversation.mapper.ConversationVCardMetadataMapper
import com.android.messaging.data.conversation.mapper.ConversationVCardMetadataMapperImpl
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.data.conversation.repository.ConversationDraftsRepositoryImpl
import com.android.messaging.data.conversation.repository.ConversationParticipantsRepository
import com.android.messaging.data.conversation.repository.ConversationParticipantsRepositoryImpl
import com.android.messaging.data.conversation.repository.ConversationVCardMetadataRepository
import com.android.messaging.data.conversation.repository.ConversationVCardMetadataRepositoryImpl
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepositoryImpl
import com.android.messaging.data.conversation.store.ConversationDraftStore
import com.android.messaging.data.conversation.store.ConversationDraftStoreImpl
import com.android.messaging.data.media.repository.ConversationAttachmentsRepository
import com.android.messaging.data.media.repository.ConversationAttachmentsRepositoryImpl
import com.android.messaging.data.media.repository.ConversationMediaRepository
import com.android.messaging.data.media.repository.ConversationMediaRepositoryImpl
import com.android.messaging.data.conversation.store.ConversationSelfIdStore
import com.android.messaging.data.conversation.store.ConversationSelfIdStoreImpl
import com.android.messaging.data.subscription.repository.ConversationSimSelectionRepository
import com.android.messaging.data.subscription.repository.ConversationSimSelectionRepositoryImpl
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepositoryImpl
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGrantedImpl
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirementsImpl
import com.android.messaging.domain.conversation.usecase.action.CreateDefaultSmsRoleRequest
import com.android.messaging.domain.conversation.usecase.action.CreateDefaultSmsRoleRequestImpl
import com.android.messaging.domain.conversation.usecase.draft.GetConversationDraftSendProtocol
import com.android.messaging.domain.conversation.usecase.draft.GetConversationDraftSendProtocolImpl
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraftImpl
import com.android.messaging.domain.conversation.usecase.forward.CreateForwardedMessage
import com.android.messaging.domain.conversation.usecase.forward.CreateForwardedMessageImpl
import com.android.messaging.domain.conversation.usecase.forward.ForwardedMessageSubjectFormatter
import com.android.messaging.domain.conversation.usecase.forward.ForwardedMessageSubjectFormatterImpl
import com.android.messaging.domain.conversation.usecase.participant.CanAddMoreConversationParticipants
import com.android.messaging.domain.conversation.usecase.participant.CanAddMoreConversationParticipantsImpl
import com.android.messaging.domain.conversation.usecase.participant.IsConversationRecipientLimitExceeded
import com.android.messaging.domain.conversation.usecase.participant.IsConversationRecipientLimitExceededImpl
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationIdImpl
import com.android.messaging.domain.conversation.usecase.telephony.IsDeviceVoiceCapable
import com.android.messaging.domain.conversation.usecase.telephony.IsDeviceVoiceCapableImpl
import com.android.messaging.domain.conversation.usecase.telephony.IsEmergencyPhoneNumber
import com.android.messaging.domain.conversation.usecase.telephony.IsEmergencyPhoneNumberImpl
import com.android.messaging.ui.contact.mapper.ContactUiModelMapper
import com.android.messaging.ui.contact.mapper.ContactUiModelMapperImpl
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapperImpl
import com.android.messaging.ui.conversation.composer.mapper.ConversationComposerAttachmentUiModelMapper
import com.android.messaging.ui.conversation.composer.mapper.ConversationComposerAttachmentUiModelMapperImpl
import com.android.messaging.ui.conversation.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.composer.mapper.ConversationComposerUiStateMapperImpl
import com.android.messaging.ui.conversation.mediapicker.mapper.ConversationDraftAttachmentMapper
import com.android.messaging.ui.conversation.mediapicker.mapper.ConversationDraftAttachmentMapperImpl
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapperImpl
import com.android.messaging.ui.conversation.metadata.mapper.ConversationMetadataUiStateMapper
import com.android.messaging.ui.conversation.metadata.mapper.ConversationMetadataUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Singleton

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
    abstract fun bindConversationContactsRepository(
        impl: ContactsRepositoryImpl,
    ): ContactsRepository

    @Binds
    @Reusable
    abstract fun bindCanAddMoreConversationParticipants(
        impl: CanAddMoreConversationParticipantsImpl,
    ): CanAddMoreConversationParticipants

    @Binds
    @Reusable
    abstract fun bindContactDestinationFormatter(
        impl: ContactDestinationFormatterImpl,
    ): ContactDestinationFormatter

    @Binds
    @Reusable
    abstract fun bindContactUiModelMapper(
        impl: ContactUiModelMapperImpl,
    ): ContactUiModelMapper

    @Binds
    @Reusable
    abstract fun bindCheckConversationActionRequirements(
        impl: CheckConversationActionRequirementsImpl,
    ): CheckConversationActionRequirements

    @Binds
    @Reusable
    abstract fun bindCreateDefaultSmsRoleRequest(
        impl: CreateDefaultSmsRoleRequestImpl,
    ): CreateDefaultSmsRoleRequest

    @Binds
    @Reusable
    abstract fun bindIsDeviceVoiceCapable(
        impl: IsDeviceVoiceCapableImpl,
    ): IsDeviceVoiceCapable

    @Binds
    @Reusable
    abstract fun bindIsEmergencyPhoneNumber(
        impl: IsEmergencyPhoneNumberImpl,
    ): IsEmergencyPhoneNumber

    @Binds
    @Reusable
    abstract fun bindCreateForwardedMessage(
        impl: CreateForwardedMessageImpl,
    ): CreateForwardedMessage

    @Binds
    @Reusable
    abstract fun bindGetConversationDraftSendProtocol(
        impl: GetConversationDraftSendProtocolImpl,
    ): GetConversationDraftSendProtocol

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
    abstract fun bindConversationSelfIdStore(
        impl: ConversationSelfIdStoreImpl,
    ): ConversationSelfIdStore

    @Binds
    @Reusable
    abstract fun bindSubscriptionsRepository(
        impl: SubscriptionsRepositoryImpl,
    ): SubscriptionsRepository

    @Binds
    @Reusable
    abstract fun bindConversationAttachmentRepository(
        impl: ConversationAttachmentsRepositoryImpl,
    ): ConversationAttachmentsRepository

    @Binds
    @Singleton
    abstract fun bindConversationSimSelectionRepository(
        impl: ConversationSimSelectionRepositoryImpl,
    ): ConversationSimSelectionRepository

    @Binds
    @Reusable
    abstract fun bindConversationDraftAttachmentMapper(
        impl: ConversationDraftAttachmentMapperImpl,
    ): ConversationDraftAttachmentMapper

    @Binds
    @Reusable
    abstract fun bindConversationComposerAttachmentUiModelMapper(
        impl: ConversationComposerAttachmentUiModelMapperImpl,
    ): ConversationComposerAttachmentUiModelMapper

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
    abstract fun bindConversationVCardAttachmentUiModelMapper(
        impl: ConversationVCardAttachmentUiModelMapperImpl,
    ): ConversationVCardAttachmentUiModelMapper

    @Binds
    @Reusable
    abstract fun bindConversationVCardMetadataRepository(
        impl: ConversationVCardMetadataRepositoryImpl,
    ): ConversationVCardMetadataRepository

    @Binds
    @Reusable
    abstract fun bindConversationVCardMetadataMapper(
        impl: ConversationVCardMetadataMapperImpl,
    ): ConversationVCardMetadataMapper

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
