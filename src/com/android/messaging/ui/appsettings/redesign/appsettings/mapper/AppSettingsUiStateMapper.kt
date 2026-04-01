package com.android.messaging.ui.appsettings.redesign.appsettings.mapper

import android.content.Context
import com.android.messaging.R
import com.android.messaging.ui.appsettings.redesign.appsettings.model.AppSettingsUiState
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.DebugUtils
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface AppSettingsUiStateMapper {
    fun map(): AppSettingsUiState
}

internal class AppSettingsUiStateMapperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppSettingsUiStateMapper {

    override fun map(): AppSettingsUiState {
        val appPrefs = BuglePrefs.getApplicationPrefs()
        val phoneUtils = PhoneUtils.getDefault()

        val sendSoundKey = context.getString(R.string.send_sound_pref_key)
        val dumpSmsKey = context.getString(R.string.dump_sms_pref_key)
        val dumpMmsKey = context.getString(R.string.dump_mms_pref_key)

        return AppSettingsUiState(
            isDefaultSmsApp = phoneUtils.isDefaultSmsApp,
            defaultSmsAppLabel = context.getString(
                R.string.default_sms_app,
                phoneUtils.defaultSmsAppLabel,
            ),
            sendSoundEnabled = appPrefs.getBoolean(
                sendSoundKey,
                context.resources.getBoolean(R.bool.send_sound_pref_default),
            ),
            isDebugEnabled = DebugUtils.isDebugEnabled(),
            dumpSmsEnabled = appPrefs.getBoolean(
                dumpSmsKey,
                context.resources.getBoolean(R.bool.dump_sms_pref_default),
            ),
            dumpMmsEnabled = appPrefs.getBoolean(
                dumpMmsKey,
                context.resources.getBoolean(R.bool.dump_mms_pref_default),
            ),
        )
    }
}
