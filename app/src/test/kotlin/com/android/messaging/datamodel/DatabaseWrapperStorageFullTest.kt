package com.android.messaging.datamodel

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteFullException
import android.database.sqlite.SQLiteStatement
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.sms.SmsStorageStatusManager
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.DebugUtils
import com.android.messaging.util.UiUtils
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
internal class DatabaseWrapperStorageFullTest {

    private lateinit var context: Context
    private lateinit var applicationContext: Context
    private lateinit var database: SQLiteDatabase
    private lateinit var databaseWrapper: DatabaseWrapper
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        resources = mockk(relaxed = true)
        every { resources.getInteger(any()) } returns 0
        every { resources.getString(R.string.db_full) } returns "Database full"
        applicationContext = createResourceContext()
        installTestFactory(context = applicationContext)
        UiUtils.DEFAULT_INTERPOLATOR.hashCode()
        database = mockk(relaxed = true)
        databaseWrapper = DatabaseWrapper(context, database)
        mockkStatic(DebugUtils::class)
        mockkStatic(SmsStorageStatusManager::class)
        mockkStatic(UiUtils::class)
        every { DebugUtils.maybePlayDebugNoise(any(), any()) } just Runs
        every { SmsStorageStatusManager.handleStorageFull() } just Runs
        every { UiUtils.showToastAtBottom(R.string.db_full) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic(DebugUtils::class)
        unmockkStatic(SmsStorageStatusManager::class)
        unmockkStatic(UiUtils::class)
        FactoryTestAccess.reset()
    }

    @Test
    fun endTransaction_whenDatabaseIsFull_requestsStorageWarningOnce() {
        every { database.endTransaction() } throws SQLiteFullException()
        databaseWrapper.beginTransaction()

        databaseWrapper.endTransaction()

        verifyStorageWarningRequested()
    }

    @Test
    fun insertWithOnConflict_whenDatabaseIsFull_requestsStorageWarningOnce() {
        every {
            database.insertWithOnConflict(any(), any(), any(), any())
        } throws SQLiteFullException()

        databaseWrapper.insertWithOnConflict(
            "table",
            null,
            ContentValues(),
            SQLiteDatabase.CONFLICT_NONE,
        )

        verifyStorageWarningRequested()
    }

    @Test
    fun update_whenDatabaseIsFull_returnsZeroAndRequestsStorageWarningOnce() {
        every { database.update(any(), any(), any(), any()) } throws SQLiteFullException()

        val count = databaseWrapper.update("table", ContentValues(), null, null)

        assertEquals(0, count)
        verifyStorageWarningRequested()
    }

    @Test
    fun delete_whenDatabaseIsFull_returnsZeroAndRequestsStorageWarningOnce() {
        every { database.delete(any(), any(), any()) } throws SQLiteFullException()

        val count = databaseWrapper.delete("table", null, null)

        assertEquals(0, count)
        verifyStorageWarningRequested()
    }

    @Test
    fun insert_whenDatabaseIsFull_returnsMinusOneAndRequestsStorageWarningOnce() {
        every { database.insert(any(), any(), any()) } throws SQLiteFullException()

        val rowId = databaseWrapper.insert("table", null, ContentValues())

        assertEquals(-1L, rowId)
        verifyStorageWarningRequested()
    }

    @Test
    fun replace_whenDatabaseIsFull_returnsMinusOneAndRequestsStorageWarningOnce() {
        every { database.replace(any(), any(), any()) } throws SQLiteFullException()

        val rowId = databaseWrapper.replace("table", null, ContentValues())

        assertEquals(-1L, rowId)
        verifyStorageWarningRequested()
    }

    @Test
    fun execSqlWithBindArgs_whenDatabaseIsFull_requestsStorageWarningOnce() {
        every { database.execSQL(any(), any()) } throws SQLiteFullException()

        databaseWrapper.execSQL("DELETE FROM table", emptyArray())

        verifyStorageWarningRequested()
    }

    @Test
    fun execSql_whenDatabaseIsFull_requestsStorageWarningOnce() {
        every { database.execSQL(any<String>()) } throws SQLiteFullException()

        databaseWrapper.execSQL("DELETE FROM table")

        verifyStorageWarningRequested()
    }

    @Test
    fun execSqlUpdateDelete_whenDatabaseIsFull_returnsZeroAndRequestsStorageWarningOnce() {
        val statement = mockk<SQLiteStatement>()
        every { database.compileStatement(any()) } returns statement
        every { statement.executeUpdateDelete() } throws SQLiteFullException()

        val rowsUpdated = databaseWrapper.execSQLUpdateDelete("DELETE FROM table")

        assertEquals(0, rowsUpdated)
        verifyStorageWarningRequested()
    }

    private fun verifyStorageWarningRequested() {
        verify(exactly = 1) { SmsStorageStatusManager.handleStorageFull() }
    }

    private fun createResourceContext(): Context {
        return object : ContextWrapper(context) {
            override fun getResources(): Resources {
                return this@DatabaseWrapperStorageFullTest.resources
            }
        }
    }
}
