package com.android.messaging.ui.conversation.screen.effects

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenDefaultSmsRoleEffectTest :
    BaseConversationScreenEffectsActionTest() {

    @Test
    fun requestDefaultSmsRole_notSendingReplacesExistingSnackbar() {
        val expectedMessage = targetContext.getString(R.string.requires_default_sms_app)
        setEffectsContent(initialSnackbarMessage = OLD_SNACKBAR_MESSAGE)
        waitForSnackbarMessage(message = OLD_SNACKBAR_MESSAGE)

        emitEffect(
            ConversationScreenEffect.RequestDefaultSmsRole(
                isSending = false,
            ),
        )
        waitForSnackbarMessage(message = expectedMessage)

        composeTestRule.runOnIdle {
            assertEquals(
                expectedMessage,
                snackbarHostState.currentSnackbarData?.visuals?.message,
            )
        }
    }

    @Test
    fun requestDefaultSmsRole_sendingUsesSendSpecificMessage() {
        val expectedMessage = targetContext.getString(R.string.requires_default_sms_app_to_send)
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.RequestDefaultSmsRole(
                isSending = true,
            ),
        )
        waitForSnackbarMessage(message = expectedMessage)

        composeTestRule
            .onNodeWithText(expectedMessage)
            .assertIsDisplayed()
    }

    @Test
    fun requestDefaultSmsRole_actionPerformedCallsPromptAction() {
        val actionText = targetContext.getString(R.string.requires_default_sms_change_button)
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.RequestDefaultSmsRole(
                isSending = false,
            ),
        )
        composeTestRule
            .onNodeWithText(actionText)
            .assertIsDisplayed()
            .performClick()

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            screenModel.onDefaultSmsRolePromptActionClick()
        }
    }

    @Test
    fun defaultSmsRoleLauncherResult_forwardsResultCodeToScreenModel() {
        setEffectsContent()

        dispatchDefaultSmsRoleResult(resultCode = Activity.RESULT_OK)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onDefaultSmsRoleRequestResult(resultCode = Activity.RESULT_OK)
            }
        }
    }

    @Test
    fun launchDefaultSmsRoleRequest_launchesIntent() {
        val intent = Intent(DEFAULT_SMS_ROLE_ACTION)
        setEffectsContent()

        emitEffect(
            ConversationScreenEffect.LaunchDefaultSmsRoleRequest(
                intent = intent,
            ),
        )

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                defaultSmsRoleLauncher.launch(intent, null)
            }
            verify(exactly = 0) {
                screenModel.onDefaultSmsRoleRequestLaunchFailed()
            }
        }
    }

    @Test
    fun launchDefaultSmsRoleRequest_whenLauncherThrowsReportsFailure() {
        val intent = Intent(DEFAULT_SMS_ROLE_ACTION)
        setEffectsContent()
        every {
            defaultSmsRoleLauncher.launch(intent, null)
        } throws ActivityNotFoundException()

        emitEffect(
            ConversationScreenEffect.LaunchDefaultSmsRoleRequest(
                intent = intent,
            ),
        )

        verify(timeout = TEST_WAIT_TIMEOUT_MILLIS, exactly = 1) {
            screenModel.onDefaultSmsRoleRequestLaunchFailed()
        }
    }

    private companion object {
        private const val DEFAULT_SMS_ROLE_ACTION =
            "com.android.messaging.test.DEFAULT_SMS_ROLE"
        private const val OLD_SNACKBAR_MESSAGE = "Existing snackbar"
    }
}
