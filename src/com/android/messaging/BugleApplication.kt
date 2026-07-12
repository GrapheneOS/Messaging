/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.telephony.CarrierConfigManager
import android.util.Log
import androidx.appcompat.mms.CarrierConfigValuesLoader
import androidx.appcompat.mms.MmsManager
import com.android.messaging.di.receiver.IncomingSmsEntryPoint
import com.android.messaging.domain.notification.usecase.MigrateConversationNotificationChannels
import com.android.messaging.sms.BugleUserAgentInfoLoader
import com.android.messaging.sms.MmsConfig
import com.android.messaging.ui.ConversationDrawables
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.DebugUtils
import com.android.messaging.util.LogUtil
import com.android.messaging.util.NotificationChannelUtil
import com.android.messaging.util.PhoneUtils
import com.android.messaging.util.Trace
import com.google.common.annotations.VisibleForTesting
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
open class BugleApplication :
    Application(),
    Thread.UncaughtExceptionHandler {

    @Inject
    internal lateinit var migrateConversationNotificationChannels:
        MigrateConversationNotificationChannels

    private var systemUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    @VisibleForTesting
    protected fun setTestsRunning() {
        runningTests = true
    }

    override fun onCreate() {
        traceSection(sectionName = "app.onCreate") {
            super.onCreate()

            // Note onCreate is called in both test and real application environments.
            if (!runningTests) {
                FactoryImpl.register(applicationContext, this)
            } else {
                LogUtil.e(
                    TAG,
                    "BugleApplication.onCreate: FactoryImpl.register skipped for test run",
                )
            }

            systemUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(this)

            NotificationChannelUtil.onCreate(context = this)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Update conversation drawables when changing writing systems.
        ConversationDrawables.get().updateDrawables()
    }

    // Called by the real factory from FactoryImpl.register(), not in tests.
    fun initializeSync(factory: Factory) {
        traceSection(sectionName = "app.initializeSync") {
            val context = factory.applicationContext
            val dataModel = factory.dataModel
            val carrierConfigValuesLoader = factory.carrierConfigValuesLoader

            maybeStartProfiling()

            updateAppConfig(context = context)

            initMmsLib(
                context = context,
                carrierConfigValuesLoader = carrierConfigValuesLoader,
            )
            migrateConversationNotificationChannels()
            // Fixup messages in flight if we crashed and send any pending.
            dataModel.onApplicationCreated()
            registerCarrierConfigChangeReceiver(context = context)
        }
    }

    // Called from thread started in FactoryImpl.register(), not in tests.
    fun initializeAsync(factory: Factory) {
        traceSection(sectionName = "app.initializeAsync") {
            maybeHandleSharedPrefsUpgrade(factory = factory)
            MmsConfig.load()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()

        if (LogUtil.isLoggable(TAG, LogUtil.DEBUG)) {
            LogUtil.d(TAG, "BugleApplication.onLowMemory")
        }
        Factory.get().reclaimMemory()
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val background = mainLooper.thread != thread
        if (background) {
            LogUtil.e(TAG, "Uncaught exception in background thread $thread", exception)

            Handler(mainLooper).post {
                forwardUncaughtException(thread = thread, exception = exception)
            }
        } else {
            forwardUncaughtException(thread = thread, exception = exception)
        }
    }

    private fun registerCarrierConfigChangeReceiver(context: Context) {
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    LogUtil.i(TAG, "Carrier config changed. Reloading MMS config.")
                    MmsConfig.loadAsync()
                }
            },
            IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED),
            RECEIVER_EXPORTED,
        )
    }

    private fun initMmsLib(
        context: Context,
        carrierConfigValuesLoader: CarrierConfigValuesLoader,
    ) {
        MmsManager.setCarrierConfigValuesLoader(carrierConfigValuesLoader)
        MmsManager.setUserAgentInfoLoader(BugleUserAgentInfoLoader(context))
    }

    private fun forwardUncaughtException(thread: Thread, exception: Throwable) {
        val handler = systemUncaughtExceptionHandler
            ?: throw IllegalStateException(
                "System uncaught exception handler is not initialized",
                exception,
            )
        handler.uncaughtException(thread, exception)
    }

    private fun maybeStartProfiling() {
        // App startup profiling support. To use it:
        //  adb shell setprop log.tag.BugleProfile DEBUG
        //  # Start the app, wait for a 30s, download trace file:
        //  adb pull /data/data/com.android.messaging/cache/startup.trace /tmp
        //  # Open trace file (using adt/tools/traceview)
        if (!Log.isLoggable(LogUtil.PROFILE_TAG, Log.DEBUG)) {
            return
        }

        DebugUtils
            .getDebugFile("startup.trace", true)
            ?.let { file ->
                Debug.startMethodTracing(
                    file.absolutePath,
                    STARTUP_TRACE_BUFFER_SIZE_BYTES,
                )
                scheduleProfilingStop(file = file)
            }
    }

    private fun scheduleProfilingStop(file: File) {
        Handler(Looper.getMainLooper()).postDelayed(
            {
                Debug.stopMethodTracing()
                DebugUtils.ensureReadable(file)
                LogUtil.d(LogUtil.PROFILE_TAG, "Tracing complete - ${file.absolutePath}")
            },
            STARTUP_TRACE_DURATION_MILLIS,
        )
    }

    private fun maybeHandleSharedPrefsUpgrade(factory: Factory) {
        val applicationPrefs = factory.applicationPrefs
        val existingVersion = applicationPrefs.getInt(
            BuglePrefsKeys.SHARED_PREFERENCES_VERSION,
            BuglePrefsKeys.SHARED_PREFERENCES_VERSION_DEFAULT,
        )
        val targetVersion = getString(R.string.pref_version).toInt()

        when {
            targetVersion > existingVersion -> upgradeSharedPrefs(
                factory = factory,
                existingVersion = existingVersion,
                targetVersion = targetVersion,
            )

            targetVersion < existingVersion -> LogUtil.e(
                TAG,
                "Shared prefs downgrade requested and ignored. " +
                    "oldVersion = $existingVersion, newVersion = $targetVersion",
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun upgradeSharedPrefs(factory: Factory, existingVersion: Int, targetVersion: Int) {
        LogUtil.i(TAG, "Upgrading shared prefs from $existingVersion to $targetVersion")

        try {
            factory.applicationPrefs.onUpgrade(existingVersion, targetVersion)
            PhoneUtils.forEachActiveSubscription { subscriptionId ->
                factory.getSubscriptionPrefs(subscriptionId)
                    .onUpgrade(existingVersion, targetVersion)
            }
            factory.applicationPrefs.putInt(
                BuglePrefsKeys.SHARED_PREFERENCES_VERSION,
                targetVersion,
            )
        } catch (exception: Exception) {
            // Upgrade failed. Don't crash the app because we can always fall back to defaults.
            LogUtil.e(TAG, "Failed to upgrade shared prefs", exception)
        }
    }

    private inline fun traceSection(sectionName: String, block: () -> Unit) {
        Trace.beginSection(sectionName)
        try {
            block()
        } finally {
            Trace.endSection()
        }
    }

    companion object {
        private const val TAG = "BugleApplication"

        private const val STARTUP_TRACE_BUFFER_SIZE_BYTES = 160 * 1024 * 1024
        private const val STARTUP_TRACE_DURATION_MILLIS = 30_000L

        private var runningTests = false

        @JvmStatic
        fun isRunningTests(): Boolean {
            return runningTests
        }

        fun updateAppConfig(context: Context) {
            // Make sure we set the correct state for the SMS/MMS receivers.
            EntryPointAccessors.fromApplication(context, IncomingSmsEntryPoint::class.java)
                .smsReceiverToggle()
                .update(context)
        }
    }
}
