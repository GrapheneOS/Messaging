package com.android.messaging.data.debugmmsconfig.repository

import com.android.messaging.data.debugmmsconfig.model.DebugSim
import com.android.messaging.data.debugmmsconfig.model.MmsConfigEntry
import com.android.messaging.data.debugmmsconfig.model.MmsConfigKeyType
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.sms.MmsConfig
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface MmsConfigRepository {
    suspend fun getActiveSims(): ImmutableList<DebugSim>
    suspend fun getEntries(subId: SubId): ImmutableList<MmsConfigEntry>
    suspend fun updateEntry(
        subId: SubId,
        key: String,
        keyType: MmsConfigKeyType,
        value: String,
    )
}

internal class MmsConfigRepositoryImpl @Inject constructor(
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : MmsConfigRepository {

    override suspend fun getActiveSims(): ImmutableList<DebugSim> {
        return withContext(messagingDbDispatcher) {
            readActiveSims()
        }
    }

    override suspend fun getEntries(subId: SubId): ImmutableList<MmsConfigEntry> {
        return withContext(messagingDbDispatcher) {
            readEntries(subId)
        }
    }

    override suspend fun updateEntry(
        subId: SubId,
        key: String,
        keyType: MmsConfigKeyType,
        value: String,
    ) {
        withContext(messagingDbDispatcher) {
            MmsConfig.get(subId.value)
                .update(keyType.rawType, key, value)
        }
    }

    private fun readActiveSims(): ImmutableList<DebugSim> {
        val subRecords = PhoneUtils.getDefault().activeSubscriptionInfoList
            ?: return persistentListOf()

        return subRecords
            .map { subInfo ->
                val subId = subInfo.subscriptionId
                val mccMnc = PhoneUtils.get(subId).mccMnc

                DebugSim(
                    subId = SubId(subId),
                    mcc = mccMnc[0],
                    mnc = mccMnc[1],
                )
            }
            .toImmutableList()
    }

    private fun readEntries(subId: SubId): ImmutableList<MmsConfigEntry> {
        val mmsConfig = MmsConfig.get(subId.value)

        return mmsConfig.keySet()
            .mapNotNull { key ->
                val keyType = MmsConfigKeyType.fromRawType(
                    MmsConfig.getKeyType(key)
                ) ?: return@mapNotNull null

                MmsConfigEntry(
                    key = key,
                    keyType = keyType,
                    value = mmsConfig.getValue(key)?.toString() ?: NULL_VALUE,
                )
            }
            .sortedBy(MmsConfigEntry::key)
            .toImmutableList()
    }

    private companion object {
        private const val NULL_VALUE = "null"
    }
}
