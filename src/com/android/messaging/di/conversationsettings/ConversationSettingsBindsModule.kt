package com.android.messaging.di.conversationsettings

import com.android.messaging.ui.conversationsettings.screen.delegate.ConversationSettingsDelegate
import com.android.messaging.ui.conversationsettings.screen.delegate.ConversationSettingsDelegateImpl
import com.android.messaging.ui.conversationsettings.screen.mapper.ConversationSettingsUiStateMapper
import com.android.messaging.ui.conversationsettings.screen.mapper.ConversationSettingsUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class ConversationSettingsBindsModule {

    @Binds
    abstract fun bindConversationSettingsDelegate(
        impl: ConversationSettingsDelegateImpl,
    ): ConversationSettingsDelegate

    @Binds
    @Reusable
    abstract fun bindConversationSettingsUiStateMapper(
        impl: ConversationSettingsUiStateMapperImpl,
    ): ConversationSettingsUiStateMapper
}
