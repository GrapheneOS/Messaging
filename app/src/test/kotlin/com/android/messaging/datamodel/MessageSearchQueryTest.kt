package com.android.messaging.datamodel

import android.content.pm.ProviderInfo
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.installTestFactory
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class MessageSearchQueryTest {

    private val context get() = RuntimeEnvironment.getApplication().applicationContext

    @Before
    fun setUp() {
        installTestFactory(context = context)
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun search_matchesTextAndJoinsConversationAndSenderNames() {
        withDatabase { db ->
            installProvider(db)
            val senderId = db.insertParticipant(
                normalizedDestination = "+15550001",
                fullName = "Sam",
            )
            val conversationId = db.insertConversation(name = "Road trip")
            db.insertMessageWithPart(
                conversationId = conversationId,
                senderId = senderId,
                receivedTimestamp = 1_700_000_000_000L,
                text = "Meet at the diner",
            )

            val rows = search("DINER")

            assertEquals(listOf("Meet at the diner"), rows.map { it.snippet })
            assertEquals(listOf("Road trip"), rows.map { it.conversationName })
            assertEquals(listOf("Sam"), rows.map { it.senderName })
        }
    }

    @Test
    fun search_ordersNewestMatchFirst() {
        withDatabase { db ->
            installProvider(db)
            val senderId = db.insertParticipant()
            val conversationId = db.insertConversation()
            db.insertMessageWithPart(conversationId, senderId, 1_000L, "old match")
            db.insertMessageWithPart(conversationId, senderId, 3_000L, "newer match")
            db.insertMessageWithPart(conversationId, senderId, 2_000L, "middle match")

            assertEquals(
                listOf("newer match", "middle match", "old match"),
                search("match").map { it.snippet },
            )
        }
    }

    @Test
    fun search_excludesNonMatchingTextAcrossAllConversations() {
        withDatabase { db ->
            installProvider(db)
            val senderId = db.insertParticipant()
            val conversationId = db.insertConversation()
            val otherConversationId = db.insertConversation()
            db.insertMessageWithPart(conversationId, senderId, 1_000L, "needle here")
            db.insertMessageWithPart(otherConversationId, senderId, 2_000L, "needle there")
            db.insertMessageWithPart(conversationId, senderId, 3_000L, "haystack only")

            // Search spans every conversation, newest match first, and skips non-matching text.
            assertEquals(
                listOf("needle there", "needle here"),
                search("needle").map { it.snippet },
            )
        }
    }

    @Test
    fun search_ignoresCaseOutsideAscii() {
        withDatabase { db ->
            installProvider(db)
            val senderId = db.insertParticipant()
            val conversationId = db.insertConversation()
            db.insertMessageWithPart(conversationId, senderId, 1_000L, "Встреча в кафе")
            db.insertMessageWithPart(conversationId, senderId, 2_000L, "Καλημέρα ΣΟΦΙΑ")
            db.insertMessageWithPart(conversationId, senderId, 3_000L, "Café ÉCLAIR")

            assertEquals(listOf("Встреча в кафе"), search("ВСТРЕЧА").map { it.snippet })
            assertEquals(listOf("Καλημέρα ΣΟΦΙΑ"), search("σοφια").map { it.snippet })
            assertEquals(listOf("Café ÉCLAIR"), search("éclair").map { it.snippet })
        }
    }

    @Test
    fun search_treatsRegexCharactersLiterally() {
        withDatabase { db ->
            installProvider(db)
            val senderId = db.insertParticipant()
            val conversationId = db.insertConversation()
            db.insertMessageWithPart(conversationId, senderId, 1_000L, "costs $5 (maybe)")
            db.insertMessageWithPart(conversationId, senderId, 2_000L, "anything at all")

            assertEquals(listOf("costs $5 (maybe)"), search("$5 (").map { it.snippet })
            assertEquals(emptyList<String>(), search(".*").map { it.snippet })
        }
    }

    @Test
    fun search_treatsLikeWildcardsLiterally() {
        withDatabase { db ->
            installProvider(db)
            val senderId = db.insertParticipant()
            val conversationId = db.insertConversation()
            db.insertMessageWithPart(conversationId, senderId, 1_000L, "100% done")
            db.insertMessageWithPart(conversationId, senderId, 2_000L, "a_b")
            db.insertMessageWithPart(conversationId, senderId, 3_000L, "no wildcards")

            // '%' and '_' in the query must match only their literal characters.
            assertEquals(listOf("100% done"), search("%").map { it.snippet })
            assertEquals(listOf("a_b"), search("_").map { it.snippet })
        }
    }

    @Test
    fun search_excludesDrafts() {
        withDatabase { db ->
            installProvider(db)
            val senderId = db.insertParticipant()
            val conversationId = db.insertConversation()
            db.insertMessageWithPart(
                conversationId = conversationId,
                senderId = senderId,
                receivedTimestamp = 1_000L,
                text = "needle draft",
                status = MessageData.BUGLE_STATUS_OUTGOING_DRAFT,
            )
            db.insertMessageWithPart(conversationId, senderId, 2_000L, "needle real")

            assertEquals(listOf("needle real"), search("needle").map { it.snippet })
        }
    }

    private fun withDatabase(block: (SQLiteDatabase) -> Unit) {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            block(db)
        }
    }

    private fun installProvider(db: SQLiteDatabase) {
        val provider = MessagingContentProvider()
        provider.attachInfo(
            context,
            ProviderInfo().apply { authority = MessagingContentProvider.AUTHORITY },
        )
        ReflectionHelpers.setField(provider, "mDatabaseWrapper", DatabaseWrapper(context, db))
        ShadowContentResolver.registerProviderInternal(MessagingContentProvider.AUTHORITY, provider)
    }

    private fun search(query: String): List<SearchRow> {
        val cursor = checkNotNull(
            context.contentResolver.query(
                MessagingContentProvider.buildSearchUri(query),
                null,
                null,
                null,
                null,
            ),
        )

        return cursor.use {
            generateSequence { cursor.takeIf { row -> row.moveToNext() } }
                .map { row ->
                    SearchRow(
                        snippet = row.getString(
                            row.getColumnIndexOrThrow(MessageSearchQuery.SNIPPET),
                        ),
                        conversationName = row.getString(
                            row.getColumnIndexOrThrow(MessageSearchQuery.CONVERSATION_NAME),
                        ),
                        senderName = row.getString(
                            row.getColumnIndexOrThrow(MessageSearchQuery.SENDER_FULL_NAME),
                        ),
                    )
                }
                .toList()
        }
    }

    private fun SQLiteDatabase.insertParticipant(
        normalizedDestination: String = "+15550001",
        fullName: String = "Sam",
    ): String {
        return insertOrThrow(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.NORMALIZED_DESTINATION to normalizedDestination,
                ParticipantColumns.FULL_NAME to fullName,
            ),
        ).toString()
    }

    private fun SQLiteDatabase.insertConversation(name: String = "Conversation"): String {
        return insertOrThrow(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(ConversationColumns.NAME to name),
        ).toString()
    }

    private fun SQLiteDatabase.insertMessageWithPart(
        conversationId: String,
        senderId: String,
        receivedTimestamp: Long,
        text: String,
        status: Int = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
    ) {
        val messageId = insertOrThrow(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to senderId,
                MessageColumns.RECEIVED_TIMESTAMP to receivedTimestamp,
                MessageColumns.STATUS to status,
            ),
        )
        insertOrThrow(
            DatabaseHelper.PARTS_TABLE,
            null,
            contentValuesOf(
                PartColumns.MESSAGE_ID to messageId,
                PartColumns.CONVERSATION_ID to conversationId,
                PartColumns.TEXT to text,
                PartColumns.CONTENT_TYPE to "text/plain",
            ),
        )
    }

    private data class SearchRow(
        val snippet: String,
        val conversationName: String,
        val senderName: String,
    )
}
