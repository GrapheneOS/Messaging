package com.android.messaging.ui.smsstoragelow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import com.android.messaging.domain.smsstoragelow.model.SmsStorageRetentionDuration
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.smsstoragelow.model.SmsStorageLowWarningScreenEffect as Effect
import com.android.messaging.ui.smsstoragelow.model.SmsStorageLowWarningUiState as State
import kotlinx.collections.immutable.persistentListOf

private val DialogMaxHeight = 560.dp
private val DialogPadding = 24.dp

@Composable
internal fun SmsStorageLowWarningScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: SmsStorageLowWarningScreenModel = viewModel<SmsStorageLowWarningViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(screenModel, onFinish) {
        screenModel.effects.collect { effect ->
            when (effect) {
                Effect.Finish -> onFinish()
            }
        }
    }

    SmsStorageLowWarningDialog(
        uiState = uiState,
        onActionClicked = screenModel::onActionClicked,
        onCleanupConfirmed = screenModel::onCleanupConfirmed,
        onConfirmationDismissed = screenModel::onConfirmationDismissed,
        onIgnoreClicked = screenModel::onIgnoreClicked,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsStorageLowWarningDialog(
    uiState: State,
    onActionClicked: (SmsStorageLowWarningAction) -> Unit,
    onCleanupConfirmed: () -> Unit,
    onConfirmationDismissed: () -> Unit,
    onIgnoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val selectedAction = uiState.selectedAction) {
        null -> {
            SmsStorageLowWarningChoiceDialog(
                uiState = uiState,
                onActionClicked = onActionClicked,
                onIgnoreClicked = onIgnoreClicked,
                modifier = modifier,
            )
        }

        else -> {
            SmsStorageLowWarningConfirmationDialog(
                action = selectedAction,
                isProcessing = uiState.isProcessing,
                onCleanupConfirmed = onCleanupConfirmed,
                onConfirmationDismissed = onConfirmationDismissed,
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsStorageLowWarningChoiceDialog(
    uiState: State,
    onActionClicked: (SmsStorageLowWarningAction) -> Unit,
    onIgnoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(
        onDismissRequest = {
            if (!uiState.isProcessing) {
                onIgnoreClicked()
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        SmsStorageLowWarningDialogSurface(
            testTag = SMS_STORAGE_LOW_WARNING_DIALOG_TEST_TAG,
            modifier = modifier,
        ) {
            SmsStorageLowWarningChoiceDialogContent(
                uiState = uiState,
                onActionClicked = onActionClicked,
                onIgnoreClicked = onIgnoreClicked,
            )
        }
    }
}

@Composable
private fun SmsStorageLowWarningDialogSurface(
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .heightIn(max = DialogMaxHeight)
            .testTag(tag = testTag),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
    ) {
        content()
    }
}

@Composable
private fun SmsStorageLowWarningChoiceDialogContent(
    uiState: State,
    onActionClicked: (SmsStorageLowWarningAction) -> Unit,
    onIgnoreClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .padding(all = DialogPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WarningHeader()

        Spacer(modifier = Modifier.height(height = 16.dp))

        SmsStorageLowWarningChoiceDialogMessage()

        Spacer(modifier = Modifier.height(height = 24.dp))

        SmsStorageLowWarningActions(
            uiState = uiState,
            onActionClicked = onActionClicked,
        )

        Spacer(modifier = Modifier.height(height = 16.dp))

        SmsStorageLowWarningIgnoreButton(
            enabled = !uiState.isProcessing,
            onClick = onIgnoreClicked,
        )
    }
}

@Composable
private fun SmsStorageLowWarningChoiceDialogMessage() {
    Text(
        text = stringResource(id = R.string.sms_storage_low_title),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag = SMS_STORAGE_LOW_WARNING_TITLE_TEST_TAG),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(height = 12.dp))

    Text(
        text = stringResource(id = R.string.sms_storage_low_text),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag = SMS_STORAGE_LOW_WARNING_MESSAGE_TEST_TAG),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SmsStorageLowWarningIgnoreButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag = SMS_STORAGE_LOW_WARNING_IGNORE_BUTTON_TEST_TAG),
    ) {
        Text(text = stringResource(id = R.string.ignore))
    }
}

@Composable
private fun WarningHeader() {
    Box(
        modifier = Modifier
            .size(size = 56.dp)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun SmsStorageLowWarningActions(
    uiState: State,
    onActionClicked: (SmsStorageLowWarningAction) -> Unit,
) {
    when {
        uiState.isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .testTag(tag = SMS_STORAGE_LOW_WARNING_LOADING_TEST_TAG),
            )
        }

        else -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            ) {
                uiState.actions.forEach { action ->
                    SmsStorageLowWarningActionRow(
                        action = action,
                        enabled = !uiState.isProcessing,
                        onClick = { onActionClicked(action) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SmsStorageLowWarningActionRow(
    action: SmsStorageLowWarningAction,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag = smsStorageLowWarningActionTestTag(action = action))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = actionIcon(action = action),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.width(width = 16.dp))

            Text(
                text = actionTitle(action = action),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsStorageLowWarningConfirmationDialog(
    action: SmsStorageLowWarningAction,
    isProcessing: Boolean,
    onCleanupConfirmed: () -> Unit,
    onConfirmationDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(
        onDismissRequest = {
            if (!isProcessing) {
                onConfirmationDismissed()
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        SmsStorageLowWarningDialogSurface(
            testTag = SMS_STORAGE_LOW_WARNING_CONFIRMATION_DIALOG_TEST_TAG,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(state = rememberScrollState())
                    .padding(all = DialogPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = actionIcon(action = action),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )

                Spacer(modifier = Modifier.height(height = 16.dp))

                Text(
                    text = stringResource(id = R.string.sms_storage_low_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(height = 12.dp))

                Text(
                    text = confirmationMessage(action = action),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(
                            tag = SMS_STORAGE_LOW_WARNING_CONFIRMATION_MESSAGE_TEST_TAG,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(height = 24.dp))

                SmsStorageLowWarningConfirmationButtons(
                    isProcessing = isProcessing,
                    onCleanupConfirmed = onCleanupConfirmed,
                    onConfirmationDismissed = onConfirmationDismissed,
                )
            }
        }
    }
}

@Composable
private fun SmsStorageLowWarningConfirmationButtons(
    isProcessing: Boolean,
    onCleanupConfirmed: () -> Unit,
    onConfirmationDismissed: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Button(
            onClick = onCleanupConfirmed,
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag = SMS_STORAGE_LOW_WARNING_CONFIRM_BUTTON_TEST_TAG),
        ) {
            Text(text = stringResource(id = android.R.string.ok))
        }

        FilledTonalButton(
            onClick = onConfirmationDismissed,
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag = SMS_STORAGE_LOW_WARNING_CANCEL_BUTTON_TEST_TAG),
        ) {
            Text(text = stringResource(id = android.R.string.cancel))
        }
    }
}

@PreviewLightDark
@Composable
private fun SmsStorageLowWarningDialogPreview() {
    MessagingPreviewTheme {
        SmsStorageLowWarningDialog(
            uiState = previewState(),
            onActionClicked = {},
            onCleanupConfirmed = {},
            onConfirmationDismissed = {},
            onIgnoreClicked = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SmsStorageLowWarningDialogLoadingPreview() {
    MessagingPreviewTheme {
        SmsStorageLowWarningDialog(
            uiState = State(isLoading = true),
            onActionClicked = {},
            onCleanupConfirmed = {},
            onConfirmationDismissed = {},
            onIgnoreClicked = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SmsStorageLowWarningConfirmationDialogPreview() {
    MessagingPreviewTheme {
        SmsStorageLowWarningDialog(
            uiState = previewState(
                selectedAction = SmsStorageLowWarningAction.DeleteOldMessages(
                    retentionDuration = previewRetentionDuration(),
                ),
            ),
            onActionClicked = {},
            onCleanupConfirmed = {},
            onConfirmationDismissed = {},
            onIgnoreClicked = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SmsStorageLowWarningProcessingPreview() {
    MessagingPreviewTheme {
        SmsStorageLowWarningDialog(
            uiState = previewState(
                selectedAction = SmsStorageLowWarningAction.DeleteMediaMessages(
                    retentionDuration = previewRetentionDuration(),
                ),
                isProcessing = true,
            ),
            onActionClicked = {},
            onCleanupConfirmed = {},
            onConfirmationDismissed = {},
            onIgnoreClicked = {},
        )
    }
}

private fun previewState(
    selectedAction: SmsStorageLowWarningAction? = null,
    isProcessing: Boolean = false,
): State {
    val retentionDuration = previewRetentionDuration()
    val actions = persistentListOf(
        SmsStorageLowWarningAction.DeleteMediaMessages(
            retentionDuration = retentionDuration,
        ),
        SmsStorageLowWarningAction.DeleteOldMessages(
            retentionDuration = retentionDuration,
        ),
    )

    return State(
        actions = actions,
        selectedAction = selectedAction,
        isLoading = false,
        isProcessing = isProcessing,
    )
}

private fun previewRetentionDuration(): SmsStorageRetentionDuration {
    return SmsStorageRetentionDuration(
        count = 1,
        unit = SmsStorageRetentionDuration.DurationUnit.MONTH,
        millis = 30L * 24L * 60L * 60L * 1000L,
    )
}
