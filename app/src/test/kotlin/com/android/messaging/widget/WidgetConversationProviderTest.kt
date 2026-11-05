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
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertArrayEquals
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
    private val widgetRebuilder = mockk<WidgetRebuilder>()
    private val rebuiltWidgetIds = slot<IntArray>()

    @Before
    fun setUp() {
        installTestFactory(context = context)
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManager
        mockkObject(WidgetRebuilder.Companion)
        every { WidgetRebuilder.get(any()) } returns widgetRebuilder
        every { widgetRebuilder.rebuild(capture(rebuiltWidgetIds), any(), any()) } just Runs
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
    fun buildViews_whenReadingTheConversationResetsTheWidget_leavesItToBeConfigured() {
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

        val views = WidgetConversationProvider.buildViews(
            resettingContext,
            WIDGET_ID,
            Long.MAX_VALUE,
        )

        val widget = views.apply(context, FrameLayout(context))
        assertEquals(View.VISIBLE, widget.findViewById<View>(R.id.widget_configuration).visibility)
    }

    @Test
    fun notifyMessagesChanged_afterProcessRestart_rebuildsOnlyThatConversationsWidgets() {
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

        verify(exactly = 1) { widgetRebuilder.rebuild(any(), any(), any()) }
        assertArrayEquals(intArrayOf(WIDGET_ID), rebuiltWidgetIds.captured)
    }

    @Test
    fun notifyConversationRenamed_queuesTheRebuildInsteadOfBuildingTheWidget() {
        WidgetConversationPrefs.saveConversationIdPref(
            appWidgetId = WIDGET_ID,
            conversationId = "7",
        )
        val provider = ComponentName(context, WidgetConversationProvider::class.java)
        every { appWidgetManager.getAppWidgetIds(provider) } returns intArrayOf(WIDGET_ID)

        WidgetConversationProvider.notifyConversationRenamed(context, "7")

        assertArrayEquals(intArrayOf(WIDGET_ID), rebuiltWidgetIds.captured)
        verify(exactly = 0) { appWidgetManager.updateAppWidget(any<Int>(), any<RemoteViews>()) }
    }

    companion object {
        private const val WIDGET_ID = 3
        private const val OTHER_WIDGET_ID = 4
    }
}
