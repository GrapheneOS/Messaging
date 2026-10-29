package com.android.messaging.datamodel

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.testutil.installTestFactory
import io.mockk.unmockkAll
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BugleDatabaseOperationsArchiveStatusTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var db: DatabaseWrapper

    @Before
    fun setUp() {
        installTestFactory(context = context)
        db = createInMemoryActionSyncTestDatabase(context = context)
    }

    @After
    fun tearDown() {
        executor.shutdownNow()
        db.database.close()
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun archiving_unpinsOnlyTheArchivedConversation() {
        val archivedConversationId = insertPinnedConversation()
        val otherConversationId = insertPinnedConversation()

        setArchived(conversationId = archivedConversationId, isArchived = true)

        assertEquals(
            ConversationFlags(isArchived = true, isPinned = false),
            flagsOf(archivedConversationId),
        )
        assertEquals(
            ConversationFlags(isArchived = false, isPinned = true),
            flagsOf(otherConversationId),
        )
    }

    @Test
    fun unarchiving_leavesTheConversationUnpinned() {
        val conversationId = insertPinnedConversation()
        setArchived(conversationId = conversationId, isArchived = true)

        setArchived(conversationId = conversationId, isArchived = false)

        assertEquals(
            ConversationFlags(isArchived = false, isPinned = false),
            flagsOf(conversationId),
        )
    }

    private fun insertPinnedConversation(): String {
        return db.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(ConversationColumns.PINNED to 1),
        ).toString()
    }

    private fun setArchived(conversationId: String, isArchived: Boolean) {
        executor.submit(
            Callable {
                db.beginTransaction()
                try {
                    BugleDatabaseOperations.updateConversationArchiveStatusInTransaction(
                        db,
                        conversationId,
                        isArchived,
                    )
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            },
        ).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun flagsOf(conversationId: String): ConversationFlags {
        return db.rawQuery(
            "SELECT ${ConversationColumns.ARCHIVE_STATUS}, ${ConversationColumns.PINNED} " +
                "FROM ${DatabaseHelper.CONVERSATIONS_TABLE} WHERE ${ConversationColumns._ID}=?",
            arrayOf(conversationId),
        ).use { cursor ->
            cursor.moveToFirst()
            ConversationFlags(isArchived = cursor.getInt(0) == 1, isPinned = cursor.getInt(1) == 1)
        }
    }

    private data class ConversationFlags(
        val isArchived: Boolean,
        val isPinned: Boolean,
    )

    private companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
