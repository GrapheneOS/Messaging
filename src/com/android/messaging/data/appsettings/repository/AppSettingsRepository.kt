package com.android.messaging.data.appsettings.repository

import android.content.Context
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.AppBooleanPref
import com.android.messaging.data.appsettings.model.AppSettings
import com.android.messaging.data.appsettings.model.LinkOpeningMode
import com.android.messaging.data.debug.DebugFeaturesProvider
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface AppSettingsRepository {
    suspend fun getAppSettings(): AppSettings
    suspend fun isYouTubeLinkPreviewsEnabled(): Boolean
    suspend fun getLinkOpeningMode(): LinkOpeningMode
    suspend fun setBooleanPref(pref: AppBooleanPref, enabled: Boolean)
    suspend fun setLinkOpeningMode(mode: LinkOpeningMode)
}

internal class AppSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val debugFeaturesProvider: DebugFeaturesProvider,
) : AppSettingsRepository {

    override suspend fun getAppSettings(): AppSettings {
        return withContext(ioDispatcher) {
            val appPrefs = BuglePrefs.getApplicationPrefs()
            val phoneUtils = PhoneUtils.getDefault()
            val resources = context.resources

            AppSettings(
                isDefaultSmsApp = phoneUtils.isDefaultSmsApp,
                defaultSmsAppLabel = phoneUtils.defaultSmsAppLabel,
                sendSoundEnabled = appPrefs.getBoolean(
                    context.getString(R.string.send_sound_pref_key),
                    resources.getBoolean(R.bool.send_sound_pref_default),
                ),
                inConversationSoundEnabled = appPrefs.getBoolean(
                    context.getString(R.string.in_conversation_sound_pref_key),
                    resources.getBoolean(R.bool.in_conversation_sound_pref_default),
                ),
                youTubeLinkPreviewsEnabled = readYouTubeLinkPreviewsEnabled(),
                linkOpeningMode = readLinkOpeningMode(),
                isDebugEnabled = debugFeaturesProvider.isEnabled(),
                dumpSmsEnabled = appPrefs.getBoolean(
                    context.getString(R.string.dump_sms_pref_key),
                    resources.getBoolean(R.bool.dump_sms_pref_default),
                ),
                dumpMmsEnabled = appPrefs.getBoolean(
                    context.getString(R.string.dump_mms_pref_key),
                    resources.getBoolean(R.bool.dump_mms_pref_default),
                ),
            )
        }
    }

    override suspend fun isYouTubeLinkPreviewsEnabled(): Boolean {
        return withContext(ioDispatcher) {
            readYouTubeLinkPreviewsEnabled()
        }
    }

    override suspend fun getLinkOpeningMode(): LinkOpeningMode {
        return withContext(ioDispatcher) {
            readLinkOpeningMode()
        }
    }

    override suspend fun setBooleanPref(
        pref: AppBooleanPref,
        enabled: Boolean,
    ) {
        withContext(ioDispatcher) {
            BuglePrefs.getApplicationPrefs().putBoolean(
                context.getString(pref.keyResId),
                enabled,
            )
        }
    }

    override suspend fun setLinkOpeningMode(mode: LinkOpeningMode) {
        withContext(ioDispatcher) {
            BuglePrefs.getApplicationPrefs().putString(
                context.getString(R.string.link_opening_pref_key),
                mode.prefValue,
            )
        }
    }

    private fun readYouTubeLinkPreviewsEnabled(): Boolean {
        return BuglePrefs.getApplicationPrefs().getBoolean(
            context.getString(R.string.youtube_link_previews_pref_key),
            context.resources.getBoolean(R.bool.youtube_link_previews_pref_default),
        )
    }

    private fun readLinkOpeningMode(): LinkOpeningMode {
        val defaultValue = context.getString(R.string.link_opening_pref_default)
        val value = BuglePrefs.getApplicationPrefs().getString(
            context.getString(R.string.link_opening_pref_key),
            defaultValue,
        )
        return linkOpeningModeOrNull(prefValue = value)
            ?: linkOpeningModeOrNull(prefValue = defaultValue)
            ?: LinkOpeningMode.AskBeforeOpening
    }

    private fun linkOpeningModeOrNull(prefValue: String?): LinkOpeningMode? {
        return LinkOpeningMode.entries.firstOrNull { mode -> mode.prefValue == prefValue }
    }
}
