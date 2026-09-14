package com.android.messaging.di.vcard

import com.android.messaging.data.vcard.mapper.VCardEntrySummarizer
import com.android.messaging.data.vcard.mapper.VCardEntrySummarizerImpl
import com.android.messaging.data.vcard.parser.VCardParser
import com.android.messaging.data.vcard.parser.VCardParserImpl
import com.android.messaging.data.vcard.photo.VCardPhotoDownscaler
import com.android.messaging.data.vcard.photo.VCardPhotoDownscalerImpl
import com.android.messaging.data.vcard.repository.VCardEntryRepository
import com.android.messaging.data.vcard.repository.VCardEntryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class VCardBindsModule {

    @Binds
    @Reusable
    abstract fun bindVCardParser(
        impl: VCardParserImpl,
    ): VCardParser

    @Binds
    @Reusable
    abstract fun bindVCardPhotoDownscaler(
        impl: VCardPhotoDownscalerImpl,
    ): VCardPhotoDownscaler

    @Binds
    @Reusable
    abstract fun bindVCardEntrySummarizer(
        impl: VCardEntrySummarizerImpl,
    ): VCardEntrySummarizer

    @Binds
    @Singleton
    abstract fun bindVCardEntryRepository(
        impl: VCardEntryRepositoryImpl,
    ): VCardEntryRepository
}
