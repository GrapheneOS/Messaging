package com.android.messaging.di.recipientselection

import com.android.messaging.ui.recipientselection.delegate.RecipientPickerDelegate
import com.android.messaging.ui.recipientselection.delegate.RecipientPickerDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class RecipientSelectionViewModelBindsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindRecipientPickerDelegate(
        impl: RecipientPickerDelegateImpl,
    ): RecipientPickerDelegate
}
