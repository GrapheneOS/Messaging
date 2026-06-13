package com.android.messaging.data.appsettings.model

import androidx.annotation.StringRes
import com.android.messaging.R

internal enum class AppColorScheme(
    @param:StringRes val titleResId: Int,
) {
    DYNAMIC(R.string.color_scheme_dynamic),
    GREEN(R.string.color_scheme_green),
    GRAY(R.string.color_scheme_gray),
    BLUE(R.string.color_scheme_blue),
    DARK_BLUE(R.string.color_scheme_dark_blue),
    PURPLE(R.string.color_scheme_purple),
    VIOLET(R.string.color_scheme_violet),
    TEAL(R.string.color_scheme_teal),
    ;

    companion object {
        fun fromPrefValue(value: String?): AppColorScheme =
            entries.firstOrNull { it.name == value } ?: DYNAMIC
    }
}
