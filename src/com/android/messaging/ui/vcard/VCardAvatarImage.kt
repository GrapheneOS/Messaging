package com.android.messaging.ui.vcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.messaging.data.vcard.model.VCardAvatarPhoto
import com.android.messaging.ui.common.components.participant.ParticipantAvatarImage

@Composable
internal fun rememberVCardAvatarImage(
    avatarPhoto: VCardAvatarPhoto?,
): ParticipantAvatarImage? {
    return remember(avatarPhoto) {
        avatarPhoto
            ?.asReadOnlyByteBuffer()
            ?.let(ParticipantAvatarImage::Bytes)
    }
}
