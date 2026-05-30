package com.android.messaging.ui.blockedparticipants.screen.model

internal sealed interface BlockedParticipantsNavEvent {

    data object CloseAfterLastUnblock : BlockedParticipantsNavEvent
}
