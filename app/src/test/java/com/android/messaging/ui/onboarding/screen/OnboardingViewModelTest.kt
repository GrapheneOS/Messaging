package com.android.messaging.ui.onboarding.screen

import app.cash.turbine.test
import com.android.messaging.data.onboarding.GetMissingPermissionLabels
import com.android.messaging.data.onboarding.RequiredPermissionsChecker
import com.android.messaging.data.onboarding.store.SmsWarningStore
import com.android.messaging.domain.onboarding.model.PermissionRequest
import com.android.messaging.domain.onboarding.usecase.DeterminePermissionRequest
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.onboarding.screen.model.OnboardingAction as Action
import com.android.messaging.ui.onboarding.screen.model.OnboardingScreenEffect as Effect
import com.android.messaging.ui.onboarding.screen.model.OnboardingUiState as State
import com.android.messaging.ui.onboarding.screen.model.SettingsGuidance
import com.android.messaging.util.core.ElapsedRealtimeProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialState_whenWarningNotAcknowledged_isSmsWarning() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(
                smsWarningStore = mockSmsWarningStore(isAcknowledged = false),
            )

            assertEquals(State.SmsWarning, viewModel.uiState.value)
        }
    }

    @Test
    fun initialState_whenWarningAcknowledged_isPermissions() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(
                smsWarningStore = mockSmsWarningStore(isAcknowledged = true),
            )

            assertEquals(State.Permissions(), viewModel.uiState.value)
        }
    }

    @Test
    fun onScreenResumed_onSmsWarningStep_whenPermissionsGranted_doesNotRedirect() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(
                checker = mockChecker(hasRequiredPermissions = true),
                smsWarningStore = mockSmsWarningStore(isAcknowledged = false),
            )

            viewModel.effects.test {
                viewModel.onScreenResumed()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(State.SmsWarning, viewModel.uiState.value)
        }
    }

    @Test
    fun nextClicked_onSmsWarningStep_whenPermissionsMissing_acknowledgesAndShowsPermissions() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val smsWarningStore = mockSmsWarningStore(isAcknowledged = false)
            val viewModel = createViewModel(
                checker = mockChecker(hasRequiredPermissions = false),
                smsWarningStore = smsWarningStore,
            )

            viewModel.effects.test {
                viewModel.onAction(Action.NextClicked)

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }

            verify { smsWarningStore.acknowledge() }
            assertEquals(State.Permissions(), viewModel.uiState.value)
        }
    }

    @Test
    fun nextClicked_onSmsWarningStep_whenPermissionsGranted_acknowledgesAndRedirects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val smsWarningStore = mockSmsWarningStore(isAcknowledged = false)
            val viewModel = createViewModel(
                checker = mockChecker(hasRequiredPermissions = true),
                smsWarningStore = smsWarningStore,
            )

            viewModel.effects.test {
                viewModel.onAction(Action.NextClicked)

                assertEquals(Effect.Redirect, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            verify { smsWarningStore.acknowledge() }
        }
    }

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

            assertNull(viewModel.permissionsState().settingsGuidance)
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

            val state = viewModel.permissionsState()
            assertEquals(SettingsGuidance.DefaultSmsApp, state.settingsGuidance)
            assertEquals(persistentListOf<String>(), state.missingPermissions)
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

            val state = viewModel.permissionsState()
            assertEquals(SettingsGuidance.Permissions, state.settingsGuidance)
            assertEquals(labels, state.missingPermissions)
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

            assertNull(viewModel.permissionsState().settingsGuidance)
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

            assertNull(viewModel.permissionsState().settingsGuidance)
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
        smsWarningStore: SmsWarningStore = mockSmsWarningStore(isAcknowledged = true),
    ): OnboardingViewModel {
        return OnboardingViewModel(
            checker = checker,
            determinePermissionRequest = determinePermissionRequest,
            getMissingPermissionLabels = getMissingPermissionLabels,
            elapsedRealtimeProvider = elapsedRealtimeProvider,
            smsWarningStore = smsWarningStore,
        )
    }

    private fun OnboardingViewModel.permissionsState(): State.Permissions {
        return uiState.value as State.Permissions
    }

    private fun mockSmsWarningStore(isAcknowledged: Boolean): SmsWarningStore {
        return mockk<SmsWarningStore>(relaxed = true).also {
            every { it.isAcknowledged() } returns isAcknowledged
        }
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
