package com.android.messaging.data.satellite.repository

import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.android.messaging.data.satellite.model.SatelliteConnection
import com.android.messaging.data.satellite.model.satelliteConnectionOf
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.util.LogUtil
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal interface SatelliteConnectionsRepository {
    /**
     * Emits the satellite connection of each subscription in [subIds] that is currently registered
     * on a satellite network. Subscriptions on a terrestrial network, or with no service, are
     * absent from the map.
     */
    fun observeSatelliteConnections(
        subIds: Collection<SubId>,
    ): Flow<ImmutableMap<SubId, SatelliteConnection>>
}

internal class SatelliteConnectionsRepositoryImpl @Inject constructor(
    private val telephonyManager: TelephonyManager,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : SatelliteConnectionsRepository {

    override fun observeSatelliteConnections(
        subIds: Collection<SubId>,
    ): Flow<ImmutableMap<SubId, SatelliteConnection>> {
        val realSubIds = subIds
            .filter { subId -> subId.value > ParticipantData.DEFAULT_SELF_SUB_ID }
            .distinct()

        if (realSubIds.isEmpty()) {
            return flowOf(persistentMapOf())
        }

        val connectionFlows = realSubIds.map { subId ->
            observeSatelliteConnection(subId).map { connection -> subId to connection }
        }

        return combine(connectionFlows) { connections ->
            connections
                .mapNotNull { (subId, connection) -> connection?.let { subId to it } }
                .toMap()
                .toImmutableMap()
        }.distinctUntilChanged()
    }

    private fun observeSatelliteConnection(subId: SubId): Flow<SatelliteConnection?> {
        return callbackFlow {
            val subscriptionTelephonyManager =
                telephonyManager.createForSubscriptionId(subId.value)
            val callback = object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                override fun onServiceStateChanged(serviceState: ServiceState) {
                    trySend(satelliteConnectionOf(serviceState))
                }
            }

            val isRegistered = try {
                subscriptionTelephonyManager.registerTelephonyCallback(
                    defaultDispatcher.asExecutor(),
                    callback,
                )
                true
            } catch (e: SecurityException) {
                LogUtil.w(TAG, "Cannot observe the service state of subId ${subId.value}", e)
                trySend(null)
                false
            }

            awaitClose {
                if (isRegistered) {
                    subscriptionTelephonyManager.unregisterTelephonyCallback(callback)
                }
            }
        }.onStart {
            // TelephonyRegistry only reports the current state for a subscription with a valid
            // phone, so treat the subscription as terrestrial until a callback says otherwise.
            emit(null)
        }.distinctUntilChanged()
    }

    private companion object {
        private const val TAG = "SatelliteConnectionsRepo"
    }
}
