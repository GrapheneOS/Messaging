package com.android.messaging.datamodel

import android.content.ContentResolver
import android.net.Uri
import com.android.messaging.BuildConfig
import com.android.messaging.Factory
import com.android.messaging.util.LogUtil
import java.io.File
import java.io.IOException

class NotificationImageProvider : FileProvider() {

    override fun getFile(path: String, extension: String?): File? {
        return getFileWithExtension(path = path, extension = extension)
    }

    companion object {
        private const val NOTIFICATION_IMAGE_DIR = "notificationimages"
        private const val NOTIFICATION_IMAGE_EXTENSION = "jpg"
        private const val AUTHORITY =
            "${BuildConfig.APPLICATION_ID}.datamodel.NotificationImageProvider"

        @JvmStatic
        fun isNotificationImageUri(uri: Uri?): Boolean {
            val segments = uri?.pathSegments ?: return false

            return uri.scheme == ContentResolver.SCHEME_CONTENT &&
                uri.authority == AUTHORITY &&
                segments.size == 1 &&
                isValidFileId(segments[0])
        }

        @JvmStatic
        fun buildNotificationImageUri(): Uri? {
            val uri = buildFileUri(AUTHORITY, NOTIFICATION_IMAGE_EXTENSION)
            val file = getFileFromUri(uri)

            return when {
                file == null -> null
                ensureFileExists(file) -> uri
                else -> {
                    LogUtil.e(
                        LogUtil.BUGLE_TAG,
                        "Failed to create notification image ${file.absolutePath}",
                    )
                    null
                }
            }
        }

        @JvmStatic
        fun getFileFromUri(uri: Uri?): File? {
            return uri
                ?.path
                ?.let { path ->
                    getFileWithExtension(
                        path = path,
                        extension = getExtensionFromUri(uri),
                    )
                }
        }

        @JvmStatic
        fun listImageFiles(): Array<File> {
            return getDirectory().listFiles() ?: emptyArray()
        }

        private fun getFileWithExtension(path: String, extension: String?): File? {
            val directory = getDirectory()
            val fileName = when {
                extension.isNullOrEmpty() -> path
                else -> "$path.$extension"
            }

            val file = File(directory, fileName)

            return try {
                when {
                    file.canonicalPath.startsWith(directory.canonicalPath) -> file
                    else -> {
                        LogUtil.e(
                            LogUtil.BUGLE_TAG,
                            "getFileWithExtension: path ${file.canonicalPath} " +
                                "does not start with ${directory.canonicalPath}",
                        )
                        null
                    }
                }
            } catch (e: IOException) {
                LogUtil.e(LogUtil.BUGLE_TAG, "getFileWithExtension: getCanonicalPath failed ", e)
                null
            }
        }

        private fun getDirectory(): File {
            return File(Factory.get().applicationContext.cacheDir, NOTIFICATION_IMAGE_DIR)
        }
    }
}
