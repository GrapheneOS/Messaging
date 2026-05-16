package com.android.messaging.data.appsettings.model

import androidx.annotation.StringRes
import com.android.messaging.R

internal enum class AppBooleanPref(
    @param:StringRes val keyResId: Int,
) {
    SEND_SOUND(R.string.send_sound_pref_key),
    DUMP_SMS(R.string.dump_sms_pref_key),
    DUMP_MMS(R.string.dump_mms_pref_key),
}
