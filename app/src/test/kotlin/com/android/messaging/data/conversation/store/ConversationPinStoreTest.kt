package com.android.messaging.data.conversation.store

import android.database.MatrixCursor
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.MessagingContentProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationPinStoreTest {

    private val databaseWrapper = mockk<DatabaseWrapper>(relaxed = true)
    private val dataModel = mockk<DataModel>()

    private val store = ConversationPinStoreImpl()

    @Before
    fun setUp() {
        mockkStatic(DataModel::class)
        mockkStatic(BugleDatabaseOperations::class)
        mockkStatic(MessagingContentProvider::class)

        every { DataModel.get() } returns dataModel
        every { dataModel.database } returns databaseWrapper
        every {
            BugleDatabaseOperations.updateConversationPinStatusInTransaction(any(), any(), any())
        } just runs
        every { MessagingContentProvider.notifyConversationListChanged() } just runs
        every { MessagingContentProvider.notifyConversationMetadataChanged(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun isConversationPinned_readsThePinnedColumnOfTheConversation() {
        stubPinnedQuery(pinnedValues = listOf(1))

        assertTrue(store.isConversationPinned(conversationId = CONVERSATION_ID))
        verify(exactly = 1) {
            databaseWrapper.query(
                DatabaseHelper.CONVERSATIONS_TABLE,
                arrayOf(ConversationColumns.PINNED),
                "${ConversationColumns._ID}=?",
                arrayOf(CONVERSATION_ID.value),
                null,
                null,
                null,
            )
        }
    }

    @Test
    fun isConversationPinned_unpinnedOrMissingConversation_returnsFalse() {
        stubPinnedQuery(pinnedValues = listOf(0))
        assertFalse(store.isConversationPinned(conversationId = CONVERSATION_ID))

        stubPinnedQuery(pinnedValues = emptyList())
        assertFalse(store.isConversationPinned(conversationId = CONVERSATION_ID))
    }

    @Test
    fun pinConversation_updatesPinStatusToTrueInsideTransactionAndNotifies() {
        store.pinConversation(CONVERSATION_ID)

        verifyOrder {
            databaseWrapper.beginTransaction()
            BugleDatabaseOperations.updateConversationPinStatusInTransaction(
                databaseWrapper,
                CONVERSATION_ID.value,
                true,
            )
            databaseWrapper.setTransactionSuccessful()
            databaseWrapper.endTransaction()
        }
        verify(exactly = 1) {
            MessagingContentProvider.notifyConversationListChanged()
        }
        verify(exactly = 1) {
            MessagingContentProvider.notifyConversationMetadataChanged(CONVERSATION_ID.value)
        }
    }

    @Test
    fun unpinConversation_updatesPinStatusToFalse() {
        store.unpinConversation(conversationId = CONVERSATION_ID)

        verify(exactly = 1) {
            BugleDatabaseOperations.updateConversationPinStatusInTransaction(
                databaseWrapper,
                CONVERSATION_ID.value,
                false,
            )
        }
    }

    @Test
    fun pinConversation_updateFails_endsTransactionWithoutNotifying() {
        val failure = IllegalStateException("update failed")
        every {
            BugleDatabaseOperations.updateConversationPinStatusInTransaction(any(), any(), any())
        } throws failure

        val thrown = assertThrows(IllegalStateException::class.java) {
            store.pinConversation(CONVERSATION_ID)
        }

        assertSame(failure, thrown)
        verify(exactly = 1) { databaseWrapper.endTransaction() }
        verify(exactly = 0) { databaseWrapper.setTransactionSuccessful() }
        verify(exactly = 0) { MessagingContentProvider.notifyConversationListChanged() }
        verify(exactly = 0) {
            MessagingContentProvider.notifyConversationMetadataChanged(any())
        }
    }

    private fun stubPinnedQuery(pinnedValues: List<Int>) {
        val cursor = MatrixCursor(arrayOf(ConversationColumns.PINNED))
        pinnedValues.forEach { pinnedValue -> cursor.addRow(arrayOf(pinnedValue)) }

        every {
            databaseWrapper.query(any(), any(), any(), any(), any(), any(), any())
        } returns cursor
    }

    private companion object {
        private val CONVERSATION_ID = ConversationId("conversation-42")
    }
}
