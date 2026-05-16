package com.android.messaging.data.subscriptionsettings.model

import androidx.annotation.StringRes
import com.android.messaging.R

internal enum class SubscriptionBooleanPref(
    @param:StringRes val keyResId: Int,
) {
    GROUP_MMS(R.string.group_mms_pref_key),
    AUTO_RETRIEVE_MMS(R.string.auto_retrieve_mms_pref_key),
    AUTO_RETRIEVE_MMS_WHEN_ROAMING(R.string.auto_retrieve_mms_when_roaming_pref_key),
    DELIVERY_REPORTS(R.string.delivery_reports_pref_key),
}
