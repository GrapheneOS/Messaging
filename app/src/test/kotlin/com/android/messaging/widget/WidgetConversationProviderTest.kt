package com.android.messaging.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContextWrapper
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.testutil.installTestFactory
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WidgetConversationProviderTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private val appWidgetManager = mockk<AppWidgetManager>(relaxed = true)

    @Before
    fun setUp() {
        installTestFactory(context = context)
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManager
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    /**
     * Reading the conversation can create the database, and creating it resets every widget. A
     * rebuild already past its check for the conversation must not put that conversation back
     * over the reset, or the widget sits on a conversation that no longer exists.
     */
    @Test
    fun rebuildWidget_whenReadingTheConversationResetsTheWidget_leavesItToBeConfigured() {
        WidgetConversationPrefs.saveConversationIdPref(
            appWidgetId = WIDGET_ID,
            conversationId = "7",
        )
        val contentResolver = mockk<ContentResolver>()
        every { contentResolver.query(any(), any(), any(), any(), any()) } answers {
            WidgetConversationPrefs.deleteConversationIdPref(WIDGET_ID)
            null
        }
        val resettingContext = object : ContextWrapper(context) {
            override fun getContentResolver(): ContentResolver {
                return contentResolver
            }
        }
        val updates = mutableListOf<RemoteViews>()
        every { appWidgetManager.updateAppWidget(WIDGET_ID, capture(updates)) } just Runs

        // Off the main thread, as only there does it read the conversation
        thread { WidgetConversationProvider.rebuildWidget(resettingContext, WIDGET_ID) }.join()

        val widget = updates.last().apply(context, FrameLayout(context))
        assertEquals(View.VISIBLE, widget.findViewById<View>(R.id.widget_configuration).visibility)
    }

    @Test
    fun notifyMessagesChanged_afterProcessRestart_refreshesOnlyThatConversationsWidgets() {
        WidgetConversationPrefs.saveConversationIdPref(
            appWidgetId = WIDGET_ID,
            conversationId = "7",
        )
        WidgetConversationPrefs.saveConversationIdPref(
            appWidgetId = OTHER_WIDGET_ID,
            conversationId = "8",
        )
        val provider = ComponentName(context, WidgetConversationProvider::class.java)
        every { appWidgetManager.getAppWidgetIds(provider) } returns
            intArrayOf(WIDGET_ID, OTHER_WIDGET_ID)

        WidgetConversationProvider.notifyMessagesChanged(context, "7")
        shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 1) {
            appWidgetManager.notifyAppWidgetViewDataChanged(WIDGET_ID, R.id.message_list)
        }
        verify(exactly = 0) {
            appWidgetManager.notifyAppWidgetViewDataChanged(OTHER_WIDGET_ID, any<Int>())
        }
    }

    companion object {
        private const val WIDGET_ID = 3
        private const val OTHER_WIDGET_ID = 4
    }
}
