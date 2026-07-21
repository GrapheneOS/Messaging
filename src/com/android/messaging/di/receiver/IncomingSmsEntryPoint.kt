package com.android.messaging.di.receiver

import com.android.messaging.data.secondaryuser.SecondaryUserMessageResolver
import com.android.messaging.data.secondaryuser.SecondaryUserNotifier
import com.android.messaging.data.sms.IncomingSmsDeliverer
import com.android.messaging.data.sms.IncomingSmsParser
import com.android.messaging.data.sms.SmsReceiverToggle
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.IoDispatcher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface IncomingSmsEntryPoint {
    fun incomingSmsParser(): IncomingSmsParser
    fun incomingSmsDeliverer(): IncomingSmsDeliverer
    fun secondaryUserMessageResolver(): SecondaryUserMessageResolver
    fun secondaryUserNotifier(): SecondaryUserNotifier
    fun smsReceiverToggle(): SmsReceiverToggle

    @ApplicationCoroutineScope
    fun applicationScope(): CoroutineScope

    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher
}
