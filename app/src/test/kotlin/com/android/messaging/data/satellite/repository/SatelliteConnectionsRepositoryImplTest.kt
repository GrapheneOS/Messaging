package com.android.messaging.data.satellite.repository

import android.telephony.NetworkRegistrationInfo
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import app.cash.turbine.test
import com.android.messaging.data.satellite.model.SatelliteConnection
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SatelliteConnectionsRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val telephonyManager = mockk<TelephonyManager>()
    private val subscriptionTelephonyManagers = mutableMapOf<Int, TelephonyManager>()
    private val callbacks = mutableMapOf<Int, TelephonyCallback.ServiceStateListener>()

    @Before
    fun setUp() {
        every { telephonyManager.createForSubscriptionId(any()) } answers {
            val subId = firstArg<Int>()
            subscriptionTelephonyManagers.getOrPut(subId) {
                mockk<TelephonyManager>().also { manager ->
                    every { manager.registerTelephonyCallback(any(), any()) } answers {
                        callbacks[subId] = secondArg<TelephonyCallback>()
                            as TelephonyCallback.ServiceStateListener
                    }
                    every { manager.unregisterTelephonyCallback(any()) } just runs
                }
            }
        }
    }

    @Test
    fun observeSatelliteConnections_doesNotListenForThePlaceholderSubscription() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            createRepository()
                .observeSatelliteConnections(
                    subIds = listOf(SubId(ParticipantData.DEFAULT_SELF_SUB_ID)),
                )
                .test {
                    assertEquals(persistentMapOf<SubId, SatelliteConnection>(), awaitItem())
                    awaitComplete()
                }

            verify(exactly = 0) { telephonyManager.createForSubscriptionId(any()) }
        }
    }

    @Test
    fun observeSatelliteConnections_emitsBeforeAnySubscriptionReportsItsServiceState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            createRepository()
                .observeSatelliteConnections(subIds = listOf(SubId(SUB_ID), SubId(OTHER_SUB_ID)))
                .test {
                    assertEquals(persistentMapOf<SubId, SatelliteConnection>(), awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @Test
    fun observeSatelliteConnections_tracksASubscriptionMovingOntoAndOffASatellite() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            createRepository()
                .observeSatelliteConnections(subIds = listOf(SubId(SUB_ID)))
                .test {
                    callbacks.getValue(SUB_ID).onServiceStateChanged(terrestrialState())
                    assertEquals(persistentMapOf<SubId, SatelliteConnection>(), awaitItem())

                    callbacks.getValue(SUB_ID).onServiceStateChanged(
                        satelliteState(NetworkRegistrationInfo.SERVICE_TYPE_SMS),
                    )
                    assertEquals(
                        mapOf(SubId(SUB_ID) to TEXT_ONLY_CONNECTION),
                        awaitItem(),
                    )

                    callbacks.getValue(SUB_ID).onServiceStateChanged(terrestrialState())
                    assertEquals(persistentMapOf<SubId, SatelliteConnection>(), awaitItem())

                    cancelAndIgnoreRemainingEvents()
                }

            val subscriptionTelephonyManager = subscriptionTelephonyManagers.getValue(SUB_ID)
            verify {
                subscriptionTelephonyManager.unregisterTelephonyCallback(
                    callbacks.getValue(SUB_ID) as TelephonyCallback,
                )
            }
        }
    }

    @Test
    fun observeSatelliteConnections_reportsOnlyTheSubscriptionOnASatellite() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            createRepository()
                .observeSatelliteConnections(subIds = listOf(SubId(SUB_ID), SubId(OTHER_SUB_ID)))
                .test {
                    callbacks.getValue(SUB_ID).onServiceStateChanged(terrestrialState())
                    callbacks.getValue(OTHER_SUB_ID).onServiceStateChanged(
                        satelliteState(
                            NetworkRegistrationInfo.SERVICE_TYPE_SMS,
                            NetworkRegistrationInfo.SERVICE_TYPE_MMS,
                        ),
                    )

                    assertEquals(
                        mapOf(
                            SubId(OTHER_SUB_ID) to SatelliteConnection(
                                isSmsAvailable = true,
                                isMmsAvailable = true,
                            ),
                        ),
                        expectMostRecentItem(),
                    )
                }
        }
    }

    @Test
    fun observeSatelliteConnections_reportsNoConnectionWhenListeningIsNotAllowed() {
        every { telephonyManager.createForSubscriptionId(SUB_ID) } returns mockk {
            every {
                registerTelephonyCallback(any(), any())
            } throws SecurityException("READ_PHONE_STATE revoked")
        }

        runTest(context = mainDispatcherRule.testDispatcher) {
            createRepository()
                .observeSatelliteConnections(subIds = listOf(SubId(SUB_ID)))
                .test {
                    assertEquals(persistentMapOf<SubId, SatelliteConnection>(), awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    private fun createRepository(): SatelliteConnectionsRepositoryImpl {
        return SatelliteConnectionsRepositoryImpl(
            telephonyManager = telephonyManager,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun terrestrialState(): ServiceState {
        return serviceState(isNonTerrestrial = false, NetworkRegistrationInfo.SERVICE_TYPE_SMS)
    }

    private fun satelliteState(vararg services: Int): ServiceState {
        return serviceState(isNonTerrestrial = true, *services)
    }

    private fun serviceState(isNonTerrestrial: Boolean, vararg services: Int): ServiceState {
        val registration = mockk<NetworkRegistrationInfo>()
        every { registration.isNonTerrestrialNetwork } returns isNonTerrestrial
        every { registration.availableServices } returns services.toList()

        val serviceState = mockk<ServiceState>()
        every { serviceState.networkRegistrationInfoList } returns listOf(registration)
        return serviceState
    }

    private companion object {
        private const val SUB_ID = 3
        private const val OTHER_SUB_ID = 4
        private val TEXT_ONLY_CONNECTION = SatelliteConnection(
            isSmsAvailable = true,
            isMmsAvailable = false,
        )
    }
}
