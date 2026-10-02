package com.android.messaging.sms

import android.content.ContentResolver
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.android.messaging.FactoryTestAccess
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * MmsSmsProvider runs thread message queries without bind arguments, so the cutoff only reaches
 * SQLite when it is part of the selection itself.
 */
@RunWith(RobolectricTestRunner::class)
class MmsUtilsThreadMessagesTest {

    private val database = SQLiteDatabase.create(null)
    private val contentResolver = mockk<ContentResolver>()
    private val context = mockk<Context>()

    @Before
    fun setUp() {
        database.execSQL("CREATE TABLE sms (_id INTEGER PRIMARY KEY, thread_id, date)")
        database.execSQL("INSERT INTO sms (thread_id, date) VALUES ($THREAD_ID, $MESSAGE_DATE)")
        every { context.contentResolver } returns contentResolver
        every { contentResolver.query(any(), any(), any(), any(), any()) } answers {
            val threadId = firstArg<Uri>().lastPathSegment
            val selection = thirdArg<String>()
            database.rawQuery(
                "SELECT _id FROM sms WHERE thread_id = $threadId AND ($selection)",
                emptyArray(),
            )
        }
        installTestFactory(context = context)
    }

    @After
    fun tearDown() {
        database.close()
        FactoryTestAccess.reset()
    }

    @Test
    fun messageUpToTheCutoffIsFound() {
        assertTrue(MmsUtils.hasThreadMessages(THREAD_ID, MESSAGE_DATE))
    }

    @Test
    fun messageAfterTheCutoffIsIgnored() {
        assertFalse(MmsUtils.hasThreadMessages(THREAD_ID, MESSAGE_DATE - 1))
    }

    private companion object {
        private const val THREAD_ID = 42L
        private const val MESSAGE_DATE = 1_790_000_000_000L
    }
}
