package com.android.messaging.ui.appsettings.subscription.mapper.subscriptionsettingsuistatemapper

import com.android.messaging.R
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SubscriptionSettingsUiStateMapperSubscriptionMappingTest :
    BaseSubscriptionSettingsUiStateMapperTest() {

    @Test
    fun map_mapsEverySubscriptionFieldOntoUiState() {
        val uiState = mapper.map(
            data = subscriptionData(
                isDefaultSmsApp = true,
                activeSubscriptionCount = 1,
                isCellBroadcastAppEnabled = true,
                defaultSelfSubscription = perSubscription(
                    subId = 7,
                    savedPhoneNumber = "+15551230000",
                    defaultPhoneNumber = "+15559990000",
                    formattedSavedPhoneNumber = "(555) 123-0000",
                    isGroupMmsSupported = true,
                    isGroupMmsEnabled = false,
                    autoRetrieveMms = false,
                    autoRetrieveMmsWhenRoaming = true,
                    isDeliveryReportsSupported = true,
                    deliveryReportsEnabled = false,
                    showCellBroadcast = true,
                ),
            ),
        )

        assertEquals(
            SubscriptionUiState(
                subId = 7,
                displayName = context.getString(R.string.advanced_settings),
                displayDetail = "(555) 123-0000",
                phoneNumber = "+15551230000",
                defaultPhoneNumber = "+15559990000",
                isGroupMmsSupported = true,
                isGroupMmsEnabled = false,
                autoRetrieveMms = false,
                autoRetrieveMmsWhenRoaming = true,
                isDeliveryReportsSupported = true,
                deliveryReportsEnabled = false,
                isWirelessAlertsSupported = true,
                isDefaultSmsApp = true,
            ),
            uiState.subscriptions.first(),
        )
    }

    @Test
    fun map_multiSimSubscriptions_mapEachNonDefaultFromItsOwnFields() {
        val uiState = mapper.map(
            data = subscriptionData(
                isDefaultSmsApp = false,
                activeSubscriptionCount = 2,
                isCellBroadcastAppEnabled = true,
                nonDefaultActiveSelfSubscriptions = listOf(
                    perSubscription(
                        subId = 1,
                        subscriptionName = "Verizon",
                        formattedSavedPhoneNumber = "(555) 111-0000",
                        showCellBroadcast = true,
                    ),
                    perSubscription(
                        subId = 2,
                        subscriptionName = "T-Mobile",
                        formattedSavedPhoneNumber = "(555) 222-0000",
                        showCellBroadcast = false,
                    ),
                ),
            ),
        )

        assertEquals("(555) 111-0000", uiState.subscriptions[0].displayDetail)
        assertTrue(uiState.subscriptions[0].isWirelessAlertsSupported)
        assertFalse(uiState.subscriptions[0].isDefaultSmsApp)
        assertEquals("(555) 222-0000", uiState.subscriptions[1].displayDetail)
        assertFalse(uiState.subscriptions[1].isWirelessAlertsSupported)
    }

    @Test
    fun map_prefersSavedFormattedNumberOverDefaultFormattedNumber() {
        val uiState = mapper.map(
            data = subscriptionData(
                defaultSelfSubscription = perSubscription(
                    formattedSavedPhoneNumber = "(555) 111-1111",
                    formattedDefaultPhoneNumber = "(555) 222-2222",
                ),
            ),
        )

        assertEquals("(555) 111-1111", uiState.subscriptions.first().displayDetail)
    }

    @Test
    fun map_whenSavedFormattedNumberMissing_usesDefaultFormattedNumber() {
        val uiState = mapper.map(
            data = subscriptionData(
                defaultSelfSubscription = perSubscription(
                    formattedSavedPhoneNumber = null,
                    formattedDefaultPhoneNumber = "(555) 222-2222",
                ),
            ),
        )

        assertEquals("(555) 222-2222", uiState.subscriptions.first().displayDetail)
    }

    @Test
    fun map_whenNoFormattedNumbers_usesUnknownPhoneNumberLabel() {
        val uiState = mapper.map(
            data = subscriptionData(
                defaultSelfSubscription = perSubscription(
                    formattedSavedPhoneNumber = null,
                    formattedDefaultPhoneNumber = null,
                ),
            ),
        )

        assertEquals(
            context.getString(R.string.unknown_phone_number_pref_display_value),
            uiState.subscriptions.first().displayDetail,
        )
    }

    @Test
    fun map_wirelessAlertsUnsupported_whenCellBroadcastShownButAppDisabled() {
        val uiState = mapper.map(
            data = subscriptionData(
                isCellBroadcastAppEnabled = false,
                defaultSelfSubscription = perSubscription(showCellBroadcast = true),
            ),
        )

        assertFalse(uiState.subscriptions.first().isWirelessAlertsSupported)
    }

    @Test
    fun map_wirelessAlertsUnsupported_whenCellBroadcastNotShownButAppEnabled() {
        val uiState = mapper.map(
            data = subscriptionData(
                isCellBroadcastAppEnabled = true,
                defaultSelfSubscription = perSubscription(showCellBroadcast = false),
            ),
        )

        assertFalse(uiState.subscriptions.first().isWirelessAlertsSupported)
    }
}
