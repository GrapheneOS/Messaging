package com.android.messaging.domain.conversation.usecase.avatar

import androidx.core.net.toUri
import com.android.messaging.util.AvatarUriUtil
import javax.inject.Inject

internal fun interface ResolveAvatarUri {
    operator fun invoke(icon: String?): String?
}

internal class ResolveAvatarUriImpl @Inject constructor() : ResolveAvatarUri {

    override operator fun invoke(icon: String?): String? {
        val iconUriString = icon?.takeIf(String::isNotBlank) ?: return null
        val iconUri = iconUriString.toUri()

        return when {
            AvatarUriUtil.isAvatarUri(iconUri) -> AvatarUriUtil.getPrimaryUri(iconUri)?.toString()
            else -> iconUriString
        }
    }
}
