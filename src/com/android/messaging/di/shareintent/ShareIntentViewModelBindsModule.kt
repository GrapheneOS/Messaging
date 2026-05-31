package com.android.messaging.di.shareintent

import com.android.messaging.ui.shareintent.screen.delegate.ShareIntentScreenDelegate
import com.android.messaging.ui.shareintent.screen.delegate.ShareIntentScreenDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class ShareIntentViewModelBindsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindShareIntentScreenDelegate(
        impl: ShareIntentScreenDelegateImpl,
    ): ShareIntentScreenDelegate
}
