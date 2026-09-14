package com.android.messaging.domain.conversation.usecase.participant

import com.android.messaging.datamodel.data.ParticipantData
import javax.inject.Inject

internal fun interface CanAddContact {
    operator fun invoke(
        isGroup: Boolean,
        lookupKey: String?,
        destination: String?,
    ): Boolean
}

internal class CanAddContactImpl @Inject constructor() : CanAddContact {

    override operator fun invoke(
        isGroup: Boolean,
        lookupKey: String?,
        destination: String?,
    ): Boolean {
        return !isGroup &&
            lookupKey.isNullOrBlank() &&
            !destination.isNullOrBlank() &&
            destination != ParticipantData.getUnknownSenderDestination()
    }
}
