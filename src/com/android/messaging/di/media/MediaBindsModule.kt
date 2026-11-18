package com.android.messaging.di.media

import com.android.messaging.domain.media.usecase.DeleteOrphanedScratchFiles
import com.android.messaging.domain.media.usecase.DeleteOrphanedScratchFilesImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MediaBindsModule {

    @Binds
    @Reusable
    abstract fun bindDeleteOrphanedScratchFiles(
        impl: DeleteOrphanedScratchFilesImpl,
    ): DeleteOrphanedScratchFiles
}
