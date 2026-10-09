package com.android.messaging.data.conversationstate

import com.android.messaging.data.participantdestination.BareNumberReader
import com.android.messaging.data.participantdestination.store.ParticipantDestinationPreferencesStore
import com.android.messaging.data.participantdestination.withReadings
import javax.inject.Inject

internal fun interface BlockedNumberRecorder {

    fun record(destinations: Set<String>)
}

internal class BlockedNumberRecorderImpl @Inject constructor(
    private val preferencesStore: ParticipantDestinationPreferencesStore,
    private val bareNumberReader: BareNumberReader,
) : BlockedNumberRecorder {

    override fun record(destinations: Set<String>) {
        preferencesStore.withLock {
            val history = preferencesStore.readSimHistory()
            val recordedReadings = withReadings(
                recordedReadings = history.recordedReadings,
                readings = bareNumberReader.readBareNumbersAmong(
                    destinations = destinations,
                    activeSubIds = bareNumberReader.readActiveSubIds(),
                    history = history,
                ),
                renormalizations = emptyList(),
            )
            if (recordedReadings != history.recordedReadings) {
                preferencesStore.saveSimHistory(
                    history = history.copy(recordedReadings = recordedReadings),
                )
            }
        }
    }
}
