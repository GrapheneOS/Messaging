package com.android.messaging.data.extension

import android.net.Uri
import android.provider.ContactsContract.Contacts

private const val LOOKUP_KEY_PATH_SEGMENT_INDEX = 2
private const val MULTI_VCARD_LOOKUP_KEY_SEPARATOR = ":"

internal fun Uri.isContactVCardUri(): Boolean {
    return matchesVCardUri(Contacts.CONTENT_VCARD_URI) ||
        matchesVCardUri(Contacts.CONTENT_MULTI_VCARD_URI)
}

internal fun Uri.excludesVCardPhotos(): Boolean {
    return getBooleanQueryParameter(Contacts.QUERY_PARAMETER_VCARD_NO_PHOTO, false)
}

internal fun Uri.contactLookupUrisFromVCardUri(): List<Uri> {
    val lookupKeys = when {
        matchesVCardUri(Contacts.CONTENT_VCARD_URI) -> {
            listOfNotNull(pathSegments.getOrNull(LOOKUP_KEY_PATH_SEGMENT_INDEX))
        }

        matchesVCardUri(Contacts.CONTENT_MULTI_VCARD_URI) -> {
            pathSegments
                .getOrNull(LOOKUP_KEY_PATH_SEGMENT_INDEX)
                ?.split(MULTI_VCARD_LOOKUP_KEY_SEPARATOR)
                ?.map(Uri::decode)
                .orEmpty()
        }

        else -> emptyList()
    }

    return lookupKeys
        .filter { it.isNotBlank() }
        .map { lookupKey ->
            Contacts.CONTENT_LOOKUP_URI
                .buildUpon()
                .appendPath(lookupKey)
                .build()
        }
}

private fun Uri.matchesVCardUri(base: Uri): Boolean {
    return scheme == base.scheme &&
        authority == base.authority &&
        pathSegments.take(base.pathSegments.size) == base.pathSegments
}
