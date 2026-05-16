package com.android.messaging.domain.subscriptionsettings.usecase

import android.content.Context
import com.android.messaging.R
import com.android.messaging.datamodel.ParticipantRefresh
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal fun interface SetSubscriptionPhoneNumber {
    suspend operator fun invoke(subId: Int, phoneNumber: String)
}

internal class SetSubscriptionPhoneNumberImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SetSubscriptionPhoneNumber {

    override suspend fun invoke(subId: Int, phoneNumber: String) {
        withContext(ioDispatcher) {
            val phoneUtils = PhoneUtils.get(subId)
            val canonical = phoneUtils.getCanonicalBySystemLocale(phoneNumber)
            val defaultCanonical = phoneUtils.getCanonicalBySystemLocale(
                phoneUtils.getCanonicalForSelf(false),
            )

            val key = context.getString(R.string.mms_phone_number_pref_key)
            val subPrefs = BuglePrefs.getSubscriptionPrefs(subId)
            if (canonical == defaultCanonical || phoneNumber.isEmpty()) {
                subPrefs.remove(key)
            } else {
                subPrefs.putString(key, phoneNumber)
            }

            ParticipantRefresh.refreshSelfParticipants()
        }
    }
}
