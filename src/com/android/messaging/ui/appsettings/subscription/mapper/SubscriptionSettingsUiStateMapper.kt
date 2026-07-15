package com.android.messaging.ui.appsettings.subscription.mapper

import android.content.Context
import com.android.messaging.R
import com.android.messaging.data.subscriptionsettings.model.PerSubscriptionData
import com.android.messaging.data.subscriptionsettings.model.SubscriptionSettingsData
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal interface SubscriptionSettingsUiStateMapper {
    fun map(data: SubscriptionSettingsData): SubscriptionSettingsUiState
}

internal class SubscriptionSettingsUiStateMapperImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SubscriptionSettingsUiStateMapper {

    override fun map(data: SubscriptionSettingsData): SubscriptionSettingsUiState {
        return SubscriptionSettingsUiState(
            isMultiSim = data.activeSubscriptionCount > 1,
            isLoaded = true,
            subscriptions = mapSubscriptions(data),
        )
    }

    private fun mapSubscriptions(
        data: SubscriptionSettingsData,
    ): ImmutableList<SubscriptionUiState> {
        return when (data.activeSubscriptionCount) {
            0 -> {
                persistentListOf()
            }

            1 -> {
                persistentListOf(
                    mapSingleSubscription(
                        perSub = data.defaultSelfSubscription,
                        displayName = context.getString(R.string.advanced_settings),
                        isDefaultSmsApp = data.isDefaultSmsApp,
                        isCellBroadcastAppEnabled = data.isCellBroadcastAppEnabled,
                    ),
                )
            }

            else -> {
                mapMultiSimSubscriptions(data)
            }
        }
    }

    private fun mapMultiSimSubscriptions(
        data: SubscriptionSettingsData,
    ): ImmutableList<SubscriptionUiState> {
        val nonDefaults = data.nonDefaultActiveSelfSubscriptions

        return when {
            nonDefaults.size > 1 -> {
                nonDefaults.map { per ->
                    mapSingleSubscription(
                        perSub = per,
                        displayName = context.getString(
                            R.string.sim_specific_settings,
                            per.subscriptionName,
                        ),
                        isDefaultSmsApp = data.isDefaultSmsApp,
                        isCellBroadcastAppEnabled = data.isCellBroadcastAppEnabled,
                    )
                }.toImmutableList()
            }

            nonDefaults.size == 1 -> {
                persistentListOf(
                    mapSingleSubscription(
                        perSub = nonDefaults.first(),
                        displayName = context.getString(R.string.advanced_settings),
                        isDefaultSmsApp = data.isDefaultSmsApp,
                        isCellBroadcastAppEnabled = data.isCellBroadcastAppEnabled,
                    ),
                )
            }

            else -> {
                persistentListOf(
                    mapSingleSubscription(
                        perSub = data.defaultSelfSubscription,
                        displayName = context.getString(R.string.advanced_settings),
                        isDefaultSmsApp = data.isDefaultSmsApp,
                        isCellBroadcastAppEnabled = data.isCellBroadcastAppEnabled,
                    ),
                )
            }
        }
    }

    private fun mapSingleSubscription(
        perSub: PerSubscriptionData,
        displayName: String,
        isDefaultSmsApp: Boolean,
        isCellBroadcastAppEnabled: Boolean,
    ): SubscriptionUiState {
        val formattedNumber = perSub.formattedSavedPhoneNumber
            ?: perSub.formattedDefaultPhoneNumber
            ?: context.getString(R.string.unknown_phone_number_pref_display_value)

        return SubscriptionUiState(
            subId = perSub.subId,
            displayName = displayName,
            displayDetail = formattedNumber,
            phoneNumber = perSub.savedPhoneNumber,
            defaultPhoneNumber = perSub.defaultPhoneNumber,
            isGroupMmsSupported = perSub.isGroupMmsSupported,
            isGroupMmsEnabled = perSub.isGroupMmsEnabled,
            autoRetrieveMms = perSub.autoRetrieveMms,
            autoRetrieveMmsWhenRoaming = perSub.autoRetrieveMmsWhenRoaming,
            isDeliveryReportsSupported = perSub.isDeliveryReportsSupported,
            deliveryReportsEnabled = perSub.deliveryReportsEnabled,
            isWirelessAlertsSupported = perSub.showCellBroadcast && isCellBroadcastAppEnabled,
            isDefaultSmsApp = isDefaultSmsApp,
        )
    }
}
