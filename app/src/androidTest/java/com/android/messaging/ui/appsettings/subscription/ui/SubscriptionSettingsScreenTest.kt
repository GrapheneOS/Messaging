package com.android.messaging.ui.appsettings.subscription.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.appsettings.screen.SettingsScreenModel
import com.android.messaging.ui.appsettings.screen.model.SettingsAction as Action
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SubscriptionSettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var screenModel: SettingsScreenModel

    @Before
    fun setup() {
        screenModel = mockk(relaxed = true)
    }

    @Test
    fun mmsCategoryHeader_isDisplayed() {
        setContent(subscriptionSettings = createDefaultSubscription())

        val mmsTitle = composeTestRule.activity.getString(
            R.string.mms_messaging_category_pref_title,
        )
        composeTestRule.onNodeWithText(mmsTitle).assertIsDisplayed()
    }

    @Test
    fun groupMms_shownWhenSupported() {
        val sub = createDefaultSubscription(isGroupMmsSupported = true)
        setContent(subscriptionSettings = sub)

        val groupMmsTitle = composeTestRule.activity.getString(R.string.group_mms_pref_title)
        composeTestRule.onNodeWithText(groupMmsTitle).assertIsDisplayed()
    }

    @Test
    fun groupMms_hiddenWhenNotSupported() {
        val sub = createDefaultSubscription(isGroupMmsSupported = false)
        setContent(subscriptionSettings = sub)

        val groupMmsTitle = composeTestRule.activity.getString(R.string.group_mms_pref_title)
        composeTestRule.onNodeWithText(groupMmsTitle).assertDoesNotExist()
    }

    @Test
    fun groupMmsClick_showsDialog() {
        val sub = createDefaultSubscription(
            isGroupMmsSupported = true,
            isDefaultSmsApp = true,
        )
        setContent(subscriptionSettings = sub)

        val groupMmsTitle = composeTestRule.activity.getString(R.string.group_mms_pref_title)
        composeTestRule.onNodeWithText(groupMmsTitle).performClick()
        composeTestRule.waitForIdle()

        val disableLabel = composeTestRule.activity.getString(R.string.disable_group_mms)
        composeTestRule.onNodeWithText(disableLabel).assertIsDisplayed()

        val okText = composeTestRule.activity.getString(android.R.string.ok)
        composeTestRule.onNodeWithText(okText).assertIsDisplayed()

        val cancelText = composeTestRule.activity.getString(android.R.string.cancel)
        composeTestRule.onNodeWithText(cancelText).assertIsDisplayed()
    }

    @Test
    fun phoneNumberItem_displaysCurrentNumber() {
        val sub = createDefaultSubscription(displayDetail = "+1234567890")
        setContent(subscriptionSettings = sub)

        composeTestRule.onNodeWithText("+1234567890").assertIsDisplayed()
    }

    @Test
    fun phoneNumberClick_showsDialog() {
        val sub = createDefaultSubscription(phoneNumber = "+1234567890")
        setContent(subscriptionSettings = sub)

        val phoneTitle = composeTestRule.activity.getString(R.string.mms_phone_number_pref_title)
        composeTestRule.onNodeWithText(phoneTitle).performClick()
        composeTestRule.waitForIdle()

        val okText = composeTestRule.activity.getString(android.R.string.ok)
        composeTestRule.onNodeWithText(okText).assertIsDisplayed()
    }

    @Test
    fun autoRetrieveMms_toggleDelegatesToScreenModel() {
        val sub = createDefaultSubscription(
            isDefaultSmsApp = true,
            autoRetrieveMms = true,
        )
        setContent(subscriptionSettings = sub)

        val title = composeTestRule.activity.getString(R.string.auto_retrieve_mms_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.AutoRetrieveMmsChanged(1, false))
        }
    }

    @Test
    fun autoRetrieveMmsWhenRoaming_disabledWhenAutoRetrieveOff() {
        val sub = createDefaultSubscription(
            isDefaultSmsApp = true,
            autoRetrieveMms = false,
        )
        setContent(subscriptionSettings = sub)

        val title = composeTestRule.activity.getString(
            R.string.auto_retrieve_mms_when_roaming_pref_title,
        )
        composeTestRule.onNodeWithText(title).assertIsNotEnabled()
    }

    @Test
    fun autoRetrieveMmsWhenRoaming_enabledWhenAutoRetrieveOn() {
        val sub = createDefaultSubscription(
            isDefaultSmsApp = true,
            autoRetrieveMms = true,
        )
        setContent(subscriptionSettings = sub)

        val title = composeTestRule.activity.getString(
            R.string.auto_retrieve_mms_when_roaming_pref_title,
        )
        composeTestRule.onNodeWithText(title).assertIsEnabled()
    }

    @Test
    fun deliveryReports_shownWhenSupported() {
        val sub = createDefaultSubscription(isDeliveryReportsSupported = true)
        setContent(subscriptionSettings = sub)

        val title = composeTestRule.activity.getString(R.string.delivery_reports_pref_title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun deliveryReports_hiddenWhenNotSupported() {
        val sub = createDefaultSubscription(isDeliveryReportsSupported = false)
        setContent(subscriptionSettings = sub)

        val title = composeTestRule.activity.getString(R.string.delivery_reports_pref_title)
        composeTestRule.onNodeWithText(title).assertDoesNotExist()
    }

    @Test
    fun deliveryReportsToggle_delegatesToScreenModel() {
        val sub = createDefaultSubscription(
            isDeliveryReportsSupported = true,
            isDefaultSmsApp = true,
            deliveryReportsEnabled = false,
        )
        setContent(subscriptionSettings = sub)

        val title = composeTestRule.activity.getString(R.string.delivery_reports_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.DeliveryReportsChanged(1, true))
        }
    }

    @Test
    fun wirelessAlerts_shownWhenSupported() {
        val sub = createDefaultSubscription(isWirelessAlertsSupported = true)
        setContent(subscriptionSettings = sub)

        val title = composeTestRule.activity.getString(R.string.wireless_alerts_title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun wirelessAlerts_hiddenWhenNotSupported() {
        val sub = createDefaultSubscription(isWirelessAlertsSupported = false)
        setContent(subscriptionSettings = sub)

        val title = composeTestRule.activity.getString(R.string.wireless_alerts_title)
        composeTestRule.onNodeWithText(title).assertDoesNotExist()
    }

    @Test
    fun wirelessAlertsClick_delegatesToScreenModel() {
        val sub = createDefaultSubscription(isWirelessAlertsSupported = true)
        setContent(subscriptionSettings = sub)

        val title = composeTestRule.activity.getString(R.string.wireless_alerts_title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.WirelessAlertsClicked(1))
        }
    }

    @Test
    fun advancedCategory_shownWhenDeliveryReportsOrWirelessAlertsSupported() {
        val sub = createDefaultSubscription(
            isDeliveryReportsSupported = true,
            isWirelessAlertsSupported = false,
        )
        setContent(subscriptionSettings = sub)

        val advancedTitle =
            composeTestRule.activity.getString(R.string.advanced_category_pref_title)
        composeTestRule.onNodeWithText(advancedTitle).assertIsDisplayed()
    }

    @Test
    fun advancedCategory_hiddenWhenNeitherSupported() {
        val sub = createDefaultSubscription(
            isDeliveryReportsSupported = false,
            isWirelessAlertsSupported = false,
        )
        setContent(subscriptionSettings = sub)

        val advancedTitle =
            composeTestRule.activity.getString(R.string.advanced_category_pref_title)
        composeTestRule.onNodeWithText(advancedTitle).assertDoesNotExist()
    }

    @Test
    fun settingsDisabled_whenNotDefaultSmsApp() {
        val sub = createDefaultSubscription(
            isDefaultSmsApp = false,
            isGroupMmsSupported = true,
            isDeliveryReportsSupported = true,
        )
        setContent(subscriptionSettings = sub)

        val groupMmsTitle = composeTestRule.activity.getString(R.string.group_mms_pref_title)
        composeTestRule.onNodeWithText(groupMmsTitle).assertIsNotEnabled()

        val autoRetrieveTitle = composeTestRule.activity.getString(
            R.string.auto_retrieve_mms_pref_title,
        )
        composeTestRule.onNodeWithText(autoRetrieveTitle).assertIsNotEnabled()

        val deliveryTitle = composeTestRule.activity.getString(R.string.delivery_reports_pref_title)
        composeTestRule.onNodeWithText(deliveryTitle).assertIsNotEnabled()
    }

    private fun setContent(
        subscriptionSettings: SubscriptionSettingsUiState = createDefaultSubscription(),
    ) {
        composeTestRule.setContent {
            AppTheme {
                SubscriptionSettingsScreen(
                    subscriptionSettings = subscriptionSettings,
                    title = "Advanced Settings",
                    screenModel = screenModel,
                    onNavigateBack = {},
                )
            }
        }
    }

    private fun createDefaultSubscription(
        subId: Int = 1,
        displayDetail: String = "+1234567890",
        phoneNumber: String = "+1234567890",
        defaultPhoneNumber: String = "+1234567890",
        isGroupMmsSupported: Boolean = false,
        isGroupMmsEnabled: Boolean = true,
        autoRetrieveMms: Boolean = true,
        autoRetrieveMmsWhenRoaming: Boolean = false,
        isDeliveryReportsSupported: Boolean = false,
        deliveryReportsEnabled: Boolean = false,
        isWirelessAlertsSupported: Boolean = false,
        isDefaultSmsApp: Boolean = true,
    ): SubscriptionSettingsUiState {
        return SubscriptionSettingsUiState(
            subId = subId,
            displayName = "SIM 1",
            displayDetail = displayDetail,
            phoneNumber = phoneNumber,
            defaultPhoneNumber = defaultPhoneNumber,
            isGroupMmsSupported = isGroupMmsSupported,
            isGroupMmsEnabled = isGroupMmsEnabled,
            autoRetrieveMms = autoRetrieveMms,
            autoRetrieveMmsWhenRoaming = autoRetrieveMmsWhenRoaming,
            isDeliveryReportsSupported = isDeliveryReportsSupported,
            deliveryReportsEnabled = deliveryReportsEnabled,
            isWirelessAlertsSupported = isWirelessAlertsSupported,
            isDefaultSmsApp = isDefaultSmsApp,
        )
    }
}
