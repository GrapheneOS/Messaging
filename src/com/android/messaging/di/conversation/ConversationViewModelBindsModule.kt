package com.android.messaging.di.conversation

import com.android.messaging.ui.conversation.audio.delegate.ConversationAudioRecordingDelegate
import com.android.messaging.ui.conversation.audio.delegate.ConversationAudioRecordingDelegateImpl
import com.android.messaging.ui.conversation.composer.delegate.ConversationComposerAttachmentsDelegate
import com.android.messaging.ui.conversation.composer.delegate.ConversationComposerAttachmentsDelegateImpl
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegateImpl
import com.android.messaging.ui.conversation.focus.delegate.ConversationFocusDelegate
import com.android.messaging.ui.conversation.focus.delegate.ConversationFocusDelegateImpl
import com.android.messaging.ui.conversation.mediapicker.delegate.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.mediapicker.delegate.ConversationMediaPickerDelegateImpl
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessageSelectionDelegate
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessageSelectionDelegateImpl
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessagesDelegateImpl
import com.android.messaging.ui.conversation.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.metadata.delegate.ConversationMetadataDelegateImpl
import com.android.messaging.ui.conversation.recipientpicker.delegate.RecipientPickerDelegate
import com.android.messaging.ui.conversation.recipientpicker.delegate.RecipientPickerDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class ConversationViewModelBindsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindConversationAudioRecordingDelegate(
        impl: ConversationAudioRecordingDelegateImpl,
    ): ConversationAudioRecordingDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationComposerAttachmentsDelegate(
        impl: ConversationComposerAttachmentsDelegateImpl,
    ): ConversationComposerAttachmentsDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationDraftDelegate(
        impl: ConversationDraftDelegateImpl,
    ): ConversationDraftDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationMediaPickerDelegate(
        impl: ConversationMediaPickerDelegateImpl,
    ): ConversationMediaPickerDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationMessageSelectionDelegate(
        impl: ConversationMessageSelectionDelegateImpl,
    ): ConversationMessageSelectionDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationMessagesDelegate(
        impl: ConversationMessagesDelegateImpl,
    ): ConversationMessagesDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationMetadataDelegate(
        impl: ConversationMetadataDelegateImpl,
    ): ConversationMetadataDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationFocusDelegate(
        impl: ConversationFocusDelegateImpl,
    ): ConversationFocusDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindRecipientPickerDelegate(
        impl: RecipientPickerDelegateImpl,
    ): RecipientPickerDelegate
}
