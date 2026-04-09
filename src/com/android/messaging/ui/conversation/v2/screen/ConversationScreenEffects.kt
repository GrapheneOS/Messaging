package com.android.messaging.ui.conversation.v2.screen

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.net.toUri
import com.android.messaging.R
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.util.ContentType
import com.android.messaging.util.UiUtils
import com.android.messaging.util.UriUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ConversationScreenEffects(
    screenModel: ConversationScreenModel,
) {
    val context = LocalContext.current
    val hostView = LocalView.current

    LaunchedEffect(screenModel, context, hostView) {
        screenModel.effects.collect { effect ->
            when (effect) {
                is ConversationScreenEffect.OpenAttachmentPreview -> {
                    openAttachmentPreview(
                        context = context,
                        hostView = hostView,
                        contentUri = effect.contentUri,
                        contentType = effect.contentType,
                        imageCollectionUri = effect.imageCollectionUri,
                    )
                }

                is ConversationScreenEffect.OpenExternalUri -> {
                    openExternalUri(
                        context = context,
                        uri = effect.uri,
                    )
                }

                is ConversationScreenEffect.ShowMessage -> {
                    UiUtils.showToastAtBottom(effect.messageResId)
                }
            }
        }
    }
}

private fun openExternalUri(
    context: Context,
    uri: String,
) {
    UIIntents.get().launchBrowserForUrl(context, uri)
}

private suspend fun openAttachmentPreview(
    context: Context,
    hostView: View,
    contentUri: String,
    contentType: String,
    imageCollectionUri: String?,
) {
    val attachmentUri = contentUri.toUri()

    when {
        ContentType.isImageType(contentType) -> {
            val isOpenedInternally = openImageAttachmentPreview(
                context = context,
                hostView = hostView,
                attachmentUri = attachmentUri,
                imageCollectionUri = imageCollectionUri,
            )
            if (!isOpenedInternally) {
                openGenericAttachmentPreview(
                    context = context,
                    attachmentUri = attachmentUri,
                    contentType = contentType,
                )
            }
        }

        ContentType.isVCardType(contentType) -> {
            UIIntents.get().launchVCardDetailActivity(
                context,
                normalizeAttachmentUriForIntent(attachmentUri = attachmentUri),
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

private fun openImageAttachmentPreview(
    context: Context,
    hostView: View,
    attachmentUri: Uri,
    imageCollectionUri: String?,
): Boolean {
    val activity = UiUtils.getActivity(context) ?: return false
    val imageCollection = imageCollectionUri?.toUri() ?: return false

    UIIntents.get().launchFullScreenPhotoViewer(
        activity,
        attachmentUri,
        UiUtils.getMeasuredBoundsOnScreen(hostView),
        imageCollection,
    )

    return true
}

private fun openGenericAttachmentPreview(
    context: Context,
    attachmentUri: Uri,
    contentType: String,
) {
    runCatching {
        Intent(Intent.ACTION_VIEW)
            .apply {
                setDataAndType(attachmentUri, contentType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            .let(context::startActivity)
    }.onFailure {
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
