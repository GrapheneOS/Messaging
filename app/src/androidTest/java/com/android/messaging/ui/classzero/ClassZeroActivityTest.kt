package com.android.messaging.ui.classzero

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.ui.UIIntents
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ClassZeroActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun launchDisplaysClassZeroMessage() {
        val scenario = ActivityScenario.launch<ClassZeroActivity>(classZeroIntent())

        scenario.use {
            composeTestRule
                .onNodeWithTag(CLASS_ZERO_TITLE_TEST_TAG)
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithText(MESSAGE_TEXT)
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithTag(CLASS_ZERO_SAVE_BUTTON_TEST_TAG)
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithTag(CLASS_ZERO_CANCEL_BUTTON_TEST_TAG)
                .assertIsDisplayed()
        }
    }

    private fun classZeroIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val messageValues = ContentValues().apply {
            put(Sms.BODY, MESSAGE_TEXT)
        }

        return Intent(context, ClassZeroActivity::class.java).apply {
            putExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_VALUES, messageValues)
        }
    }

    private companion object {
        private const val MESSAGE_TEXT = "Class zero activity message"
    }
}
