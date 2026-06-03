package com.android.messaging.ui.permissioncheck.screen

import app.cash.turbine.test
import com.android.messaging.data.permissioncheck.GetMissingPermissionLabels
import com.android.messaging.data.permissioncheck.RequiredPermissionsChecker
import com.android.messaging.domain.permissioncheck.model.PermissionRequest
import com.android.messaging.domain.permissioncheck.usecase.DeterminePermissionRequest
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.permissioncheck.screen.model.PermissionCheckAction as Action
import com.android.messaging.ui.permissioncheck.screen.model.PermissionCheckScreenEffect as Effect
import com.android.messaging.ui.permissioncheck.screen.model.SettingsGuidance
import com.android.messaging.util.core.ElapsedRealtimeProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionCheckViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onScreenResumed_whenPermissionsGranted_emitsRedirect() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val checker = mockChecker(hasRequiredPermissions = true)
            val viewModel = createViewModel(checker = checker)

            viewModel.effects.test {
                viewModel.onScreenResumed()

                assertEquals(Effect.Redirect, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onScreenResumed_whenPermissionsMissing_emitsNothing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val checker = mockChecker(hasRequiredPermissions = false)
            val viewModel = createViewModel(checker = checker)

            viewModel.effects.test {
                viewModel.onScreenResumed()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun nextClicked_whenSmsRoleMissing_emitsRequestSmsRole() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val determineRequest = DeterminePermissionRequest { PermissionRequest.SmsRole }
            val viewModel = createViewModel(determinePermissionRequest = determineRequest)

            viewModel.effects.test {
                viewModel.onAction(Action.NextClicked)

                assertEquals(Effect.RequestSmsRole, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun nextClicked_whenRuntimePermissionsMissing_emitsRequestRuntimePermissions() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val permissions = persistentListOf("android.permission.READ_SMS")
            val determineRequest = DeterminePermissionRequest {
                PermissionRequest.RuntimePermissions(permissions)
            }
            val viewModel = createViewModel(determinePermissionRequest = determineRequest)

            viewModel.effects.test {
                viewModel.onAction(Action.NextClicked)

                assertEquals(Effect.RequestRuntimePermissions(permissions), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun nextClicked_whenAlreadyGranted_emitsRedirect() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val determineRequest = DeterminePermissionRequest { PermissionRequest.AlreadyGranted }
            val viewModel = createViewModel(determinePermissionRequest = determineRequest)

            viewModel.effects.test {
                viewModel.onAction(Action.NextClicked)

                assertEquals(Effect.Redirect, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun settingsClicked_emitsOpenAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onAction(Action.SettingsClicked)

                assertEquals(Effect.OpenAppSettings, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onRequestResult_whenPermissionsGranted_emitsRedirectAndKeepsStateClean() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val checker = mockChecker(hasRequiredPermissions = true)
            val viewModel = createViewModel(checker = checker)

            viewModel.effects.test {
                viewModel.onRequestResult()

                assertEquals(Effect.Redirect, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertNull(viewModel.uiState.value.settingsGuidance)
        }
    }

    @Test
    fun onRequestResult_whenSmsRoleDeniedInstantly_showsDefaultSmsAppGuidance() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            // Request started at 1000ms, result arrives at 1100ms -> 100ms elapsed (< 250ms),
            // meaning the system auto-denied without showing the dialog (permanently denied).
            // The SMS role is still not held, so guide the user to set the default SMS app first.
            val checker = mockChecker(hasRequiredPermissions = false, isSmsRoleHeld = false)
            val time = mockTime(startMillis = 1000L, resultMillis = 1100L)
            val determineRequest = DeterminePermissionRequest { PermissionRequest.SmsRole }
            val viewModel = createViewModel(
                checker = checker,
                determinePermissionRequest = determineRequest,
                elapsedRealtimeProvider = time,
            )

            viewModel.effects.test {
                viewModel.onAction(Action.NextClicked)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.onRequestResult()

            assertEquals(SettingsGuidance.DefaultSmsApp, viewModel.uiState.value.settingsGuidance)
            assertEquals(persistentListOf<String>(), viewModel.uiState.value.missingPermissions)
        }
    }

    @Test
    fun onRequestResult_whenPermissionDeniedInstantlyWithSmsRoleHeld_showsPermissionsGuidance() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            // The app already holds the SMS role, so the instant denial is a permanently denied
            // runtime permission. Guide the user to the permissions screen instead.
            val checker = mockChecker(hasRequiredPermissions = false, isSmsRoleHeld = true)
            val time = mockTime(startMillis = 1000L, resultMillis = 1100L)
            val permissions = persistentListOf("android.permission.READ_SMS")
            val determineRequest = DeterminePermissionRequest {
                PermissionRequest.RuntimePermissions(permissions)
            }
            val labels = persistentListOf("SMS", "Contacts")
            val viewModel = createViewModel(
                checker = checker,
                determinePermissionRequest = determineRequest,
                getMissingPermissionLabels = { labels },
                elapsedRealtimeProvider = time,
            )

            viewModel.effects.test {
                viewModel.onAction(Action.NextClicked)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.onRequestResult()

            assertEquals(SettingsGuidance.Permissions, viewModel.uiState.value.settingsGuidance)
            assertEquals(labels, viewModel.uiState.value.missingPermissions)
        }
    }

    @Test
    fun onRequestResult_whenUserSawDialogAndDenied_doesNotShowSettingsGuidance() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            // 5s elapsed (>= 250ms): the user actually interacted with the system dialog and
            // denied, so no settings guidance is shown yet.
            val checker = mockChecker(hasRequiredPermissions = false)
            val time = mockTime(startMillis = 1000L, resultMillis = 6000L)
            val determineRequest = DeterminePermissionRequest { PermissionRequest.SmsRole }
            val viewModel = createViewModel(
                checker = checker,
                determinePermissionRequest = determineRequest,
                elapsedRealtimeProvider = time,
            )

            viewModel.effects.test {
                viewModel.onAction(Action.NextClicked)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.onRequestResult()

            assertNull(viewModel.uiState.value.settingsGuidance)
        }
    }

    @Test
    fun onRequestResult_atThresholdBoundary_doesNotShowSettingsGuidance() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            // Exactly 250ms elapsed: boundary is exclusive (elapsed < threshold), so no guidance.
            val checker = mockChecker(hasRequiredPermissions = false)
            val time = mockTime(startMillis = 1000L, resultMillis = 1250L)
            val determineRequest = DeterminePermissionRequest { PermissionRequest.SmsRole }
            val viewModel = createViewModel(
                checker = checker,
                determinePermissionRequest = determineRequest,
                elapsedRealtimeProvider = time,
            )

            viewModel.effects.test {
                viewModel.onAction(Action.NextClicked)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.onRequestResult()

            assertNull(viewModel.uiState.value.settingsGuidance)
        }
    }

    private fun createViewModel(
        checker: RequiredPermissionsChecker = mockChecker(hasRequiredPermissions = false),
        determinePermissionRequest: DeterminePermissionRequest = DeterminePermissionRequest {
            PermissionRequest.AlreadyGranted
        },
        getMissingPermissionLabels: GetMissingPermissionLabels = GetMissingPermissionLabels {
            persistentListOf()
        },
        elapsedRealtimeProvider: ElapsedRealtimeProvider = ElapsedRealtimeProvider { 0L },
    ): PermissionCheckViewModel {
        return PermissionCheckViewModel(
            checker = checker,
            determinePermissionRequest = determinePermissionRequest,
            getMissingPermissionLabels = getMissingPermissionLabels,
            elapsedRealtimeProvider = elapsedRealtimeProvider,
        )
    }

    private fun mockChecker(
        hasRequiredPermissions: Boolean,
        isSmsRoleHeld: Boolean = false,
    ): RequiredPermissionsChecker {
        return mockk<RequiredPermissionsChecker>(relaxed = true).also {
            every { it.hasRequiredPermissions() } returns hasRequiredPermissions
            every { it.isSmsRoleHeld() } returns isSmsRoleHeld
        }
    }

    private fun mockTime(
        startMillis: Long,
        resultMillis: Long,
    ): ElapsedRealtimeProvider {
        return mockk<ElapsedRealtimeProvider>().also {
            every { it.elapsedRealtimeMillis() } returnsMany listOf(startMillis, resultMillis)
        }
    }
}
