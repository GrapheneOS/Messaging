package com.android.messaging.ui.conversation.v2.screen

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.android.messaging.R
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.MessageDetailsDialog
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.util.ContentType
import com.android.messaging.util.UiUtils
import com.android.messaging.util.UriUtil
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Composable
internal fun ConversationScreenEffects(
    screenModel: ConversationScreenModel,
    hostBoundsState: State<ComposeRect?>,
) {
    val context = LocalContext.current

    LaunchedEffect(screenModel, context, hostBoundsState) {
        screenModel.effects.collect { effect ->
            when (effect) {
                is ConversationScreenEffect.OpenAttachmentPreview -> {
                    openAttachmentPreview(
                        context = context,
                        hostBounds = hostBoundsState.value,
                        contentUri = effect.contentUri,
                        contentType = effect.contentType,
                        imageCollectionUri = effect.imageCollectionUri,
                        awaitHostBounds = {
                            snapshotFlow { hostBoundsState.value }
                                .filterNotNull()
                                .first()
                        },
                    )
                }

                is ConversationScreenEffect.OpenExternalUri -> {
                    openExternalUri(
                        context = context,
                        uri = effect.uri,
                    )
                }

                is ConversationScreenEffect.LaunchForwardMessage -> {
                    UIIntents.get().launchForwardMessageActivity(
                        context,
                        effect.message,
                    )
                }

                is ConversationScreenEffect.ShareMessage -> {
                    openShareSheet(
                        context = context,
                        attachmentContentType = effect.attachmentContentType,
                        attachmentContentUri = effect.attachmentContentUri,
                        text = effect.text,
                    )
                }

                is ConversationScreenEffect.ShowMessage -> {
                    UiUtils.showToastAtBottom(effect.messageResId)
                }

                is ConversationScreenEffect.ShowMessageDetails -> {
                    MessageDetailsDialog.show(
                        context,
                        effect.message,
                        effect.participants,
                        effect.selfParticipant,
                    )
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

private suspend fun openShareSheet(
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

private suspend fun openAttachmentPreview(
    context: Context,
    hostBounds: ComposeRect?,
    contentUri: String,
    contentType: String,
    imageCollectionUri: String?,
    awaitHostBounds: suspend () -> ComposeRect,
) {
    val attachmentUri = contentUri.toUri()

    when {
        ContentType.isImageType(contentType) -> {
            val resolvedHostBounds = hostBounds ?: awaitHostBounds()
            val isOpenedInternally = openImageAttachmentPreview(
                context = context,
                hostBounds = resolvedHostBounds,
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
    hostBounds: ComposeRect,
    attachmentUri: Uri,
    imageCollectionUri: String?,
): Boolean {
    val activity = UiUtils.getActivity(context) ?: return false
    val imageCollection = imageCollectionUri?.toUri() ?: return false

    UIIntents.get().launchFullScreenPhotoViewer(
        activity,
        attachmentUri,
        hostBounds.toAndroidRect(),
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

private fun ComposeRect.toAndroidRect(): Rect {
    return Rect(
        left.roundToInt(),
        top.roundToInt(),
        right.roundToInt(),
        bottom.roundToInt(),
    )
}
