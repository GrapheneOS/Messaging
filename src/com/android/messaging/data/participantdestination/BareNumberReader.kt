package com.android.messaging.data.participantdestination

import com.android.messaging.data.participantdestination.model.BareNumberParticipant
import com.android.messaging.data.participantdestination.model.BareNumberReading
import com.android.messaging.data.participantdestination.model.SimHistory
import com.android.messaging.data.participantdestination.store.ParticipantDestinationDatabaseStore
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject

internal interface BareNumberReader {

    fun readActiveSubIds(): Set<Int>

    fun readBareNumbers(activeSubIds: Set<Int>, history: SimHistory): List<BareNumberReading>

    fun canonicalDestinationOf(reading: BareNumberReading, activeSubIds: Set<Int>): String?
}

internal class BareNumberReaderImpl @Inject constructor(
    private val databaseStore: ParticipantDestinationDatabaseStore,
) : BareNumberReader {

    override fun readActiveSubIds(): Set<Int> {
        return PhoneUtils.getDefault().activeSubscriptionInfoList.mapTo(mutableSetOf()) { info ->
            info.subscriptionId
        }
    }

    override fun readBareNumbers(
        activeSubIds: Set<Int>,
        history: SimHistory,
    ): List<BareNumberReading> {
        return readingsOf(
            participants = databaseStore.readBareNumberParticipants(),
            activeSubIds = activeSubIds,
            history = history,
        )
    }

    override fun canonicalDestinationOf(
        reading: BareNumberReading,
        activeSubIds: Set<Int>,
    ): String? {
        val destination = reading.participant.destination
        val readingSubIds = reading.readingSubIds
        val canonicalDestinations = when {
            activeSubIds.containsAll(readingSubIds) ->
                readingSubIds.mapTo(mutableSetOf()) { subId ->
                    PhoneUtils.get(subId).getCanonicalBySimLocale(destination)
                }
            else -> emptySet()
        }

        return canonicalDestinations.singleOrNull()?.takeIf { canonicalDestination ->
            canonicalDestination != destination
        }
    }

    private fun readingsOf(
        participants: List<BareNumberParticipant>,
        activeSubIds: Set<Int>,
        history: SimHistory,
    ): List<BareNumberReading> {
        val subIdsByParticipant = when {
            participants.isEmpty() -> emptyMap()
            else -> databaseStore.readSubIdsByParticipant()
        }

        val recentSubIds = activeSubIds + history.lastActiveSubIds

        return participants.map { participant ->
            BareNumberReading(
                participant = participant,
                readingSubIds = readingSubIdsOf(
                    subIds = subIdsByParticipant[participant.participantId].orEmpty(),
                    earlierReadingSubIds = history.recordedReadings[participant.destination]
                        ?.subIds
                        .orEmpty(),
                    recentSubIds = recentSubIds,
                ),
            )
        }
    }

    private fun readingSubIdsOf(
        subIds: Set<Int>,
        earlierReadingSubIds: Set<Int>,
        recentSubIds: Set<Int>,
    ): Set<Int> {
        val knownSubIds = subIds - ParticipantData.DEFAULT_SELF_SUB_ID + earlierReadingSubIds

        return when {
            isEverySimKnown(subIds = subIds) -> knownSubIds
            else -> knownSubIds + recentSubIds
        }
    }

    private fun isEverySimKnown(subIds: Set<Int>): Boolean {
        return subIds.isNotEmpty() && ParticipantData.DEFAULT_SELF_SUB_ID !in subIds
    }
}
