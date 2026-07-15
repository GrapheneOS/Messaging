package com.android.messaging.ui.vcarddetail.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.data.vcarddetail.model.VCardFieldAction
import com.android.messaging.ui.common.components.contentSurfaceShape
import com.android.messaging.ui.common.components.safeDrawingContentPadding
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.vcarddetail.common.VCardContactCard
import com.android.messaging.ui.vcarddetail.common.VCardDetailTopAppBar
import com.android.messaging.ui.vcarddetail.screen.model.VCardContactUiModel
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailAction as Action
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailUiState as State
import com.android.messaging.ui.vcarddetail.screen.model.VCardFieldUiModel
import kotlinx.collections.immutable.persistentListOf

private val ScreenContentPadding = 8.dp
private val CardSpacing = 8.dp

@Composable
internal fun VCardDetailScreen(
    effectHandler: VCardDetailEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: VCardDetailScreenModel = viewModel<VCardDetailViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    val currentEffectHandler by rememberUpdatedState(effectHandler)
    LaunchedEffect(screenModel) {
        screenModel.effects.collect { effect ->
            currentEffectHandler.handle(effect)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        screenModel.refresh()
    }

    VCardDetailContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VCardDetailContent(
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            VCardDetailTopAppBar(
                onNavigateBack = onNavigateBack,
                canAddToContacts = uiState.canAddToContacts,
                onAddToContactsClick = { onAction(Action.AddToContactsClicked) },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .clip(MaterialTheme.contentSurfaceShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    VCardContactList(
                        contacts = uiState.contacts,
                        onAction = onAction,
                        scaffoldContentPadding = contentPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun VCardContactList(
    contacts: List<VCardContactUiModel>,
    onAction: (Action) -> Unit,
    scaffoldContentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(CardSpacing),
        contentPadding = safeDrawingContentPadding(
            top = ScreenContentPadding,
            bottom = ScreenContentPadding + scaffoldContentPadding.calculateBottomPadding(),
            horizontal = ScreenContentPadding,
        ),
    ) {
        items(
            items = contacts,
        ) { contact ->
            VCardContactCard(
                contact = contact,
                onFieldClick = { action -> onAction(Action.FieldClicked(action)) },
                onFieldLongClick = { value -> onAction(Action.FieldLongClicked(value)) },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun VCardDetailContentPreview() {
    MessagingPreviewTheme {
        VCardDetailContent(
            uiState = State(
                isLoading = false,
                contacts = persistentListOf(
                    VCardContactUiModel(
                        displayName = "Ada Lovelace",
                        normalizedDestination = "+15550001",
                        avatarPhoto = null,
                        fields = persistentListOf(
                            VCardFieldUiModel(
                                value = "+1 555 0001",
                                label = "Mobile",
                                action = VCardFieldAction.Dial("+15550001"),
                            ),
                            VCardFieldUiModel(
                                value = "ada@example.com",
                                label = "Home",
                                action = VCardFieldAction.Email("ada@example.com"),
                            ),
                        ),
                    ),
                ),
                canAddToContacts = true,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun VCardDetailLoadingPreview() {
    MessagingPreviewTheme {
        VCardDetailContent(
            uiState = State(isLoading = true),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
