package com.android.messaging.ui.permissioncheck.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.permissioncheck.screen.model.PermissionCheckAction as Action
import com.android.messaging.ui.permissioncheck.screen.model.PermissionCheckScreenEffect as Effect
import com.android.messaging.ui.permissioncheck.screen.model.PermissionCheckUiState as State
import com.android.messaging.ui.permissioncheck.screen.model.SettingsGuidance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val HeroIconSize = 96.dp
private val ScreenPadding = 24.dp

@Composable
internal fun PermissionCheckScreen(
    effectHandler: PermissionCheckEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: PermissionCheckScreenModel = viewModel<PermissionCheckViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    val currentEffectHandler by rememberUpdatedState(effectHandler)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        screenModel.onRequestResult()
    }

    val smsRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        screenModel.onRequestResult()
    }

    LaunchedEffect(screenModel) {
        screenModel.effects.collect { effect ->
            when (effect) {
                is Effect.RequestRuntimePermissions -> {
                    permissionLauncher.launch(effect.permissions.toTypedArray())
                }

                Effect.RequestSmsRole -> {
                    smsRoleLauncher.launch(currentEffectHandler.createSmsRoleIntent())
                }

                else -> currentEffectHandler.handle(effect)
            }
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        screenModel.onScreenResumed()
    }

    PermissionCheckContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
private fun PermissionCheckContent(
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PermissionHero()

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.required_permissions_promo),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                uiState.settingsGuidance?.let { guidance ->
                    Spacer(modifier = Modifier.height(24.dp))

                    PermissionSettingsGuidance(
                        guidance = guidance,
                        missingPermissions = uiState.missingPermissions,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PermissionCheckActions(
                showSettingsGuidance = uiState.settingsGuidance != null,
                onAction = onAction,
                onNavigateBack = onNavigateBack,
            )
        }
    }
}

@Composable
private fun PermissionHero() {
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        modifier = Modifier
            .size(HeroIconSize)
            .clip(CircleShape)
            .background(colorResource(R.color.ic_launcher_background)),
    )
}

@Composable
private fun PermissionSettingsGuidance(
    guidance: SettingsGuidance,
    missingPermissions: ImmutableList<String>,
) {
    val textRes = when (guidance) {
        SettingsGuidance.DefaultSmsApp -> R.string.set_default_sms_app_procedure
        SettingsGuidance.Permissions -> R.string.enable_permission_procedure
    }
    val descriptionRes = when (guidance) {
        SettingsGuidance.DefaultSmsApp -> R.string.set_default_sms_app_procedure_description
        SettingsGuidance.Permissions -> R.string.enable_permission_procedure_description
    }
    val appName = stringResource(R.string.app_name)
    val description = stringResource(descriptionRes, appName)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Text(
                text = stringResource(textRes, appName),
                modifier = Modifier
                    .padding(16.dp)
                    .semantics { contentDescription = description },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (missingPermissions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.missing_permissions_list,
                    missingPermissions.joinToString(separator = ", "),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PermissionCheckActions(
    showSettingsGuidance: Boolean,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val primaryActionLabel = when {
        showSettingsGuidance -> R.string.settings
        else -> R.string.next
    }
    val onPrimaryActionClick = when {
        showSettingsGuidance -> Action.SettingsClicked
        else -> Action.NextClicked
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = { onAction(onPrimaryActionClick) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(primaryActionLabel))
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.exit))
        }
    }
}

@PreviewLightDark
@Composable
private fun PermissionCheckContentPreview() {
    MessagingPreviewTheme {
        PermissionCheckContent(
            uiState = State(settingsGuidance = null),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PermissionCheckContentDefaultSmsAppGuidancePreview() {
    MessagingPreviewTheme {
        PermissionCheckContent(
            uiState = State(settingsGuidance = SettingsGuidance.DefaultSmsApp),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PermissionCheckContentPermissionsGuidancePreview() {
    MessagingPreviewTheme {
        PermissionCheckContent(
            uiState = State(
                settingsGuidance = SettingsGuidance.Permissions,
                missingPermissions = persistentListOf(
                    "read your text messages",
                    "read your contacts",
                ),
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
