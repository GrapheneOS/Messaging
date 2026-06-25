package com.android.messaging.ui.smsstoragelow

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import com.android.messaging.domain.smsstoragelow.model.SmsStorageRetentionDuration
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.smsstoragelow.model.SmsStorageLowWarningScreenEffect
import com.android.messaging.ui.smsstoragelow.model.SmsStorageLowWarningUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SmsStorageLowWarningScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val mediaAction = SmsStorageLowWarningAction.DeleteMediaMessages(
        retentionDuration = retentionDuration(),
    )
    private val oldMessagesAction = SmsStorageLowWarningAction.DeleteOldMessages(
        retentionDuration = retentionDuration(),
    )
    private val actions = persistentListOf(mediaAction, oldMessagesAction)
    private val uiStateFlow = MutableStateFlow(
        SmsStorageLowWarningUiState(
            actions = actions,
            isLoading = false,
        ),
    )
    private val effectsFlow = MutableSharedFlow<SmsStorageLowWarningScreenEffect>(
        extraBufferCapacity = 1,
    )
    private val finishCalls = mutableListOf<Unit>()

    private lateinit var screenModel: SmsStorageLowWarningScreenModel

    @Before
    fun setUp() {
        screenModel = mockk(relaxed = true)
        every { screenModel.uiState } returns uiStateFlow
        every { screenModel.effects } returns effectsFlow
    }

    @Test
    fun content_showsWarningAndActions() {
        renderScreen()

        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_TITLE_TEST_TAG)
            .assertTextEquals(string(resId = R.string.sms_storage_low_title))

        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_MESSAGE_TEST_TAG)
            .assertTextEquals(string(resId = R.string.sms_storage_low_text))

        composeTestRule
            .onNodeWithTag(
                testTag = smsStorageLowWarningActionTestTag(action = mediaAction),
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(
                testTag = smsStorageLowWarningActionTestTag(action = oldMessagesAction),
            )
            .assertIsDisplayed()
    }

    @Test
    fun loading_showsProgressAndHidesActions() {
        updateState(SmsStorageLowWarningUiState(isLoading = true))

        renderScreen()

        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_LOADING_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(
                testTag = smsStorageLowWarningActionTestTag(action = mediaAction),
            )
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithTag(
                testTag = smsStorageLowWarningActionTestTag(action = oldMessagesAction),
            )
            .assertDoesNotExist()
    }

    @Test
    fun actionClick_callsScreenModel() {
        renderScreen()

        composeTestRule
            .onNodeWithTag(
                testTag = smsStorageLowWarningActionTestTag(action = mediaAction),
            )
            .performClick()

        verify {
            screenModel.onActionClicked(mediaAction)
        }
    }

    @Test
    fun ignoreClick_callsScreenModel() {
        renderScreen()

        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_IGNORE_BUTTON_TEST_TAG)
            .performClick()

        verify {
            screenModel.onIgnoreClicked()
        }
    }

    @Test
    fun choiceDialog_outsideClick_doesNotDismiss() {
        renderScreen()

        composeTestRule
            .onAllNodes(matcher = isRoot())[1]
            .performTouchInput {
                click(position = Offset(x = 0f, y = 0f))
            }

        verify(exactly = 0) {
            screenModel.onIgnoreClicked()
        }
    }

    @Test
    fun selectedAction_showsConfirmationDialog() {
        updateState(
            SmsStorageLowWarningUiState(
                actions = actions,
                selectedAction = oldMessagesAction,
                isLoading = false,
            ),
        )
        renderScreen()

        val duration = quantityString(
            resId = R.plurals.month_count,
            quantity = oldMessagesAction.retentionDuration.count,
            oldMessagesAction.retentionDuration.count,
        )

        composeTestRule
            .onNodeWithTag(
                testTag = SMS_STORAGE_LOW_WARNING_CONFIRMATION_DIALOG_TEST_TAG,
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(
                testTag = SMS_STORAGE_LOW_WARNING_CONFIRMATION_MESSAGE_TEST_TAG,
            )
            .assertTextEquals(
                string(
                    resId = R.string.delete_oldest_messages_confirmation,
                    duration,
                ),
            )
    }

    @Test
    fun confirmationDialog_outsideClick_doesNotDismiss() {
        updateState(
            SmsStorageLowWarningUiState(
                actions = actions,
                selectedAction = mediaAction,
                isLoading = false,
            ),
        )
        renderScreen()

        composeTestRule
            .onAllNodes(matcher = isRoot())[1]
            .performTouchInput {
                click(position = Offset(x = 0f, y = 0f))
            }

        verify(exactly = 0) {
            screenModel.onConfirmationDismissed()
        }
    }

    @Test
    fun choiceDialog_whenHeightConstrained_scrollsToIgnoreButton() {
        renderScreen(modifier = Modifier.height(height = 140.dp))

        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_IGNORE_BUTTON_TEST_TAG)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun confirmationDialog_whenHeightConstrained_scrollsToCancelButton() {
        updateState(
            SmsStorageLowWarningUiState(
                actions = actions,
                selectedAction = mediaAction,
                isLoading = false,
            ),
        )

        renderScreen(modifier = Modifier.height(height = 140.dp))

        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_CANCEL_BUTTON_TEST_TAG)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun confirmationButtons_callScreenModel() {
        updateState(
            SmsStorageLowWarningUiState(
                actions = actions,
                selectedAction = mediaAction,
                isLoading = false,
            ),
        )
        renderScreen()

        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_CONFIRM_BUTTON_TEST_TAG)
            .performClick()
        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_CANCEL_BUTTON_TEST_TAG)
            .performClick()

        verify { screenModel.onCleanupConfirmed() }
        verify { screenModel.onConfirmationDismissed() }
    }

    @Test
    fun processing_disablesChoiceActionsAndIgnoreButton() {
        updateState(
            SmsStorageLowWarningUiState(
                actions = actions,
                isLoading = false,
                isProcessing = true,
            ),
        )
        renderScreen()

        composeTestRule
            .onNodeWithTag(
                testTag = smsStorageLowWarningActionTestTag(action = mediaAction),
            )
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithTag(
                testTag = smsStorageLowWarningActionTestTag(action = oldMessagesAction),
            )
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_IGNORE_BUTTON_TEST_TAG)
            .assertIsNotEnabled()
    }

    @Test
    fun processing_disablesConfirmationButtons() {
        updateState(
            SmsStorageLowWarningUiState(
                actions = actions,
                selectedAction = mediaAction,
                isLoading = false,
                isProcessing = true,
            ),
        )
        renderScreen()

        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_CONFIRM_BUTTON_TEST_TAG)
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithTag(testTag = SMS_STORAGE_LOW_WARNING_CANCEL_BUTTON_TEST_TAG)
            .assertIsNotEnabled()
    }

    @Test
    fun finishEffect_callsOnFinish() {
        renderScreen()

        composeTestRule.runOnIdle {
            effectsFlow.tryEmit(SmsStorageLowWarningScreenEffect.Finish)
        }
        composeTestRule.waitForIdle()

        assertEquals(1, finishCalls.size)
    }

    private fun renderScreen(modifier: Modifier = Modifier) {
        composeTestRule.setContent {
            AppTheme {
                SmsStorageLowWarningScreen(
                    onFinish = { finishCalls += Unit },
                    modifier = modifier,
                    screenModel = screenModel,
                )
            }
        }
    }

    private fun updateState(state: SmsStorageLowWarningUiState) {
        uiStateFlow.value = state
    }

    private fun string(resId: Int, vararg formatArgs: Any): String {
        return composeTestRule.activity.getString(resId, *formatArgs)
    }

    private fun quantityString(resId: Int, quantity: Int, vararg formatArgs: Any): String {
        return composeTestRule.activity.resources.getQuantityString(
            resId,
            quantity,
            *formatArgs,
        )
    }

    private fun retentionDuration(): SmsStorageRetentionDuration {
        return SmsStorageRetentionDuration(
            count = 1,
            unit = SmsStorageRetentionDuration.DurationUnit.MONTH,
            millis = 30L * 24L * 60L * 60L * 1000L,
        )
    }
}
