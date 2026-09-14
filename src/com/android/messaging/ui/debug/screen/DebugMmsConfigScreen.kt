package com.android.messaging.ui.debug.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.ui.common.components.horizontalSafeDrawingInsets
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.debug.common.DebugMmsConfigEditDialog
import com.android.messaging.ui.debug.common.DebugMmsConfigItem
import com.android.messaging.ui.debug.common.DebugMmsConfigTopAppBar
import com.android.messaging.ui.debug.screen.model.DebugMmsConfigAction as Action
import com.android.messaging.ui.debug.screen.model.DebugMmsConfigUiState as State
import com.android.messaging.ui.debug.screen.model.DebugSimUiState
import com.android.messaging.ui.debug.screen.model.MmsConfigItemUiState
import kotlinx.collections.immutable.persistentListOf

private val ScreenHorizontalPadding = 16.dp
private val ContentVerticalPadding = 8.dp
private val SelectorTopPadding = 12.dp
private val SelectorSpacing = 8.dp

@Composable
internal fun DebugMmsConfigScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: DebugMmsConfigScreenModel = viewModel<DebugMmsConfigViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    DebugMmsConfigContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
private fun DebugMmsConfigContent(
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingItem by remember { mutableStateOf<MmsConfigItemUiState.Editable?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            DebugMmsConfigTopAppBar(onNavigateBack = onNavigateBack)
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .padding(horizontalSafeDrawingInsets()),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Butt,
                    gapSize = 0.dp,
                )
            }

            SimSelector(
                sims = uiState.sims,
                selectedSubId = uiState.selectedSubId,
                onSimSelected = { subId -> onAction(Action.SimSelected(subId)) },
            )

            DebugMmsConfigList(
                uiState = uiState,
                scaffoldContentPadding = contentPadding,
                onToggle = { key, checked ->
                    onAction(Action.EntryToggled(key, checked))
                },
                onEditClick = { item -> editingItem = item },
            )
        }
    }

    editingItem?.let { item ->
        DebugMmsConfigEditDialog(
            item = item,
            onDismiss = { editingItem = null },
            onConfirm = { value ->
                onAction(
                    Action.EntryValueSubmitted(
                        key = item.key,
                        value = value,
                        isNumeric = item.isNumeric,
                    ),
                )
                editingItem = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimSelector(
    sims: List<DebugSimUiState>,
    selectedSubId: SubId?,
    onSimSelected: (SubId) -> Unit,
) {
    if (sims.isEmpty()) {
        return
    }

    val selectedSim = sims.firstOrNull { sim -> sim.subId == selectedSubId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = ScreenHorizontalPadding,
                end = ScreenHorizontalPadding,
                top = SelectorTopPadding,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SelectorSpacing),
        ) {
            sims.forEach { sim ->
                FilterChip(
                    selected = sim.subId == selectedSubId,
                    onClick = { onSimSelected(sim.subId) },
                    label = { Text(text = sim.subId.value.toString()) },
                )
            }
        }

        if (selectedSim != null) {
            val subIdLabel = stringResource(R.string.debug_sub_id_spinner_text)

            Text(
                text = "${selectedSim.mccMnc} $subIdLabel",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    top = SelectorSpacing,
                    bottom = SelectorSpacing,
                ),
            )
        }
    }
}

@Composable
private fun DebugMmsConfigList(
    uiState: State,
    scaffoldContentPadding: PaddingValues,
    onToggle: (String, Boolean) -> Unit,
    onEditClick: (MmsConfigItemUiState.Editable) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = ContentVerticalPadding,
            bottom = ContentVerticalPadding + scaffoldContentPadding.calculateBottomPadding(),
        ),
    ) {
        items(
            items = uiState.items,
            key = { item -> item.key },
            contentType = { item -> item::class },
        ) { item ->
            DebugMmsConfigItem(
                item = item,
                onToggle = { checked -> onToggle(item.key, checked) },
                onEditClick = {
                    if (item is MmsConfigItemUiState.Editable) {
                        onEditClick(item)
                    }
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DebugMmsConfigContentPreview() {
    MessagingPreviewTheme {
        DebugMmsConfigContent(
            uiState = State(
                items = persistentListOf(
                    MmsConfigItemUiState.Toggle(key = "enabledMMS", checked = true),
                    MmsConfigItemUiState.Editable(
                        key = "maxMessageSize",
                        value = "1048576",
                        isNumeric = true,
                    ),
                    MmsConfigItemUiState.Editable(
                        key = "userAgent",
                        value = "Messaging",
                        isNumeric = false,
                    ),
                ),
                sims = persistentListOf(
                    DebugSimUiState(
                        subId = SubId(1),
                        mccMnc = "(310/260)",
                    ),
                    DebugSimUiState(
                        subId = SubId(2),
                        mccMnc = "(208/01)",
                    ),
                ),
                selectedSubId = SubId(1),
                isLoading = false,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
