package com.android.messaging.domain.photoviewer.usecase

import android.content.ContentResolver
import android.net.Uri
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UriUtil
import com.android.messaging.util.core.extension.typedFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

internal interface PreparePhotoViewerSendUri {
    operator fun invoke(uri: Uri): Flow<Uri>
}

internal class PreparePhotoViewerSendUriImpl @Inject constructor(
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : PreparePhotoViewerSendUri {

    override fun invoke(uri: Uri): Flow<Uri> {
        return when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> copyFileUriToScratchSpace(uri = uri)
            else -> flowOf(uri)
        }
    }

    private fun copyFileUriToScratchSpace(uri: Uri): Flow<Uri> {
        return typedFlow {
            UriUtil
                .persistContentToScratchSpace(uri)
                .also { persistedUri ->
                    if (persistedUri != null) {
                        LogUtil.w(TAG, "Failed to copy photo viewer file URI to scratch space")
                    }
                }
        }.flowOn(context = ioDispatcher)
    }

    private companion object {
        private const val TAG = "PhotoViewerSendUri"
    }
}
