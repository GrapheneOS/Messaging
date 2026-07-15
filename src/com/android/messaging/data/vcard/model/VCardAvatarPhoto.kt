package com.android.messaging.data.vcard.model

import androidx.compose.runtime.Immutable
import java.nio.ByteBuffer

@Immutable
internal class VCardAvatarPhoto(
    bytes: ByteArray,
) {
    private val bytes = bytes.copyOf()

    fun asReadOnlyByteBuffer(): ByteBuffer {
        return ByteBuffer.wrap(bytes).asReadOnlyBuffer()
    }

    fun toByteArray(): ByteArray {
        return bytes.copyOf()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCardAvatarPhoto) return false

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}
