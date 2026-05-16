package com.android.messaging.data.appsettings.repository

import android.content.Context
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.AppBooleanPref
import com.android.messaging.data.appsettings.model.AppSettings
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.DebugUtils
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface AppSettingsRepository {
    suspend fun getAppSettings(): AppSettings
    suspend fun setBooleanPref(pref: AppBooleanPref, enabled: Boolean)
}

internal class AppSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppSettingsRepository {

    override suspend fun getAppSettings(): AppSettings {
        return withContext(ioDispatcher) {
            val appPrefs = BuglePrefs.getApplicationPrefs()
            val phoneUtils = PhoneUtils.getDefault()
            val resources = context.resources

            AppSettings(
                isDefaultSmsApp = phoneUtils.isDefaultSmsApp,
                defaultSmsAppLabel = phoneUtils.defaultSmsAppLabel,
                sendSoundEnabled = appPrefs.getBoolean(
                    context.getString(R.string.send_sound_pref_key),
                    resources.getBoolean(R.bool.send_sound_pref_default),
                ),
                isDebugEnabled = DebugUtils.isDebugEnabled(),
                dumpSmsEnabled = appPrefs.getBoolean(
                    context.getString(R.string.dump_sms_pref_key),
                    resources.getBoolean(R.bool.dump_sms_pref_default),
                ),
                dumpMmsEnabled = appPrefs.getBoolean(
                    context.getString(R.string.dump_mms_pref_key),
                    resources.getBoolean(R.bool.dump_mms_pref_default),
                ),
            )
        }
    }

    override suspend fun setBooleanPref(
        pref: AppBooleanPref,
        enabled: Boolean,
    ) {
        withContext(ioDispatcher) {
            BuglePrefs.getApplicationPrefs().putBoolean(
                context.getString(pref.keyResId),
                enabled,
            )
        }
    }
}
