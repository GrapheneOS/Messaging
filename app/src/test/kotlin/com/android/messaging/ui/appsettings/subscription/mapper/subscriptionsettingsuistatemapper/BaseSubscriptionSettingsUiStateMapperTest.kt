package com.android.messaging.ui.appsettings.subscription.mapper.subscriptionsettingsuistatemapper

import android.content.Context
import com.android.messaging.R
import com.android.messaging.data.subscriptionsettings.model.PerSubscriptionData
import com.android.messaging.data.subscriptionsettings.model.SubscriptionSettingsData
import com.android.messaging.ui.appsettings.subscription.mapper.SubscriptionSettingsUiStateMapperImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Before

internal abstract class BaseSubscriptionSettingsUiStateMapperTest {

    protected val context: Context = mockk()
    protected val mapper = SubscriptionSettingsUiStateMapperImpl(context = context)

    @Before
    fun setUpBaseSubscriptionSettingsUiStateMapperTest() {
        every { context.getString(R.string.advanced_settings) } returns ADVANCED_SETTINGS_LABEL
        every {
            context.getString(R.string.sim_specific_settings, VERIZON_SUBSCRIPTION_NAME)
        } returns VERIZON_SIM_SPECIFIC_LABEL
        every {
            context.getString(R.string.sim_specific_settings, T_MOBILE_SUBSCRIPTION_NAME)
        } returns T_MOBILE_SIM_SPECIFIC_LABEL
        every {
            context.getString(R.string.sim_specific_settings, null)
        } returns NULL_SIM_SPECIFIC_LABEL
        every {
            context.getString(R.string.unknown_phone_number_pref_display_value)
        } returns UNKNOWN_PHONE_NUMBER_LABEL
    }

    protected fun subscriptionData(
        isDefaultSmsApp: Boolean = false,
        activeSubscriptionCount: Int = 1,
        isCellBroadcastAppEnabled: Boolean = false,
        defaultSelfSubscription: PerSubscriptionData = perSubscription(),
        nonDefaultActiveSelfSubscriptions: List<PerSubscriptionData> = persistentListOf(),
    ): SubscriptionSettingsData {
        return SubscriptionSettingsData(
            isDefaultSmsApp = isDefaultSmsApp,
            activeSubscriptionCount = activeSubscriptionCount,
            isCellBroadcastAppEnabled = isCellBroadcastAppEnabled,
            defaultSelfSubscription = defaultSelfSubscription,
            nonDefaultActiveSelfSubscriptions = nonDefaultActiveSelfSubscriptions.toImmutableList(),
        )
    }

    protected fun perSubscription(
        subId: Int = 1,
        subscriptionName: String? = "SIM 1",
        savedPhoneNumber: String = "",
        defaultPhoneNumber: String = "",
        formattedSavedPhoneNumber: String? = null,
        formattedDefaultPhoneNumber: String? = null,
        isGroupMmsSupported: Boolean = false,
        isGroupMmsEnabled: Boolean = true,
        autoRetrieveMms: Boolean = true,
        autoRetrieveMmsWhenRoaming: Boolean = false,
        isDeliveryReportsSupported: Boolean = false,
        deliveryReportsEnabled: Boolean = false,
        showCellBroadcast: Boolean = false,
    ): PerSubscriptionData {
        return PerSubscriptionData(
            subId = subId,
            subscriptionName = subscriptionName,
            savedPhoneNumber = savedPhoneNumber,
            defaultPhoneNumber = defaultPhoneNumber,
            formattedSavedPhoneNumber = formattedSavedPhoneNumber,
            formattedDefaultPhoneNumber = formattedDefaultPhoneNumber,
            isGroupMmsSupported = isGroupMmsSupported,
            isGroupMmsEnabled = isGroupMmsEnabled,
            autoRetrieveMms = autoRetrieveMms,
            autoRetrieveMmsWhenRoaming = autoRetrieveMmsWhenRoaming,
            isDeliveryReportsSupported = isDeliveryReportsSupported,
            deliveryReportsEnabled = deliveryReportsEnabled,
            showCellBroadcast = showCellBroadcast,
        )
    }

    private companion object {
        private const val ADVANCED_SETTINGS_LABEL = "Advanced settings"
        private const val NULL_SIM_SPECIFIC_LABEL = "SIM-specific settings: null"
        private const val T_MOBILE_SIM_SPECIFIC_LABEL = "SIM-specific settings: T-Mobile"
        private const val T_MOBILE_SUBSCRIPTION_NAME = "T-Mobile"
        private const val UNKNOWN_PHONE_NUMBER_LABEL = "Unknown phone number"
        private const val VERIZON_SIM_SPECIFIC_LABEL = "SIM-specific settings: Verizon"
        private const val VERIZON_SUBSCRIPTION_NAME = "Verizon"
    }
}
