package com.android.messaging.di.conversationsettings

import com.android.messaging.ui.conversationsettings.screen.delegate.ConversationSettingsDelegate
import com.android.messaging.ui.conversationsettings.screen.delegate.ConversationSettingsDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class ConversationSettingsViewModelBindsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindConversationSettingsDelegate(
        impl: ConversationSettingsDelegateImpl,
    ): ConversationSettingsDelegate
}
