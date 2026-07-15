package com.android.messaging.di.photoviewer

import com.android.messaging.data.media.repository.PhotoViewerRepository
import com.android.messaging.data.media.repository.PhotoViewerRepositoryImpl
import com.android.messaging.domain.photoviewer.usecase.NormalizePhotoViewerUri
import com.android.messaging.domain.photoviewer.usecase.NormalizePhotoViewerUriImpl
import com.android.messaging.domain.photoviewer.usecase.PreparePhotoViewerSendUri
import com.android.messaging.domain.photoviewer.usecase.PreparePhotoViewerSendUriImpl
import com.android.messaging.domain.photoviewer.usecase.ResolveConversationPhotoViewerInitialOccurrenceIndex
import com.android.messaging.domain.photoviewer.usecase.ResolveConversationPhotoViewerInitialOccurrenceIndexImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PhotoViewerBindsModule {

    @Binds
    @Reusable
    abstract fun bindNormalizePhotoViewerUri(
        impl: NormalizePhotoViewerUriImpl,
    ): NormalizePhotoViewerUri

    @Binds
    @Reusable
    abstract fun bindResolveConversationPhotoViewerInitialOccurrenceIndex(
        impl: ResolveConversationPhotoViewerInitialOccurrenceIndexImpl,
    ): ResolveConversationPhotoViewerInitialOccurrenceIndex

    @Binds
    @Reusable
    abstract fun bindPhotoViewerRepository(
        impl: PhotoViewerRepositoryImpl,
    ): PhotoViewerRepository

    @Binds
    @Reusable
    abstract fun bindPreparePhotoViewerSendUri(
        impl: PreparePhotoViewerSendUriImpl,
    ): PreparePhotoViewerSendUri
}
