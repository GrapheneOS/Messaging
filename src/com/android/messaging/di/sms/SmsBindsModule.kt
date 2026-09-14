package com.android.messaging.di.sms

import com.android.messaging.data.sms.IncomingSmsDeliverer
import com.android.messaging.data.sms.IncomingSmsDelivererImpl
import com.android.messaging.data.sms.IncomingSmsParser
import com.android.messaging.data.sms.IncomingSmsParserImpl
import com.android.messaging.data.sms.SmsReceiverToggle
import com.android.messaging.data.sms.SmsReceiverToggleImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SmsBindsModule {

    @Binds
    @Reusable
    abstract fun bindIncomingSmsParser(
        impl: IncomingSmsParserImpl,
    ): IncomingSmsParser

    @Binds
    @Reusable
    abstract fun bindIncomingSmsDeliverer(
        impl: IncomingSmsDelivererImpl,
    ): IncomingSmsDeliverer

    @Binds
    @Reusable
    abstract fun bindSmsReceiverToggle(
        impl: SmsReceiverToggleImpl,
    ): SmsReceiverToggle
}
