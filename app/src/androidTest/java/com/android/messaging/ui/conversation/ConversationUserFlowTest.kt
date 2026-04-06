package com.android.messaging.ui.conversation

import android.os.ParcelFileDescriptor
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.debug.seedTestData
import com.android.messaging.ui.conversationlist.ConversationListActivity
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationUserFlowTest {

    @Before
    fun setUpDefaultSmsApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val command = "cmd role add-role-holder android.app.role.SMS $packageName"
        val parcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)

        ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor).use { inputStream ->
            val result = String(inputStream.readBytes())
            println("Role assignment result: $result")
        }
    }

    @Test
    fun conversationListToConversation_uiElementsArePresent() {
        val scenario = ActivityScenario.launch(
            ConversationListActivity::class.java,
        )

        onView(withId(android.R.id.list))
            .check(matches(isDisplayed()))

        onView(withId(R.id.start_new_conversation_button))
            .check(matches(isDisplayed()))

        onView(withId(android.R.id.list))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()),
            )

        onView(allOf(withId(R.id.compose_message_text), isAssignableFrom(EditText::class.java)))
            .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.attach_media_button), isAssignableFrom(ImageButton::class.java)))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun seedOnce() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            seedTestData(context)
        }
    }
}
