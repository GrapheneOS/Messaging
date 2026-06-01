package com.android.messaging.ui.appsettings.subscription.mapper.subscriptionsettingsuistatemapper

import com.android.messaging.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SubscriptionSettingsUiStateMapperSelectionTest :
    BaseSubscriptionSettingsUiStateMapperTest() {

    @Test
    fun map_noActiveSubscriptions_isNotMultiSimAndProducesNoSubscriptions() {
        val uiState = mapper.map(
            data = subscriptionData(activeSubscriptionCount = 0),
        )

        assertEquals(false, uiState.isMultiSim)
        assertTrue(uiState.isLoaded)
        assertTrue(uiState.subscriptions.isEmpty())
    }

    @Test
    fun map_singleActiveSubscription_mapsDefaultSubscriptionWithAdvancedSettingsLabel() {
        val uiState = mapper.map(
            data = subscriptionData(
                activeSubscriptionCount = 1,
                defaultSelfSubscription = perSubscription(subId = 5),
            ),
        )

        assertEquals(false, uiState.isMultiSim)
        assertEquals(1, uiState.subscriptions.size)
        assertEquals(5, uiState.subscriptions.first().subId)
        assertEquals(
            context.getString(R.string.advanced_settings),
            uiState.subscriptions.first().displayName,
        )
    }

    @Test
    fun map_singleActiveSubscription_ignoresNonDefaultSubscriptions() {
        val uiState = mapper.map(
            data = subscriptionData(
                activeSubscriptionCount = 1,
                defaultSelfSubscription = perSubscription(subId = 3),
                nonDefaultActiveSelfSubscriptions = listOf(perSubscription(subId = 99)),
            ),
        )

        assertEquals(1, uiState.subscriptions.size)
        assertEquals(3, uiState.subscriptions.first().subId)
    }

    @Test
    fun map_multipleNonDefaultSubscriptions_mapsEachInOrderWithSimSpecificLabel() {
        val uiState = mapper.map(
            data = subscriptionData(
                activeSubscriptionCount = 2,
                nonDefaultActiveSelfSubscriptions = listOf(
                    perSubscription(subId = 1, subscriptionName = "Verizon"),
                    perSubscription(subId = 2, subscriptionName = "T-Mobile"),
                ),
            ),
        )

        assertEquals(true, uiState.isMultiSim)
        assertEquals(listOf(1, 2), uiState.subscriptions.map { it.subId })
        assertEquals(
            context.getString(R.string.sim_specific_settings, "Verizon"),
            uiState.subscriptions[0].displayName,
        )
        assertEquals(
            context.getString(R.string.sim_specific_settings, "T-Mobile"),
            uiState.subscriptions[1].displayName,
        )
    }

    @Test
    fun map_multiSimWithSingleNonDefault_mapsThatSubscriptionWithAdvancedSettingsLabel() {
        val uiState = mapper.map(
            data = subscriptionData(
                activeSubscriptionCount = 2,
                nonDefaultActiveSelfSubscriptions = listOf(perSubscription(subId = 9)),
            ),
        )

        assertEquals(true, uiState.isMultiSim)
        assertEquals(1, uiState.subscriptions.size)
        assertEquals(9, uiState.subscriptions.first().subId)
        assertEquals(
            context.getString(R.string.advanced_settings),
            uiState.subscriptions.first().displayName,
        )
    }

    @Test
    fun map_multiSimWithNoNonDefaults_fallsBackToDefaultSubscriptionWithAdvancedSettingsLabel() {
        val uiState = mapper.map(
            data = subscriptionData(
                activeSubscriptionCount = 2,
                defaultSelfSubscription = perSubscription(subId = 3),
                nonDefaultActiveSelfSubscriptions = emptyList(),
            ),
        )

        assertEquals(true, uiState.isMultiSim)
        assertEquals(1, uiState.subscriptions.size)
        assertEquals(3, uiState.subscriptions.first().subId)
        assertEquals(
            context.getString(R.string.advanced_settings),
            uiState.subscriptions.first().displayName,
        )
    }

    @Test
    fun map_multipleNonDefaultsWithNullName_passesNullThroughToSimSpecificLabel() {
        val uiState = mapper.map(
            data = subscriptionData(
                activeSubscriptionCount = 2,
                nonDefaultActiveSelfSubscriptions = listOf(
                    perSubscription(subId = 1, subscriptionName = null),
                    perSubscription(subId = 2, subscriptionName = "T-Mobile"),
                ),
            ),
        )

        assertEquals(
            context.getString(R.string.sim_specific_settings, null),
            uiState.subscriptions[0].displayName,
        )
    }
}
