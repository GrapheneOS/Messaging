package com.android.messaging.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Looper
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BugleWidgetProviderTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private val appWidgetManager = mockk<AppWidgetManager>(relaxed = true)
    private val widgetRebuilder = mockk<WidgetRebuilder>()
    private val rebuiltWidgetIds = slot<IntArray>()

    @Before
    fun setUp() {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManager
        mockkObject(WidgetRebuilder.Companion)
        every { WidgetRebuilder.get(any()) } returns widgetRebuilder
        every { widgetRebuilder.rebuild(capture(rebuiltWidgetIds), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun notifyConversationListChanged_afterProcessRestart_rebuildsTheWidgets() {
        val provider = ComponentName(context, BugleWidgetProvider::class.java)
        every { appWidgetManager.getAppWidgetIds(provider) } returns intArrayOf(WIDGET_ID)

        BugleWidgetProvider.notifyConversationListChanged(context)
        shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 1) { widgetRebuilder.rebuild(any(), any(), any()) }
        assertArrayEquals(intArrayOf(WIDGET_ID), rebuiltWidgetIds.captured)
    }

    companion object {
        private const val WIDGET_ID = 5
    }
}
