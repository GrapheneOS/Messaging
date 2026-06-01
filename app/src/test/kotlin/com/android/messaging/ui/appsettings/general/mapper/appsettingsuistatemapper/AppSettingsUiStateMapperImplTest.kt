package com.android.messaging.ui.appsettings.general.mapper.appsettingsuistatemapper

import android.content.Context
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.AppSettings
import com.android.messaging.ui.appsettings.general.mapper.AppSettingsUiStateMapperImpl
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppSettingsUiStateMapperImplTest {

    @Test
    fun map_formatsDefaultSmsAppLabelAndCopiesEveryField() {
        val context = mockk<Context>()
        every {
            context.getString(R.string.default_sms_app, DEFAULT_SMS_APP_LABEL)
        } returns FORMATTED_DEFAULT_SMS_APP_LABEL
        val mapper = AppSettingsUiStateMapperImpl(context = context)

        val result = mapper.map(
            appSettings = AppSettings(
                isDefaultSmsApp = true,
                defaultSmsAppLabel = DEFAULT_SMS_APP_LABEL,
                sendSoundEnabled = false,
                isDebugEnabled = true,
                dumpSmsEnabled = true,
                dumpMmsEnabled = false,
            ),
        )

        assertEquals(
            AppSettingsUiState(
                isDefaultSmsApp = true,
                defaultSmsAppLabel = FORMATTED_DEFAULT_SMS_APP_LABEL,
                sendSoundEnabled = false,
                isDebugEnabled = true,
                dumpSmsEnabled = true,
                dumpMmsEnabled = false,
            ),
            result,
        )
    }

    private companion object {
        private const val DEFAULT_SMS_APP_LABEL = "Messaging"
        private const val FORMATTED_DEFAULT_SMS_APP_LABEL = "Default SMS app: Messaging"
    }
}
