package com.android.messaging.screenshots

import android.content.Context
import com.android.messaging.Factory
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.MemoryCacheManager
import com.android.messaging.datamodel.ParticipantRefresh.ContactContentObserver
import com.android.messaging.datamodel.media.MediaCacheManager
import com.android.messaging.datamodel.media.MediaResourceManager
import com.android.messaging.sms.BugleCarrierConfigValuesLoader
import com.android.messaging.ui.UIIntents
import com.android.messaging.util.BugleGservices
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.MediaUtil
import com.android.messaging.util.PhoneUtils

// The conversation list formats its timestamps through `Dates`, which reads the context from the
// app-wide Factory. Nothing else the screenshots draw needs it.
internal class ReadmeFactory(
    private val context: Context,
) : Factory() {
    fun install() = setInstance(this)

    override fun getApplicationContext(): Context = context

    override fun onRequiredPermissionsAcquired() = unused()
    override fun getDataModel(): DataModel = unused()
    override fun getBugleGservices(): BugleGservices = unused()
    override fun getApplicationPrefs(): BuglePrefs = unused()
    override fun getSubscriptionPrefs(subId: Int): BuglePrefs = unused()
    override fun getWidgetPrefs(): BuglePrefs = unused()
    override fun getUIIntents(): UIIntents = unused()
    override fun getMemoryCacheManager(): MemoryCacheManager = unused()
    override fun getMediaResourceManager(): MediaResourceManager = unused()
    override fun getMediaCacheManager(): MediaCacheManager = unused()
    override fun getContactContentObserver(): ContactContentObserver = unused()
    override fun getPhoneUtils(subId: Int): PhoneUtils = unused()
    override fun getMediaUtil(): MediaUtil = unused()
    override fun getCarrierConfigValuesLoader(): BugleCarrierConfigValuesLoader = unused()
    override fun reclaimMemory() = unused()
    override fun onActivityResume() = unused()

    private fun unused(): Nothing = error("Not available in screenshot tests")
}
