package com.android.messaging.data.appsettings.model

import androidx.annotation.StringRes
import com.android.messaging.R

internal enum class ConversationSwipePref(
    @param:StringRes val keyResId: Int,
) {
    START_TO_END(R.string.conversation_swipe_start_to_end_pref_key),
    END_TO_START(R.string.conversation_swipe_end_to_start_pref_key),
}
