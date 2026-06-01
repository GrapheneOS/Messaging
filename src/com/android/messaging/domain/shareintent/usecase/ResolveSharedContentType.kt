package com.android.messaging.domain.shareintent.usecase

import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.android.messaging.util.LogUtil
import com.android.messaging.util.MediaMetadataRetrieverWrapper
import java.io.IOException
import javax.inject.Inject

internal interface ResolveSharedContentType {
    operator fun invoke(uri: Uri?, fallbackType: String?): String?
}

internal class ResolveSharedContentTypeImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : ResolveSharedContentType {

    override fun invoke(uri: Uri?, fallbackType: String?): String? {
        if (uri == null) {
            return fallbackType
        }

        // First try looking at file extension. This is less reliable in some ways but it's
        // recommended by
        // https://developer.android.com/training/secure-file-sharing/retrieve-info.html
        // Some implementations of MediaMetadataRetriever get things horribly wrong for common
        // formats such as jpeg (reports as video/ffmpeg).
        return contentResolver.getType(uri) ?: extractFromMetadata(uri, fallbackType)
    }

    private fun extractFromMetadata(uri: Uri, fallbackType: String?): String? {
        val retriever = MediaMetadataRetrieverWrapper()

        return try {
            retriever.setDataSource(uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: fallbackType
        } catch (e: IOException) {
            LogUtil.i(LogUtil.BUGLE_TAG, "Could not determine type of $uri", e)
            fallbackType
        } finally {
            retriever.release()
        }
    }
}
