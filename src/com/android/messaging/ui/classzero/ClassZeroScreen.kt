package com.android.messaging.ui.classzero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.classzero.model.ClassZeroUiState
import com.android.messaging.ui.core.MessagingPreviewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClassZeroScreen(
    uiState: ClassZeroUiState,
    onSaveClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = {},
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(state = rememberScrollState())
                    .padding(all = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ClassZeroHeader()

                Spacer(modifier = Modifier.height(20.dp))

                ClassZeroMessage(text = uiState.messageText)

                Spacer(modifier = Modifier.height(24.dp))

                ClassZeroActions(
                    onSaveClicked = onSaveClicked,
                    onCancelClicked = onCancelClicked,
                )
            }
        }
    }
}

@Composable
private fun ClassZeroHeader() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Icon(
            imageVector = Icons.Rounded.Sms,
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp)
                .size(32.dp),
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.class_0_message_activity),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CLASS_ZERO_TITLE_TEST_TAG),
    )
}

@Composable
private fun ClassZeroMessage(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(
                min = 180.dp,
                max = 300.dp,
            ),
            shape = RoundedCornerShape(
                topStart = 6.dp,
                topEnd = 24.dp,
                bottomStart = 24.dp,
                bottomEnd = 24.dp,
            ),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(
                        horizontal = 20.dp,
                        vertical = 20.dp,
                    ),
            )
        }
    }
}

@Composable
private fun ClassZeroActions(
    onSaveClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CLASS_ZERO_SAVE_BUTTON_TEST_TAG),
            onClick = onSaveClicked,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text = stringResource(R.string.save))
        }

        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CLASS_ZERO_CANCEL_BUTTON_TEST_TAG),
            onClick = onCancelClicked,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text = stringResource(android.R.string.cancel))
        }
    }
}

@PreviewLightDark
@Composable
private fun ClassZeroScreenShortMessagePreview() {
    MessagingPreviewTheme {
        PreviewClassZeroScreen(messageText = "Flash SMS message")
    }
}

@PreviewLightDark
@Composable
private fun ClassZeroScreenMultilineMessagePreview() {
    MessagingPreviewTheme {
        PreviewClassZeroScreen(
            messageText = "Bank alert\nPayment approved\nCard ending 1234",
        )
    }
}

@PreviewLightDark
@Composable
private fun ClassZeroScreenLongMessagePreview() {
    MessagingPreviewTheme {
        PreviewClassZeroScreen(
            messageText = "This is a longer class 0 SMS message intended to verify wrapping, " +
                "bubble sizing, vertical spacing, and the scroll cap for urgent flash messages. " +
                "It should remain readable without making the action buttons difficult to reach. " +
                "Additional text keeps the preview tall enough to exercise the maximum bubble " +
                "height and scrolling behavior.",
        )
    }
}

@PreviewLightDark
@Composable
private fun ClassZeroScreenNarrowTextPreview() {
    MessagingPreviewTheme {
        PreviewClassZeroScreen(
            messageText = "Security code: 492-183. Use it only in Messages. " +
                "Do not share this code with anyone.",
        )
    }
}

@Composable
private fun PreviewClassZeroScreen(messageText: String) {
    ClassZeroScreen(
        uiState = ClassZeroUiState(
            messageText = messageText,
        ),
        onSaveClicked = {},
        onCancelClicked = {},
    )
}

@Preview(widthDp = 320)
@Composable
private fun ClassZeroScreenCompactWidthPreview() {
    MessagingPreviewTheme {
        PreviewClassZeroScreen(
            messageText = "Compact width preview with enough text to wrap across several lines.",
        )
    }
}
