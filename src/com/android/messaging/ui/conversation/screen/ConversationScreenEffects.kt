package com.android.messaging.ui.conversation.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Point
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.MessageDetailsDialog
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.LogUtil
import com.android.messaging.util.MediaUtil
import com.android.messaging.util.UiUtils

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
    val draftSentTick = remember { mutableIntStateOf(0) }

    LaunchedEffect(screenModel, context, snackbarHostState, hostBoundsState, onNavigateBack) {
        screenModel.effects.collect { effect ->
            screenModel.handleConversationScreenEffect(
                context = context,
                snackbarHostState = snackbarHostState,
                hostBoundsState = hostBoundsState,
                effect = effect,
                launchRoleRequest = defaultSmsRoleLauncher::launch,
                onNavigateBack = onNavigateBack,
                onDraftSent = { draftSentTick.intValue++ },
            )
        }
    }

    SendingMessageAnnouncement(triggerKey = draftSentTick.intValue)
}

private suspend fun ConversationScreenModel.handleConversationScreenEffect(
    context: Context,
    snackbarHostState: SnackbarHostState,
    hostBoundsState: State<ComposeRect?>,
    effect: ConversationScreenEffect,
    launchRoleRequest: (Intent) -> Unit,
    onNavigateBack: () -> Unit,
    onDraftSent: () -> Unit,
) {
    when (effect) {
        ConversationScreenEffect.CloseConversation -> onNavigateBack()
        is ConversationScreenEffect.RequestDefaultSmsRole -> {
            requestDefaultSmsRole(
                context = context,
                snackbarHostState = snackbarHostState,
                effect = effect,
                onActionClick = ::onDefaultSmsRolePromptActionClick,
            )
        }

        is ConversationScreenEffect.LaunchDefaultSmsRoleRequest -> {
            launchDefaultSmsRoleRequest(
                effect = effect,
                launchRoleRequest = launchRoleRequest,
                onLaunchFailed = ::onDefaultSmsRoleRequestLaunchFailed,
            )
        }

        is ConversationScreenEffect.OpenAttachmentPreview -> {
            openAttachmentPreviewEffect(
                context = context,
                hostBoundsState = hostBoundsState,
                effect = effect,
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

        is ConversationScreenEffect.LaunchAddContactFlow,
        is ConversationScreenEffect.LaunchForwardMessage,
        ConversationScreenEffect.NotifyDraftSent,
        is ConversationScreenEffect.OpenExternalUri,
        is ConversationScreenEffect.PlacePhoneCall,
        is ConversationScreenEffect.ShowMessage,
        is ConversationScreenEffect.ShowMessageDetails,
        is ConversationScreenEffect.ShowSaveAttachmentsResult,
        -> {
            handleImmediateConversationScreenEffect(
                context = context,
                effect = effect,
                onDraftSent = onDraftSent,
            )
        }
    }
}

private fun handleImmediateConversationScreenEffect(
    context: Context,
    effect: ConversationScreenEffect,
    onDraftSent: () -> Unit,
) {
    when (effect) {
        is ConversationScreenEffect.LaunchAddContactFlow -> {
            UIIntents.get().launchAddContactActivity(
                context,
                effect.destination,
            )
        }

        is ConversationScreenEffect.LaunchForwardMessage -> {
            UIIntents.get().launchForwardMessageActivity(
                context,
                effect.message,
            )
        }

        ConversationScreenEffect.NotifyDraftSent -> {
            playDraftSentSound(context = context)
            onDraftSent()
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

        is ConversationScreenEffect.ShowSaveAttachmentsResult -> {
            showSaveAttachmentsResultToast(
                context = context,
                effect = effect,
            )
        }

        else -> {}
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

@Composable
private fun SendingMessageAnnouncement(
    triggerKey: Int,
) {
    if (triggerKey == 0) {
        return
    }

    val text = stringResource(R.string.sending_message)

    key(triggerKey) {
        Box(
            modifier = Modifier
                .size(0.dp)
                .clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = text
                },
        )
    }
}

private fun playDraftSentSound(context: Context) {
    val prefs = BuglePrefs.getApplicationPrefs()
    val prefKey = context.getString(R.string.send_sound_pref_key)
    val default = context.resources.getBoolean(R.bool.send_sound_pref_default)

    if (prefs.getBoolean(prefKey, default)) {
        MediaUtil.get().playSound(context, R.raw.message_sent, null)
    }
}
