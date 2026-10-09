package com.android.messaging.data.participantdestination

import android.content.Context
import com.android.messaging.data.participantdestination.model.BareNumberReading
import com.android.messaging.data.participantdestination.model.RecordedReading
import com.android.messaging.data.participantdestination.model.Renormalization
import com.android.messaging.data.participantdestination.model.SimHistory
import com.android.messaging.data.participantdestination.store.ParticipantDestinationDatabaseStore
import com.android.messaging.data.participantdestination.store.ParticipantDestinationPreferencesStore
import com.android.messaging.di.core.DatabaseVersion
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

internal interface ParticipantDestinationNormalizer {

    fun renormalizeIfPending(): Boolean

    fun onDatabaseCreated()

    fun onDatabaseUpgraded(oldVersion: Int)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Provider {
        fun participantDestinationNormalizer(): ParticipantDestinationNormalizer
    }

    companion object {

        @JvmStatic
        fun get(context: Context): ParticipantDestinationNormalizer {
            return EntryPointAccessors
                .fromApplication(context, Provider::class.java)
                .participantDestinationNormalizer()
        }
    }
}

internal class ParticipantDestinationNormalizerImpl @Inject constructor(
    private val preferencesStore: ParticipantDestinationPreferencesStore,
    private val databaseStore: ParticipantDestinationDatabaseStore,
    private val bareNumberReader: BareNumberReader,
    @param:DatabaseVersion
    private val databaseVersion: Int,
) : ParticipantDestinationNormalizer {

    override fun renormalizeIfPending(): Boolean {
        return preferencesStore.withLock {
            val normalizedSubIds = preferencesStore.readNormalizedSubIds()
            val activeSubIds = bareNumberReader.readActiveSubIds()
            val history = preferencesStore.readSimHistory()
            when {
                normalizedSubIds.containsAll(activeSubIds) -> {
                    recordBareNumbers(activeSubIds = activeSubIds, history = history)
                    false
                }
                else -> renormalizePending(
                    isRetry = normalizedSubIds.isNotEmpty(),
                    activeSubIds = activeSubIds,
                    history = history,
                )
            }
        }
    }

    override fun onDatabaseCreated() {
        preferencesStore.clearNormalization()
    }

    override fun onDatabaseUpgraded(oldVersion: Int) {
        if (preferencesStore.readNormalizedDatabaseVersion() > oldVersion) {
            preferencesStore.clearNormalization()
        }
    }

    private fun renormalizePending(
        isRetry: Boolean,
        activeSubIds: Set<Int>,
        history: SimHistory,
    ): Boolean {
        val lastParticipantId = when {
            isRetry -> preferencesStore.readNormalizedParticipantId()
            else -> databaseStore.readLastParticipantId()
        }
        val readings = bareNumberReader.readBareNumbers(
            activeSubIds = activeSubIds,
            history = history,
        )
        val renormalizations = renormalizationsOf(
            readings = readings,
            activeSubIds = activeSubIds,
            lastParticipantId = lastParticipantId,
        )
        databaseStore.applyRenormalizations(renormalizations = renormalizations)
        preferencesStore.saveNormalization(
            subIds = activeSubIds,
            databaseVersion = databaseVersion,
            lastParticipantId = lastParticipantId,
            history = SimHistory(
                lastActiveSubIds = activeSubIds,
                recordedReadings = readingsOfNumbersStillHeld(
                    recordedReadings = withReadings(
                        recordedReadings = history.recordedReadings,
                        readings = readings,
                        renormalizations = renormalizations,
                    ),
                ),
            ),
        )
        return renormalizations.isNotEmpty()
    }

    private fun renormalizationsOf(
        readings: List<BareNumberReading>,
        activeSubIds: Set<Int>,
        lastParticipantId: Long,
    ): List<Renormalization> {
        return readings
            .filter { reading ->
                reading.participant.participantId.toLong() <= lastParticipantId
            }
            .mapNotNull { reading ->
                bareNumberReader
                    .canonicalDestinationOf(
                        reading = reading,
                        activeSubIds = activeSubIds,
                    )
                    ?.let { canonicalDestination ->
                        Renormalization(
                            participant = reading.participant,
                            canonicalDestination = canonicalDestination,
                        )
                    }
            }
    }

    private fun recordBareNumbers(activeSubIds: Set<Int>, history: SimHistory) {
        val updatedHistory = SimHistory(
            lastActiveSubIds = activeSubIds.ifEmpty { history.lastActiveSubIds },
            recordedReadings = readingsOfNumbersStillHeld(
                recordedReadings = withReadings(
                    recordedReadings = history.recordedReadings,
                    readings = bareNumberReader.readBareNumbers(
                        activeSubIds = activeSubIds,
                        history = history,
                    ),
                    renormalizations = emptyList(),
                ),
            ),
        )
        if (updatedHistory != history) {
            preferencesStore.saveSimHistory(history = updatedHistory)
        }
    }

    private fun readingsOfNumbersStillHeld(
        recordedReadings: Map<String, RecordedReading>,
    ): Map<String, RecordedReading> {
        return when {
            recordedReadings.isEmpty() -> recordedReadings
            else -> {
                val senderDestinations = databaseStore.readSenderDestinations()
                recordedReadings.filter { (destination, reading) ->
                    destination in senderDestinations ||
                        reading.canonicalDestination in senderDestinations
                }
            }
        }
    }
}
