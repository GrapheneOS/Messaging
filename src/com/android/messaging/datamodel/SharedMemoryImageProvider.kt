package com.android.messaging.datamodel

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SharedMemory
import android.system.OsConstants
import com.android.messaging.BuildConfig
import com.android.messaging.datamodel.media.MediaResourceManager
import com.android.messaging.datamodel.media.UriImageRequestDescriptor
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException

class SharedMemoryImageProvider : FileProvider() {
    companion object {
        private val TAG = Companion::class.java.simpleName

        const val AUTHORITY: String =
            BuildConfig.APPLICATION_ID + ".datamodel.SharedMemoryImageProvider"

        /**
         * Returns a uri that can be used to access an image.
         *
         * @return the URI for an image
         */
        fun buildUri(imageRequestUri: Uri, mimeType: String): Uri? {
            if (!ContentType.isImageType(mimeType)) {
                return null
            }

            return Uri.Builder()
                .authority(AUTHORITY)
                .scheme(ContentResolver.SCHEME_CONTENT)
                .appendPath(imageRequestUri.toString())
                .build()
        }
    }

    override fun getFile(path: String, extension: String): File? = null

    override fun openFile(uri: Uri, fileMode: String): ParcelFileDescriptor? {
        val imageRequestUri = uri.path?.replaceFirst("/", "") ?: return null
        val imageDescriptor = UriImageRequestDescriptor(Uri.parse(imageRequestUri))
        val imageRequest = imageDescriptor.buildSyncMediaRequest(context)
        val imageResource = MediaResourceManager.get().requestMediaResourceSync(imageRequest)
        if (imageResource != null) {
            try {
                val byteStream = ByteArrayOutputStream()
                imageResource.bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream)

                SharedMemory.create(uri.toString(), byteStream.size()).use {
                    val byteBuffer = it.mapReadWrite()
                    byteBuffer.put(byteStream.toByteArray())
                    SharedMemory.unmap(byteBuffer)
                    it.setProtect(OsConstants.PROT_READ)
                    return it.getParcelFileDescriptor()
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, "Failed to open image", e)
                throw FileNotFoundException()
            } finally {
                imageResource.release()
            }
        }
        return null
    }

    override fun getType(uri: Uri): String {
        return ContentType.IMAGE_PNG
    }
}

private fun SharedMemory.getParcelFileDescriptor(): ParcelFileDescriptor? {
    val getFdDupMethod = SharedMemory::class.java.getMethod("getFdDup")
    return getFdDupMethod.invoke(this) as ParcelFileDescriptor?
}
