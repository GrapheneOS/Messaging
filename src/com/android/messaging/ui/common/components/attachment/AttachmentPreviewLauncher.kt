package com.android.messaging.ui.common.components.attachment

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.android.messaging.R
import com.android.messaging.ui.UIIntents
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UiUtils
import com.android.messaging.util.UriUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOG_TAG = "AttachmentPreviewLauncher"

internal suspend fun openAttachmentPreview(
    context: Context,
    contentUri: String,
    contentType: String,
) {
    val attachmentUri = contentUri.toUri()

    when {
        ContentType.isImageType(contentType) -> {
            openGenericAttachmentPreview(
                context = context,
                attachmentUri = attachmentUri,
                contentType = contentType,
            )
        }

        ContentType.isVideoType(contentType) -> {
            UIIntents.get().launchFullScreenVideoViewer(
                context,
                normalizeAttachmentUriForIntent(attachmentUri = attachmentUri),
            )
        }

        else -> {
            openGenericAttachmentPreview(
                context = context,
                attachmentUri = normalizeAttachmentUriForIntent(attachmentUri = attachmentUri),
                contentType = contentType,
            )
        }
    }
}

private fun openGenericAttachmentPreview(
    context: Context,
    attachmentUri: Uri,
    contentType: String,
) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(attachmentUri, contentType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    try {
        context.startActivity(intent)
    } catch (exception: ActivityNotFoundException) {
        LogUtil.w(LOG_TAG, "No activity found for the attachment preview intent", exception)
        UiUtils.showToastAtBottom(R.string.activity_not_found_message)
    } catch (exception: SecurityException) {
        LogUtil.w(LOG_TAG, "Not allowed to open the attachment", exception)
        UiUtils.showToastAtBottom(R.string.activity_not_found_message)
    }
}

private suspend fun normalizeAttachmentUriForIntent(
    attachmentUri: Uri,
): Uri {
    return when {
        attachmentUri.scheme != ContentResolver.SCHEME_FILE -> attachmentUri

        else -> {
            withContext(context = Dispatchers.IO) {
                UriUtil.persistContentToScratchSpace(attachmentUri) ?: attachmentUri
            }
        }
    }
}
