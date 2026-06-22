package com.android.messaging.di.conversationlist

import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListActionsDelegate
import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListActionsDelegateImpl
import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListOptimisticSnapshotDelegate
import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListOptimisticSnapshotDelegateImpl
import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListSelectionDelegate
import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListSelectionDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class ConversationListViewModelBindsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindConversationListSelectionDelegate(
        impl: ConversationListSelectionDelegateImpl,
    ): ConversationListSelectionDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationListActionsDelegate(
        impl: ConversationListActionsDelegateImpl,
    ): ConversationListActionsDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationListOptimisticSnapshotDelegate(
        impl: ConversationListOptimisticSnapshotDelegateImpl,
    ): ConversationListOptimisticSnapshotDelegate
}
