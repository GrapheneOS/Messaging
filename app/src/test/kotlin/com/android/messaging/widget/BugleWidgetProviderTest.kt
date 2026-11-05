package com.android.messaging.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Looper
import com.android.messaging.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
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

    @Before
    fun setUp() {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun notifyConversationListChanged_afterProcessRestart_refreshesTheWidgets() {
        val provider = ComponentName(context, BugleWidgetProvider::class.java)
        every { appWidgetManager.getAppWidgetIds(provider) } returns intArrayOf(WIDGET_ID)

        BugleWidgetProvider.notifyConversationListChanged(context)
        shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 1) {
            appWidgetManager.notifyAppWidgetViewDataChanged(
                intArrayOf(WIDGET_ID),
                R.id.conversation_list,
            )
        }
    }

    companion object {
        private const val WIDGET_ID = 5
    }
}
