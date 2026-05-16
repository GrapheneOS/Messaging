package com.android.messaging.data.subscriptionsettings.repository

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import com.android.messaging.Factory
import com.android.messaging.R
import com.android.messaging.data.subscriptionsettings.model.PerSubscriptionData
import com.android.messaging.data.subscriptionsettings.model.SubscriptionBooleanPref
import com.android.messaging.data.subscriptionsettings.model.SubscriptionSettingsData
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.sms.MmsConfig
import com.android.messaging.ui.UIIntents
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

internal interface SubscriptionSettingsRepository {
    fun isMultiSim(): Boolean
    fun observeSubscriptionsChanged(): Flow<Unit>
    suspend fun getSubscriptionSettings(): SubscriptionSettingsData
    suspend fun setSubscriptionBooleanPref(
        subId: Int,
        pref: SubscriptionBooleanPref,
        enabled: Boolean,
    )
}

internal class SubscriptionSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val subscriptionManager: SubscriptionManager,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SubscriptionSettingsRepository {

    override fun isMultiSim(): Boolean {
        return PhoneUtils.getDefault().activeSubscriptionCount > 1
    }

    override fun observeSubscriptionsChanged(): Flow<Unit> {
        return callbackFlow {
            val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    trySend(Unit)
                }
            }
            subscriptionManager.addOnSubscriptionsChangedListener(context.mainExecutor, listener)
            awaitClose {
                subscriptionManager.removeOnSubscriptionsChangedListener(listener)
            }
        }
    }

    override suspend fun getSubscriptionSettings(): SubscriptionSettingsData {
        return withContext(ioDispatcher) {
            val phoneUtilsDefault = PhoneUtils.getDefault()
            val activeCount = phoneUtilsDefault.activeSubscriptionCount
            val isDefaultSmsApp = phoneUtilsDefault.isDefaultSmsApp
            val isCellBroadcastAppEnabled = readCellBroadcastAppEnabled()
            val defaultSelfData = readPerSubscriptionData(
                subId = ParticipantData.DEFAULT_SELF_SUB_ID,
                subscriptionName = null,
            )

            val nonDefaultActives = when {
                activeCount > 1 -> {
                    querySelfParticipants()
                        .filter { !it.isDefaultSelf && it.isActiveSubscription }
                        .map { self ->
                            readPerSubscriptionData(
                                subId = self.subId,
                                subscriptionName = self.subscriptionName,
                            )
                        }
                        .toImmutableList()
                }

                else -> {
                    persistentListOf()
                }
            }

            SubscriptionSettingsData(
                isDefaultSmsApp = isDefaultSmsApp,
                activeSubscriptionCount = activeCount,
                isCellBroadcastAppEnabled = isCellBroadcastAppEnabled,
                defaultSelfSubscription = defaultSelfData,
                nonDefaultActiveSelfSubscriptions = nonDefaultActives,
            )
        }
    }

    override suspend fun setSubscriptionBooleanPref(
        subId: Int,
        pref: SubscriptionBooleanPref,
        enabled: Boolean,
    ) {
        withContext(ioDispatcher) {
            BuglePrefs.getSubscriptionPrefs(subId).putBoolean(
                context.getString(pref.keyResId),
                enabled,
            )
        }
    }

    private fun readPerSubscriptionData(
        subId: Int,
        subscriptionName: String?,
    ): PerSubscriptionData {
        val subPrefs = Factory.get().getSubscriptionPrefs(subId)
        val phoneUtils = PhoneUtils.get(subId)
        val mmsConfig = MmsConfig.get(subId)
        val resources = context.resources

        val savedPhoneNumber = subPrefs.getString(
            context.getString(R.string.mms_phone_number_pref_key),
            "",
        ).orEmpty()
        val defaultPhoneNumber = phoneUtils.getCanonicalForSelf(false).orEmpty()

        return PerSubscriptionData(
            subId = subId,
            subscriptionName = subscriptionName,
            savedPhoneNumber = savedPhoneNumber,
            defaultPhoneNumber = defaultPhoneNumber,
            formattedSavedPhoneNumber = savedPhoneNumber
                .takeIf(String::isNotEmpty)
                ?.let(phoneUtils::formatForDisplay),
            formattedDefaultPhoneNumber = defaultPhoneNumber
                .takeIf(String::isNotEmpty)
                ?.let(phoneUtils::formatForDisplay),
            isGroupMmsSupported = mmsConfig.groupMmsEnabled,
            isGroupMmsEnabled = subPrefs.getBoolean(
                context.getString(R.string.group_mms_pref_key),
                resources.getBoolean(R.bool.group_mms_pref_default),
            ),
            autoRetrieveMms = subPrefs.getBoolean(
                context.getString(R.string.auto_retrieve_mms_pref_key),
                resources.getBoolean(R.bool.auto_retrieve_mms_pref_default),
            ),
            autoRetrieveMmsWhenRoaming = subPrefs.getBoolean(
                context.getString(R.string.auto_retrieve_mms_when_roaming_pref_key),
                resources.getBoolean(R.bool.auto_retrieve_mms_when_roaming_pref_default),
            ),
            isDeliveryReportsSupported = mmsConfig.smsDeliveryReportsEnabled,
            deliveryReportsEnabled = subPrefs.getBoolean(
                context.getString(R.string.delivery_reports_pref_key),
                resources.getBoolean(R.bool.delivery_reports_pref_default),
            ),
            showCellBroadcast = mmsConfig.showCellBroadcast,
        )
    }

    private fun querySelfParticipants(): List<ParticipantData> {
        val cursor = contentResolver.query(
            MessagingContentProvider.PARTICIPANTS_URI,
            ParticipantData.ParticipantsQuery.PROJECTION,
            ParticipantColumns.SUB_ID + " <> ?",
            arrayOf(ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
            null,
        ) ?: return emptyList()

        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(ParticipantData.getFromCursor(it))
                }
            }
        }
    }

    private fun readCellBroadcastAppEnabled(): Boolean {
        return try {
            val state = context.packageManager.getApplicationEnabledSetting(
                UIIntents.CMAS_COMPONENT,
            )
            state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
