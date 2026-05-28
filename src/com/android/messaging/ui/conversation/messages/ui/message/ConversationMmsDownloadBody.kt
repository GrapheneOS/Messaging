package com.android.messaging.ui.conversation.messages.ui.message

import android.content.Context
import android.content.res.Resources
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import com.android.messaging.ui.conversation.preview.previewMmsDownloadUiModel
import com.android.messaging.ui.core.MessagingPreviewColumn

private const val MMS_DOWNLOAD_STATUS_SEPARATOR = " • "

@Composable
internal fun ConversationMmsDownloadBody(
    download: MmsDownloadUiModel,
    canDownloadMessage: Boolean,
    isSelected: Boolean,
    contentColor: Color,
    simDisplayName: String?,
) {
    val supportingColor = mmsDownloadSupportingColor(
        isSelected = isSelected,
        contentColor = contentColor,
    )

    ConversationMmsDownloadBodyContent(
        titleText = stringResource(id = mmsDownloadTitleResId(state = download.state)),
        infoText = rememberMmsDownloadInfoText(download = download),
        statusLineText = rememberMmsDownloadStatusLineText(
            statusText = stringResource(id = mmsDownloadStatusResId(state = download.state)),
            simDisplayName = simDisplayName,
        ),
        contentColor = contentColor,
        supportingColor = supportingColor,
        statusColor = mmsDownloadStatusColor(
            state = download.state,
            canDownloadMessage = canDownloadMessage,
            isSelected = isSelected,
            contentColor = contentColor,
            supportingColor = supportingColor,
        ),
    )
}

@Composable
private fun mmsDownloadSupportingColor(
    isSelected: Boolean,
    contentColor: Color,
): Color {
    return when {
        isSelected -> contentColor.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun ConversationMmsDownloadBodyContent(
    titleText: String,
    infoText: String,
    statusLineText: String,
    contentColor: Color,
    supportingColor: Color,
    statusColor: Color,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
        )

        Text(
            text = infoText,
            style = MaterialTheme.typography.bodyMedium,
            color = supportingColor,
        )

        Text(
            text = statusLineText,
            style = MaterialTheme.typography.bodyLarge,
            color = statusColor,
        )
    }
}

@Composable
private fun rememberMmsDownloadInfoText(download: MmsDownloadUiModel): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    return remember(
        context,
        configuration,
        download.sizeBytes,
        download.expiryTimestamp,
    ) {
        buildMmsDownloadInfoText(
            context = context,
            download = download,
        )
    }
}

@Composable
private fun rememberMmsDownloadStatusLineText(
    statusText: String,
    simDisplayName: String?,
): String {
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current

    return remember(
        resources,
        configuration,
        statusText,
        simDisplayName,
    ) {
        buildMmsDownloadStatusLineText(
            resources = resources,
            statusText = statusText,
            simDisplayName = simDisplayName,
        )
    }
}

@Composable
private fun mmsDownloadStatusColor(
    state: MmsDownloadUiModel.State,
    canDownloadMessage: Boolean,
    isSelected: Boolean,
    contentColor: Color,
    supportingColor: Color,
): Color {
    return when {
        isSelected -> contentColor
        state == MmsDownloadUiModel.State.AwaitingManualDownload && canDownloadMessage -> {
            MaterialTheme.colorScheme.primary
        }
        state == MmsDownloadUiModel.State.DownloadFailed && canDownloadMessage -> {
            MaterialTheme.colorScheme.primary
        }
        state == MmsDownloadUiModel.State.DownloadFailed -> MaterialTheme.colorScheme.error
        state == MmsDownloadUiModel.State.ExpiredOrUnavailable -> {
            MaterialTheme.colorScheme.error
        }
        else -> supportingColor
    }
}

@StringRes
private fun mmsDownloadTitleResId(state: MmsDownloadUiModel.State): Int {
    return when (state) {
        MmsDownloadUiModel.State.AwaitingManualDownload -> {
            R.string.message_title_manual_download
        }
        MmsDownloadUiModel.State.Downloading -> R.string.message_title_downloading
        MmsDownloadUiModel.State.DownloadFailed -> R.string.message_title_download_failed
        MmsDownloadUiModel.State.ExpiredOrUnavailable -> {
            R.string.message_title_download_failed
        }
    }
}

@StringRes
private fun mmsDownloadStatusResId(state: MmsDownloadUiModel.State): Int {
    return when (state) {
        MmsDownloadUiModel.State.AwaitingManualDownload -> {
            R.string.message_status_download
        }
        MmsDownloadUiModel.State.Downloading -> R.string.message_status_downloading
        MmsDownloadUiModel.State.DownloadFailed -> R.string.message_status_download
        MmsDownloadUiModel.State.ExpiredOrUnavailable -> {
            R.string.message_status_download_error
        }
    }
}

private fun buildMmsDownloadInfoText(
    context: Context,
    download: MmsDownloadUiModel,
): String {
    val formattedSize = Formatter.formatFileSize(context, download.sizeBytes)
    val formattedExpiry = DateUtils.formatDateTime(
        context,
        download.expiryTimestamp,
        DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_SHOW_TIME or
            DateUtils.FORMAT_NUMERIC_DATE or
            DateUtils.FORMAT_NO_YEAR,
    )

    return context.getString(R.string.mms_info, formattedSize, formattedExpiry)
}

private fun buildMmsDownloadStatusLineText(
    resources: Resources,
    statusText: String,
    simDisplayName: String?,
): String {
    val simAnnotation = simDisplayName
        ?.takeIf { displayName -> displayName.isNotBlank() }
        ?.let { displayName ->
            resources.getString(R.string.conversation_message_sim_annotation, displayName)
        }

    return when {
        simAnnotation == null -> statusText
        else -> "$statusText$MMS_DOWNLOAD_STATUS_SEPARATOR$simAnnotation"
    }
}

@PreviewLightDark
@Composable
private fun ConversationMmsDownloadBodyPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            MmsDownloadUiModel.State.entries.forEach { state ->
                ConversationMmsDownloadBody(
                    download = previewMmsDownloadUiModel(state = state),
                    canDownloadMessage = true,
                    isSelected = state == MmsDownloadUiModel.State.Downloading,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    simDisplayName = "Personal",
                )
            }
        }
    }
}
