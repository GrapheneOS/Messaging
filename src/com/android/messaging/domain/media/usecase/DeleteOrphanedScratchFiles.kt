package com.android.messaging.domain.media.usecase

import android.content.Context
import androidx.core.net.toUri
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.LogUtil
import com.android.messaging.util.core.extension.unitFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

internal interface DeleteOrphanedScratchFiles {
    operator fun invoke(): Flow<Unit>
}

internal class DeleteOrphanedScratchFilesImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : DeleteOrphanedScratchFiles {

    override operator fun invoke(): Flow<Unit> {
        return unitFlow {
            val prefs = BuglePrefs.getApplicationPrefs()
            val nowMillis = System.currentTimeMillis()

            if (isCleanupDue(prefs = prefs, nowMillis = nowMillis)) {
                deleteExpiredUnreferencedFiles(nowMillis = nowMillis)
                prefs.putLong(BuglePrefsKeys.LAST_SCRATCH_FILE_CLEANUP_TIME, nowMillis)
            }
        }.catch { exception ->
            LogUtil.w(TAG, "Could not delete orphaned scratch files", exception)
        }.flowOn(messagingDbDispatcher)
    }

    private fun isCleanupDue(prefs: BuglePrefs, nowMillis: Long): Boolean {
        val lastCleanupMillis = prefs.getLong(
            BuglePrefsKeys.LAST_SCRATCH_FILE_CLEANUP_TIME,
            BuglePrefsKeys.LAST_SCRATCH_FILE_CLEANUP_TIME_DEFAULT,
        )

        return nowMillis - lastCleanupMillis !in 0 until CLEANUP_INTERVAL.inWholeMilliseconds
    }

    private fun deleteExpiredUnreferencedFiles(nowMillis: Long) {
        val expiredFiles = listExpiredFiles(nowMillis = nowMillis)

        if (expiredFiles.isNotEmpty()) {
            deleteUnreferencedFiles(files = expiredFiles)
        }
    }

    private fun listExpiredFiles(nowMillis: Long): List<File> {
        val expiryTimeMillis = nowMillis - GRACE_PERIOD.inWholeMilliseconds

        return MediaScratchFileProvider
            .getDirectory(context)
            .listFiles()
            .orEmpty()
            .filter { file -> file.isFile && file.lastModified() <= expiryTimeMillis }
    }

    private fun deleteUnreferencedFiles(files: List<File>) {
        val referencedFiles = queryReferencedFiles()
        val deletedFileCount = files
            .filterNot(referencedFiles::contains)
            .count(File::delete)

        if (deletedFileCount > 0) {
            LogUtil.i(TAG, "Deleted $deletedFileCount orphaned scratch files")
        }
    }

    private fun queryReferencedFiles(): Set<File> {
        val scratchUriPattern = "${MediaScratchFileProvider.getUriBuilder().build()}/%"

        return REFERENCING_COLUMNS.flatMapTo(mutableSetOf()) { (table, column) ->
            queryScratchFiles(
                table = table,
                column = column,
                scratchUriPattern = scratchUriPattern,
            )
        }
    }

    private fun queryScratchFiles(
        table: String,
        column: String,
        scratchUriPattern: String,
    ): List<File> {
        return DataModel.get().database.query(
            table,
            arrayOf(column),
            "$column LIKE ?",
            arrayOf(scratchUriPattern),
            null,
            null,
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    scratchFileOrNull(uriString = cursor.getString(0))?.let(::add)
                }
            }
        }
    }

    private fun scratchFileOrNull(uriString: String): File? {
        val uri = uriString.toUri()

        return when {
            MediaScratchFileProvider.isMediaScratchSpaceUri(uri) -> {
                MediaScratchFileProvider.getFileFromUri(uri)
            }

            else -> null
        }
    }

    private companion object {
        private const val TAG = "OrphanedScratchFiles"

        private val CLEANUP_INTERVAL = 1.days

        private val GRACE_PERIOD = 1.days

        private val REFERENCING_COLUMNS = listOf(
            DatabaseHelper.PARTS_TABLE to PartColumns.CONTENT_URI,
            DatabaseHelper.CONVERSATIONS_TABLE to ConversationColumns.PREVIEW_URI,
            DatabaseHelper.CONVERSATIONS_TABLE to ConversationColumns.DRAFT_PREVIEW_URI,
        )
    }
}
