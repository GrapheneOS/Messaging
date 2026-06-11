@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.messaging.ui.conversation.messagedetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.ui.conversation.messagedetails.model.MessageDetailsUiState
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun MessageDetailsScreen(
    conversationId: String,
    messageId: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    screenModel: MessageDetailsScreenModel = hiltViewModel<MessageDetailsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(conversationId, messageId, screenModel) {
        screenModel.onArguments(
            conversationId = conversationId,
            messageId = messageId,
        )
    }

    MessageDetailsScaffold(
        uiState = uiState,
        modifier = modifier,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun MessageDetailsScaffold(
    uiState: MessageDetailsUiState,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                        )
                    }
                },
                title = {
                    Text(text = stringResource(id = R.string.message_details_title))
                },
            )
        },
    ) { contentPadding ->
        when (uiState) {
            MessageDetailsUiState.Loading -> {
                MessageDetailsPlaceholder(modifier = Modifier.padding(contentPadding)) {
                    CircularProgressIndicator()
                }
            }

            MessageDetailsUiState.Unavailable -> {
                MessageDetailsPlaceholder(modifier = Modifier.padding(contentPadding)) {
                    Text(
                        text = stringResource(id = R.string.message_details_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is MessageDetailsUiState.Content -> {
                MessageDetailsContent(
                    content = uiState,
                    modifier = Modifier.padding(contentPadding),
                )
            }
        }
    }
}

@Composable
private fun MessageDetailsContent(
    content: MessageDetailsUiState.Content,
    modifier: Modifier = Modifier,
) {
    val details = content.details

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        MessageDetailsPreviewCard(
            preview = content.preview,
            details = details,
        )

        MessageDetailsMessageFields(details = details)

        if (details.sentTimestamp != null || details.receivedTimestamp != null) {
            MessageDetailsStatusSection(
                sentTimestamp = details.sentTimestamp,
                receivedTimestamp = details.receivedTimestamp,
            )
        }

        MessageDetailsDeliveryFields(details = details)

        details.debug?.let { debug ->
            MessageDetailsDebugSection(debug = debug)
        }
    }
}

@Composable
private fun MessageDetailsPlaceholder(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@PreviewLightDark
@Composable
private fun MessageDetailsContentPreview() {
    MessagingPreviewTheme {
        MessageDetailsScaffold(
            uiState = MessageDetailsUiState.Content(
                preview = previewIncomingMessage(),
                details = ConversationMessageDetails(
                    type = ConversationMessageDetails.Type.SMS,
                    sender = "+1 555-0100",
                    recipients = persistentListOf("+1 555-0199"),
                    sentTimestamp = PREVIEW_SENT_TIMESTAMP,
                    receivedTimestamp = PREVIEW_RECEIVED_TIMESTAMP,
                    priority = null,
                    sizeBytes = null,
                    subscriptionLabel = null,
                    debug = null,
                ),
            ),
        )
    }
}

private const val PREVIEW_SENT_TIMESTAMP = 1_700_000_000_000L
private const val PREVIEW_RECEIVED_TIMESTAMP = 1_700_000_060_000L
