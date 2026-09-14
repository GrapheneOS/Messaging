package com.android.messaging.ui.common.components.participant

import java.nio.ByteBuffer

internal sealed interface ParticipantAvatarImage {

    data class Uri(
        val value: String,
    ) : ParticipantAvatarImage

    data class Bytes(
        val value: ByteBuffer,
    ) : ParticipantAvatarImage
}
