package com.android.messaging.di.widget

import com.android.messaging.widget.WidgetRebuilder
import com.android.messaging.widget.WidgetRebuilderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WidgetBindsModule {

    @Binds
    @Singleton
    abstract fun bindWidgetRebuilder(
        impl: WidgetRebuilderImpl,
    ): WidgetRebuilder
}
