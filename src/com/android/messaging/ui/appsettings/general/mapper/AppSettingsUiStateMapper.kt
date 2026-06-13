package com.android.messaging.ui.appsettings.general.mapper

import android.content.Context
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.AppSettings
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface AppSettingsUiStateMapper {
    fun map(appSettings: AppSettings): AppSettingsUiState
}

internal class AppSettingsUiStateMapperImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppSettingsUiStateMapper {

    override fun map(appSettings: AppSettings): AppSettingsUiState {
        return AppSettingsUiState(
            isDefaultSmsApp = appSettings.isDefaultSmsApp,
            defaultSmsAppLabel = context.getString(
                R.string.default_sms_app,
                appSettings.defaultSmsAppLabel,
            ),
            sendSoundEnabled = appSettings.sendSoundEnabled,
            isDebugEnabled = appSettings.isDebugEnabled,
            dumpSmsEnabled = appSettings.dumpSmsEnabled,
            dumpMmsEnabled = appSettings.dumpMmsEnabled,
            colorScheme = appSettings.colorScheme,
        )
    }
}
