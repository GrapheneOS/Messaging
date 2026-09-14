package com.android.messaging.ui.conversationpicker.host.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.common.test.rules.AppTestRule
import com.android.common.test.rules.MessagingTestRule
import com.android.messaging.R
import com.android.messaging.datamodel.MediaScratchFileProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ShareIntentActivityUiTest {

    @get:Rule
    val appRule = AppTestRule()

    @get:Rule
    val messagingRule = MessagingTestRule()

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun forwardIntent_displaysForwardMessageTitle() {
        val scenario = ActivityScenario.launch<ShareIntentActivity>(
            ShareIntentActivity.createForwardIntent(
                context = context,
                uri = seededImageUri(),
                contentType = IMAGE_JPEG,
            ),
        )

        scenario.use {
            val title = context.getString(R.string.forward_message_activity_title)

            composeRule.waitUntilAtLeastOneExists(
                matcher = hasText(text = title),
                timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS,
            )
            composeRule.onNodeWithText(text = title).assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun shareIntent_displaysShareTitle() {
        val scenario = ActivityScenario.launch<ShareIntentActivity>(
            Intent(context, ShareIntentActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = IMAGE_JPEG
                putExtra(Intent.EXTRA_STREAM, seededImageUri())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )

        scenario.use {
            val title = context.getString(R.string.share_intent_activity_label)

            composeRule.waitUntilAtLeastOneExists(
                matcher = hasText(text = title),
                timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS,
            )
            composeRule.onNodeWithText(text = title).assertIsDisplayed()
        }
    }

    private fun seededImageUri(): Uri {
        return MediaScratchFileProvider
            .getUriBuilder()
            .appendPath(SEED_IMAGE_FILE_ID)
            .appendQueryParameter(
                SEED_IMAGE_FILE_EXTENSION_QUERY_PARAMETER,
                SEED_IMAGE_FILE_EXTENSION,
            )
            .build()
    }

    private companion object {
        private const val IMAGE_JPEG = "image/jpeg"
        private const val SEED_IMAGE_FILE_EXTENSION = "jpg"
        private const val SEED_IMAGE_FILE_EXTENSION_QUERY_PARAMETER = "ext"
        private const val SEED_IMAGE_FILE_ID = "800001"
        private const val TEST_WAIT_TIMEOUT_MILLIS = 5_000L
    }
}
