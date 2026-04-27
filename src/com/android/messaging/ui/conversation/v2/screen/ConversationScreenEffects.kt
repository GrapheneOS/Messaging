package com.android.messaging.ui.conversation.v2.screen

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UiUtils
import com.android.messaging.util.UriUtil
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val LOG_TAG = "ConversationScreenEffects"

@Composable
internal fun ConversationScreenEffects(
    screenModel: ConversationScreenModel,
    snackbarHostState: SnackbarHostState,
    hostBoundsState: State<ComposeRect?>,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val defaultSmsRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        screenModel.onDefaultSmsRoleRequestResult(resultCode = result.resultCode)
    }

    LaunchedEffect(screenModel, context, snackbarHostState, hostBoundsState, onNavigateBack) {
        screenModel.effects.collect { effect ->
            when (effect) {
                ConversationScreenEffect.CloseConversation -> {
                    onNavigateBack()
                }

                is ConversationScreenEffect.RequestDefaultSmsRole -> {
                    requestDefaultSmsRole(
                        context = context,
                        snackbarHostState = snackbarHostState,
                        effect = effect,
                        onActionClick = screenModel::onDefaultSmsRolePromptActionClick,
                    )
                }

                is ConversationScreenEffect.LaunchAddContactFlow -> {
                    UIIntents.get().launchAddContactActivity(
                        context,
                        effect.destination,
                    )
                }

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

                is ConversationScreenEffect.PlacePhoneCall -> {
                    placePhoneCall(
                        context = context,
                        phoneNumber = effect.phoneNumber,
                    )
                }

                is ConversationScreenEffect.ShowSaveAttachmentsResult -> {
                    showSaveAttachmentsResultToast(
                        context = context,
                        effect = effect,
                    )
                }

                is ConversationScreenEffect.LaunchDefaultSmsRoleRequest -> {
                    launchDefaultSmsRoleRequest(
                        effect = effect,
                        launchRoleRequest = { intent ->
                            defaultSmsRoleLauncher.launch(intent)
                        },
                        onLaunchFailed = screenModel::onDefaultSmsRoleRequestLaunchFailed,
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

private suspend fun requestDefaultSmsRole(
    context: Context,
    snackbarHostState: SnackbarHostState,
    effect: ConversationScreenEffect.RequestDefaultSmsRole,
    onActionClick: () -> Unit,
) {
    snackbarHostState.currentSnackbarData?.dismiss()

    val messageResId = when {
        effect.isSending -> R.string.requires_default_sms_app_to_send
        else -> R.string.requires_default_sms_app
    }

    val snackbarResult = snackbarHostState.showSnackbar(
        message = context.getString(messageResId),
        actionLabel = context.getString(R.string.requires_default_sms_change_button),
        duration = SnackbarDuration.Indefinite,
    )

    if (snackbarResult == SnackbarResult.ActionPerformed) {
        onActionClick()
    }
}

private fun launchDefaultSmsRoleRequest(
    effect: ConversationScreenEffect.LaunchDefaultSmsRoleRequest,
    launchRoleRequest: (Intent) -> Unit,
    onLaunchFailed: () -> Unit,
) {
    try {
        launchRoleRequest(effect.intent)
    } catch (exception: ActivityNotFoundException) {
        LogUtil.w(LOG_TAG, "Couldn't find activity", exception)
        onLaunchFailed()
    }
}

private fun openExternalUri(
    context: Context,
    uri: String,
) {
    UIIntents.get().launchBrowserForUrl(context, uri)
}

private fun placePhoneCall(
    context: Context,
    phoneNumber: String,
) {
    UIIntents.get().launchPhoneCallActivity(
        context,
        phoneNumber,
        Point(0, 0),
    )
}

private fun showSaveAttachmentsResultToast(
    context: Context,
    effect: ConversationScreenEffect.ShowSaveAttachmentsResult,
) {
    if (effect.failCount > 0) {
        UiUtils.showToastAtBottom(
            context.resources.getQuantityString(
                R.plurals.attachment_save_error,
                effect.failCount,
                effect.failCount,
            ),
        )

        return
    }

    val total = effect.imageCount + effect.videoCount + effect.otherCount
    if (total == 0) {
        return
    }

    val pluralResId = when {
        effect.otherCount > 0 && effect.imageCount + effect.videoCount == 0 -> {
            R.plurals.attachments_saved_to_downloads
        }

        effect.otherCount > 0 -> R.plurals.attachments_saved
        effect.videoCount == 0 -> R.plurals.photos_saved
        effect.imageCount == 0 -> R.plurals.videos_saved
        else -> R.plurals.attachments_saved
    }

    UiUtils.showToastAtBottom(
        context.resources.getQuantityString(pluralResId, total, total),
    )
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
