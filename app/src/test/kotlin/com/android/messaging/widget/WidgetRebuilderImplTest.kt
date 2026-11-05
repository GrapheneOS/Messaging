@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.messaging.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver.PendingResult
import android.content.Context
import android.widget.RemoteViews
import com.android.messaging.R
import com.android.messaging.data.onboarding.RequiredPermissionsChecker
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class WidgetRebuilderImplTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private val appWidgetManager = mockk<AppWidgetManager>(relaxed = true)
    private val requiredPermissionsChecker = mockk<RequiredPermissionsChecker> {
        every { hasRequiredPermissions() } returns true
    }
    private val builtWidgetIds = mutableListOf<Int>()
    private val bitmapBudgets = mutableListOf<Long>()
    private val builtViews = mutableListOf<RemoteViews>()
    private val viewsBuilder = WidgetViewsBuilder { _, appWidgetId, maxBitmapByteCount ->
        builtWidgetIds += appWidgetId
        bitmapBudgets += maxBitmapByteCount
        RemoteViews(context.packageName, R.layout.widget_conversation).also { builtViews += it }
    }

    @Test
    fun rebuild_widgetQueuedAgainBeforeItsRebuildStarts_buildsEachWidgetOnce() {
        runTest {
            val widgetRebuilder = widgetRebuilder()

            widgetRebuilder.rebuild(intArrayOf(WIDGET_ID, OTHER_WIDGET_ID), viewsBuilder, null)
            widgetRebuilder.rebuild(intArrayOf(OTHER_WIDGET_ID), viewsBuilder, null)
            widgetRebuilder.rebuild(intArrayOf(WIDGET_ID), viewsBuilder, null)
            advanceUntilIdle()

            assertEquals(listOf(WIDGET_ID, OTHER_WIDGET_ID), builtWidgetIds)
            verify(exactly = 2) { appWidgetManager.updateAppWidget(any<Int>(), any<RemoteViews>()) }
        }
    }

    @Test
    fun rebuild_widgetQueuedAgainWhileItRebuilds_buildsItAgainFromTheNewerData() {
        runTest {
            val widgetRebuilder = widgetRebuilder()
            val dataChangingViewsBuilder = object : WidgetViewsBuilder {
                override fun build(
                    context: Context,
                    appWidgetId: Int,
                    maxBitmapByteCount: Long,
                ): RemoteViews {
                    if (builtWidgetIds.isEmpty()) {
                        widgetRebuilder.rebuild(intArrayOf(appWidgetId), this, null)
                    }
                    return viewsBuilder.build(context, appWidgetId, maxBitmapByteCount)
                }
            }

            widgetRebuilder.rebuild(intArrayOf(WIDGET_ID), dataChangingViewsBuilder, null)
            advanceUntilIdle()

            assertEquals(listOf(WIDGET_ID, WIDGET_ID), builtWidgetIds)
        }
    }

    @Test
    fun rebuild_viewsBuilt_replacesTheWholeWidget() {
        runTest {
            val widgetRebuilder = widgetRebuilder()

            widgetRebuilder.rebuild(intArrayOf(WIDGET_ID), viewsBuilder, null)
            advanceUntilIdle()

            verify(exactly = 1) { appWidgetManager.updateAppWidget(WIDGET_ID, builtViews.single()) }
            verify(exactly = 0) { appWidgetManager.partiallyUpdateAppWidget(any<Int>(), any()) }
            verify(exactly = 0) {
                appWidgetManager.notifyAppWidgetViewDataChanged(any<Int>(), any<Int>())
            }
        }
    }

    @Test
    fun rebuild_withoutRequiredPermissions_showsTheMissingPermissionView() {
        runTest {
            every { requiredPermissionsChecker.hasRequiredPermissions() } returns false
            val updatedViews = slot<RemoteViews>()
            every { appWidgetManager.updateAppWidget(WIDGET_ID, capture(updatedViews)) } just Runs
            val widgetRebuilder = widgetRebuilder()

            widgetRebuilder.rebuild(intArrayOf(WIDGET_ID), viewsBuilder, null)
            advanceUntilIdle()

            assertEquals(R.layout.widget_missing_permission, updatedViews.captured.layoutId)
            assertTrue(builtWidgetIds.isEmpty())
        }
    }

    @Test
    fun rebuild_viewsPassTheSystemsBitmapLimit_retriesWithAQuarterOfTheBitmapBudget() {
        runTest {
            every {
                appWidgetManager.updateAppWidget(WIDGET_ID, any<RemoteViews>())
            } throws IllegalArgumentException(BITMAP_LIMIT_MESSAGE) andThen Unit
            val widgetRebuilder = widgetRebuilder()

            widgetRebuilder.rebuild(intArrayOf(WIDGET_ID), viewsBuilder, null)
            advanceUntilIdle()

            val displayMetrics = context.resources.displayMetrics
            val screenByteCount = 4L * displayMetrics.widthPixels * displayMetrics.heightPixels
            assertEquals(listOf(screenByteCount, screenByteCount / 4), bitmapBudgets)
            verify(exactly = 1) { appWidgetManager.updateAppWidget(WIDGET_ID, builtViews.last()) }
        }
    }

    @Test
    fun rebuild_withPendingResult_finishesItOnceTheWidgetsAreUpdated() {
        runTest {
            val pendingResult = mockk<PendingResult>(relaxed = true)
            val widgetRebuilder = widgetRebuilder()

            widgetRebuilder.rebuild(intArrayOf(WIDGET_ID), viewsBuilder, pendingResult)

            verify(exactly = 0) { pendingResult.finish() }
            advanceUntilIdle()
            verifyOrder {
                appWidgetManager.updateAppWidget(WIDGET_ID, any<RemoteViews>())
                pendingResult.finish()
            }
        }
    }

    private fun TestScope.widgetRebuilder(): WidgetRebuilderImpl {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return WidgetRebuilderImpl(
            context = context,
            appWidgetManager = appWidgetManager,
            requiredPermissionsChecker = requiredPermissionsChecker,
            applicationScope = this,
            ioDispatcher = dispatcher,
        )
    }

    private companion object {
        private const val WIDGET_ID = 3
        private const val OTHER_WIDGET_ID = 4
        private const val BITMAP_LIMIT_MESSAGE = "RemoteViews for widget update exceeds maximum " +
            "bitmap memory usage"
    }
}
