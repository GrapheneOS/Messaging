package com.android.messaging.data.vcard.repository

import com.android.messaging.data.vcard.parser.VCardParser
import com.android.messaging.datamodel.media.CustomVCardEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

internal interface VCardEntryRepository {
    suspend fun getEntries(vCardUri: String): List<CustomVCardEntry>
}

@Singleton
internal class VCardEntryRepositoryImpl @Inject constructor(
    private val parser: VCardParser,
) : VCardEntryRepository {

    private val lock = Mutex()
    private val pendingParseResults =
        mutableMapOf<String, CompletableDeferred<List<CustomVCardEntry>>>()
    private val cachedEntries = object : LinkedHashMap<String, List<CustomVCardEntry>>(
        CACHE_SIZE,
        LOAD_FACTOR,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<CustomVCardEntry>>,
        ): Boolean {
            return size > CACHE_SIZE
        }
    }

    override suspend fun getEntries(vCardUri: String): List<CustomVCardEntry> {
        if (vCardUri.isBlank()) {
            return emptyList()
        }

        cached(vCardUri)?.let { entries ->
            return entries
        }

        val parseHandle = getOrCreateParseHandle(vCardUri)
        if (!parseHandle.isOwner) {
            return parseHandle.deferred.await()
        }

        return try {
            val entries = parser.parse(vCardUri)
            if (entries.isNotEmpty()) {
                lock.withLock {
                    cachedEntries[vCardUri] = entries
                }
            }
            parseHandle.deferred.complete(entries)

            entries
        } catch (throwable: Exception) {
            parseHandle.deferred.completeExceptionally(throwable)
            throw throwable
        } finally {
            clearParseHandle(vCardUri, parseHandle.deferred)
        }
    }

    private suspend fun cached(vCardUri: String): List<CustomVCardEntry>? {
        return lock.withLock {
            cachedEntries[vCardUri]
        }
    }

    private suspend fun getOrCreateParseHandle(vCardUri: String): ParseHandle {
        return lock.withLock {
            val existing = pendingParseResults[vCardUri]
            if (existing != null) {
                return@withLock ParseHandle(
                    deferred = existing,
                    isOwner = false,
                )
            }

            CompletableDeferred<List<CustomVCardEntry>>().let { deferred ->
                pendingParseResults[vCardUri] = deferred
                ParseHandle(
                    deferred = deferred,
                    isOwner = true,
                )
            }
        }
    }

    private suspend fun clearParseHandle(
        vCardUri: String,
        deferred: CompletableDeferred<List<CustomVCardEntry>>,
    ) {
        lock.withLock {
            if (pendingParseResults[vCardUri] === deferred) {
                pendingParseResults.remove(vCardUri)
            }
        }
    }

    private data class ParseHandle(
        val deferred: CompletableDeferred<List<CustomVCardEntry>>,
        val isOwner: Boolean,
    )

    private companion object {
        private const val CACHE_SIZE = 5
        private const val LOAD_FACTOR = 0.75f
    }
}
