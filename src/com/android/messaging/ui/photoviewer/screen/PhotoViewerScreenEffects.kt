package com.android.messaging.ui.photoviewer.screen

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.android.messaging.R
import com.android.messaging.ui.AttachmentSaveTask
import com.android.messaging.ui.conversationpicker.host.share.ShareIntentActivity
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerEffect
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UiUtils
import com.android.messaging.util.UriUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOG_TAG = "PhotoViewerScreenEffects"

@Composable
internal fun PhotoViewerScreenEffects(
    screenModel: PhotoViewerScreenModel,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val currentContext = rememberUpdatedState(context)
    val currentOnFinish = rememberUpdatedState(onFinish)

    LaunchedEffect(screenModel) {
        screenModel.effects.collect { effect ->
            handlePhotoViewerEffect(
                context = currentContext.value,
                effect = effect,
                onFinish = currentOnFinish.value,
            )
        }
    }
}

private suspend fun handlePhotoViewerEffect(
    context: Context,
    effect: PhotoViewerEffect,
    onFinish: () -> Unit,
) {
    when (effect) {
        PhotoViewerEffect.Finish -> onFinish()

        is PhotoViewerEffect.Save -> {
            AttachmentSaveTask(
                context,
                effect.uri,
                effect.contentType,
            ).executeOnThreadPool()
        }

        is PhotoViewerEffect.Share -> {
            sharePhoto(
                context = context,
                uri = effect.uri,
                contentType = effect.contentType,
            )
        }

        is PhotoViewerEffect.Forward -> {
            forwardPhoto(
                context = context,
                uri = effect.uri,
                contentType = effect.contentType,
            )
        }
    }
}

private suspend fun sharePhoto(
    context: Context,
    uri: Uri,
    contentType: String,
) {
    val shareUri = normalizeUriForSendIntent(uri = uri)

    try {
        Intent(Intent.ACTION_SEND)
            .apply {
                type = contentType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            .let { intent ->
                Intent.createChooser(
                    intent,
                    context.getText(R.string.action_share),
                )
            }
            .let(context::startActivity)
    } catch (e: ActivityNotFoundException) {
        LogUtil.w(LOG_TAG, "No activity found for photo share intent", e)
        UiUtils.showToastAtBottom(R.string.activity_not_found_message)
    }
}

private suspend fun forwardPhoto(
    context: Context,
    uri: Uri,
    contentType: String,
) {
    val forwardUri = normalizeUriForSendIntent(uri = uri)

    try {
        Intent(context, ShareIntentActivity::class.java)
            .apply {
                action = Intent.ACTION_SEND
                type = contentType
                putExtra(Intent.EXTRA_STREAM, forwardUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            .let(context::startActivity)
    } catch (e: ActivityNotFoundException) {
        LogUtil.w(LOG_TAG, "No activity found for photo forward intent", e)
        UiUtils.showToastAtBottom(R.string.activity_not_found_message)
    }
}

@Suppress("InjectDispatcher")
private suspend fun normalizeUriForSendIntent(uri: Uri): Uri {
    return when {
        uri.scheme != ContentResolver.SCHEME_FILE -> uri

        else -> {
            withContext(context = Dispatchers.IO) {
                UriUtil.persistContentToScratchSpace(uri) ?: uri
            }
        }
    }
}
