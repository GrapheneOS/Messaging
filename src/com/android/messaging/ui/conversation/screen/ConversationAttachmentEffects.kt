package com.android.messaging.ui.conversation.screen

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.core.net.toUri
import com.android.messaging.R
import com.android.messaging.ui.common.components.attachment.openAttachmentPreview
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.model.PhotoViewerSourceBounds
import com.android.messaging.util.ContentType
import com.android.messaging.util.UriUtil
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal suspend fun openAttachmentPreviewEffect(
    context: Context,
    hostBoundsState: State<ComposeRect?>,
    effect: ConversationScreenEffect.OpenAttachmentPreview,
    onNavigateToPhotoViewer: (PhotoViewerLaunchRequest) -> Unit,
) {
    val imageCollectionUri = effect.imageCollectionUri
    val isPhotoViewerAttachment = ContentType.isImageType(effect.contentType) &&
        imageCollectionUri != null

    if (!isPhotoViewerAttachment) {
        openAttachmentPreview(
            context = context,
            contentUri = effect.contentUri,
            contentType = effect.contentType,
        )

        return
    }

    val hostBounds = hostBoundsState.value ?: snapshotFlow { hostBoundsState.value }
        .filterNotNull()
        .first()

    onNavigateToPhotoViewer(
        PhotoViewerLaunchRequest(
            initialPhotoUri = effect.contentUri,
            photosUri = imageCollectionUri,
            sourceBounds = hostBounds.toPhotoViewerSourceBounds(),
            initialPhotoOccurrenceIndex = effect.initialPhotoOccurrenceIndex,
        ),
    )
}

private fun ComposeRect.toPhotoViewerSourceBounds(): PhotoViewerSourceBounds {
    return PhotoViewerSourceBounds(
        left = left.roundToInt(),
        top = top.roundToInt(),
        right = right.roundToInt(),
        bottom = bottom.roundToInt(),
    )
}

internal suspend fun openShareSheet(
    context: Context,
    attachmentContentType: String?,
    attachmentContentUri: String?,
    text: String?,
) {
    val shareIntent = Intent(Intent.ACTION_SEND)

    if (
        !attachmentContentType.isNullOrBlank() &&
        !attachmentContentUri.isNullOrBlank()
    ) {
        val normalizedAttachmentUri = normalizeAttachmentUriForIntent(
            attachmentUri = attachmentContentUri.toUri(),
        )

        shareIntent.putExtra(
            Intent.EXTRA_STREAM,
            normalizedAttachmentUri,
        )
        shareIntent.setType(attachmentContentType)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        shareIntent.putExtra(
            Intent.EXTRA_TEXT,
            text.orEmpty(),
        )
        shareIntent.setType("text/plain")
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getText(R.string.action_share),
        ),
    )
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
