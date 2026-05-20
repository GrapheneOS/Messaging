package com.android.messaging.ui.blockedparticipants.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsAction as Action
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState as State

@Composable
internal fun BlockedParticipantsScreen(
    effectHandler: BlockedParticipantsEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: BlockedParticipantsScreenModel = viewModel<BlockedParticipantsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(screenModel, effectHandler) {
        screenModel.effects.collect(effectHandler::handle)
    }

    LaunchedEffect(screenModel, onNavigateBack) {
        screenModel.navigationEvents.collect { event ->
        }
    }

    BlockedParticipantsContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
private fun BlockedParticipantsContent(
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
}
