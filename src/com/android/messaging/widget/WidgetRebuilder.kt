package com.android.messaging.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver.PendingResult
import android.content.Context
import android.widget.RemoteViews
import com.android.messaging.data.onboarding.RequiredPermissionsChecker
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UiUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal fun interface WidgetViewsBuilder {
    fun build(context: Context, appWidgetId: Int, maxBitmapByteCount: Long): RemoteViews
}

internal interface WidgetRebuilder {

    fun rebuild(
        appWidgetIds: IntArray,
        viewsBuilder: WidgetViewsBuilder,
        pendingResult: PendingResult?,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Provider {
        fun widgetRebuilder(): WidgetRebuilder
    }

    companion object {

        @JvmStatic
        fun get(context: Context): WidgetRebuilder {
            return EntryPointAccessors
                .fromApplication(context, Provider::class.java)
                .widgetRebuilder()
        }
    }
}

@Singleton
internal class WidgetRebuilderImpl @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
    private val requiredPermissionsChecker: RequiredPermissionsChecker,
    @ApplicationCoroutineScope
    private val applicationScope: CoroutineScope,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : WidgetRebuilder {

    private val serialDispatcher = ioDispatcher.limitedParallelism(parallelism = 1)
    private val staleWidgetIds = mutableSetOf<Int>()

    override fun rebuild(
        appWidgetIds: IntArray,
        viewsBuilder: WidgetViewsBuilder,
        pendingResult: PendingResult?,
    ) {
        synchronized(staleWidgetIds) {
            staleWidgetIds.addAll(appWidgetIds.asIterable())
        }
        val rebuilds = applicationScope.launch(serialDispatcher) {
            for (appWidgetId in appWidgetIds) {
                if (takeStale(appWidgetId = appWidgetId)) {
                    rebuildNow(appWidgetId = appWidgetId, viewsBuilder = viewsBuilder)
                }
            }
        }
        pendingResult?.let { result ->
            finishAfter(rebuilds = rebuilds, pendingResult = result)
        }
    }

    private fun takeStale(appWidgetId: Int): Boolean {
        return synchronized(staleWidgetIds) {
            staleWidgetIds.remove(appWidgetId)
        }
    }

    private fun rebuildNow(appWidgetId: Int, viewsBuilder: WidgetViewsBuilder) {
        when {
            requiredPermissionsChecker.hasRequiredPermissions() -> {
                updateWithinBitmapLimit(appWidgetId = appWidgetId, viewsBuilder = viewsBuilder)
            }

            else -> {
                appWidgetManager.updateAppWidget(
                    appWidgetId,
                    UiUtils.getWidgetMissingPermissionView(context),
                )
            }
        }
    }

    private fun updateWithinBitmapLimit(appWidgetId: Int, viewsBuilder: WidgetViewsBuilder) {
        val maxBitmapByteCount = maxBitmapByteCount()
        val views = viewsBuilder.build(context, appWidgetId, maxBitmapByteCount)
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (exception: IllegalArgumentException) {
            // The system caps a widget's bitmaps by its own display, which can be smaller than ours
            LogUtil.w(TAG, "Widget $appWidgetId is over the system's bitmap limit", exception)
            val fewerImages = viewsBuilder.build(
                context,
                appWidgetId,
                maxBitmapByteCount / FALLBACK_BITMAP_BUDGET_DIVISOR,
            )
            appWidgetManager.updateAppWidget(appWidgetId, fewerImages)
        }
    }

    /**
     * One screen of ARGB pixels, under the 1.5 screens the system allows a widget.
     */
    private fun maxBitmapByteCount(): Long {
        val displayMetrics = context.resources.displayMetrics
        return BYTES_PER_PIXEL * displayMetrics.widthPixels * displayMetrics.heightPixels
    }

    private fun finishAfter(rebuilds: Job, pendingResult: PendingResult) {
        applicationScope.launch(ioDispatcher) {
            try {
                // Release the broadcast before it ANRs; the rebuilds go on either way
                withTimeoutOrNull(BROADCAST_BUDGET) { rebuilds.join() }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = LogUtil.BUGLE_WIDGET_TAG
        private const val BYTES_PER_PIXEL = 4L
        private const val FALLBACK_BITMAP_BUDGET_DIVISOR = 4
        private val BROADCAST_BUDGET = 8.seconds
    }
}
