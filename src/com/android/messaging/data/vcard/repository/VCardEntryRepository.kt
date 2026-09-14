package com.android.messaging.data.vcard.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.provider.ContactsContract.Contacts
import androidx.core.net.toUri
import com.android.messaging.data.vcard.parser.VCardParser
import com.android.messaging.datamodel.media.CustomVCardEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface VCardEntryRepository {
    fun observeEntries(
        vCardUri: String,
        refreshes: Flow<Unit> = emptyFlow(),
    ): Flow<List<CustomVCardEntry>>

    suspend fun getEntries(vCardUri: String): List<CustomVCardEntry>
}

@Singleton
internal class VCardEntryRepositoryImpl @Inject constructor(
    private val parser: VCardParser,
    private val contentResolver: ContentResolver,
) : VCardEntryRepository {

    private val lock = Mutex()
    private val cachedEntries = object : LinkedHashMap<String, List<CustomVCardEntry>>(
        MAX_CACHED_VCARDS,
        LOAD_FACTOR,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<CustomVCardEntry>>,
        ): Boolean {
            return size > MAX_CACHED_VCARDS
        }
    }

    override fun observeEntries(
        vCardUri: String,
        refreshes: Flow<Unit>,
    ): Flow<List<CustomVCardEntry>> {
        if (vCardUri.isBlank()) {
            return flowOf(emptyList())
        }

        return refreshEvents(
            vCardUri,
            refreshes,
        ).map {
            getEntries(vCardUri)
        }
    }

    override suspend fun getEntries(vCardUri: String): List<CustomVCardEntry> {
        if (vCardUri.isBlank()) {
            return emptyList()
        }

        val shouldCache = shouldCache(vCardUri)
        val cached = when {
            shouldCache -> cached(vCardUri)
            else -> null
        }

        return cached ?: parser.parse(vCardUri).also { entries ->
            cacheIfNeeded(
                vCardUri = vCardUri,
                entries = entries,
                shouldCache = shouldCache,
            )
        }
    }

    private suspend fun cacheIfNeeded(
        vCardUri: String,
        entries: List<CustomVCardEntry>,
        shouldCache: Boolean,
    ) {
        if (!shouldCache || entries.isEmpty()) {
            return
        }

        lock.withLock {
            cachedEntries[vCardUri] = entries
        }
    }

    private fun refreshEvents(
        vCardUri: String,
        refreshes: Flow<Unit>,
    ): Flow<Unit> {
        return when {
            vCardUri.toUri().isContactVCardUri() -> contactChangeEvents().conflate()
            else -> merge(flowOf(Unit), refreshes)
        }
    }

    private fun contactChangeEvents(): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }

            contentResolver.registerContentObserver(Contacts.CONTENT_URI, true, observer)
            trySend(Unit)

            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    private fun shouldCache(vCardUri: String): Boolean {
        return !vCardUri.toUri().isContactVCardUri()
    }

    private fun Uri.isContactVCardUri(): Boolean {
        return matchesVCardUri(Contacts.CONTENT_VCARD_URI) ||
            matchesVCardUri(Contacts.CONTENT_MULTI_VCARD_URI)
    }

    private fun Uri.matchesVCardUri(base: Uri): Boolean {
        return scheme == base.scheme &&
            authority == base.authority &&
            pathSegments.take(base.pathSegments.size) == base.pathSegments
    }

    private suspend fun cached(vCardUri: String): List<CustomVCardEntry>? {
        return lock.withLock {
            cachedEntries[vCardUri]
        }
    }

    private companion object {
        private const val MAX_CACHED_VCARDS = 5
        private const val LOAD_FACTOR = 0.75f
    }
}
