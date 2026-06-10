package com.android.messaging.data.subscription.repository

import android.Manifest.permission.MODIFY_PHONE_STATE
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.telephony.SubscriptionManager
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.debug.DebugSimEmulationMode
import com.android.messaging.debug.DebugSimEmulationSource
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.sms.MmsConfig
import com.android.messaging.util.BugleGservices
import com.android.messaging.util.BugleGservicesKeys
import com.android.messaging.util.LogUtil
import com.android.messaging.util.PhoneUtils
import com.android.messaging.util.core.extension.typedFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface SubscriptionsRepository {
    fun observeActiveSubscriptions(): Flow<ImmutableList<Subscription>>

    fun observeDefaultSmsSubscriptionId(): Flow<Int>

    fun getDefaultSmsSubscriptionId(): Int

    fun resolveAttachmentLimit(): Int

    fun resolveMaxMessageSize(selfParticipantId: String): Flow<Int>
}

internal class SubscriptionsRepositoryImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val subscriptionManager: SubscriptionManager,
    private val debugSimEmulationSource: DebugSimEmulationSource,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : SubscriptionsRepository {

    override fun observeActiveSubscriptions(): Flow<ImmutableList<Subscription>> {
        val uri = MessagingContentProvider.PARTICIPANTS_URI

        val realSubscriptions = observeUri(uri = uri)
            .flowOn(defaultDispatcher)
            .conflate()
            .map {
                queryActiveSubscriptions()
            }
            .flowOn(messagingDbDispatcher)

        return combine(
            realSubscriptions,
            debugSimEmulationSource.mode,
        ) { subscriptions, emulationMode ->
            applyDebugEmulation(
                subscriptions = subscriptions,
                mode = emulationMode,
            )
        }
    }

    override fun observeDefaultSmsSubscriptionId(): Flow<Int> {
        return callbackFlow {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (
                        intent.action ==
                        SubscriptionManager.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED
                    ) {
                        trySend(Unit)
                    }
                }
            }
            val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    trySend(Unit)
                }
            }

            context.registerReceiver(
                receiver,
                IntentFilter(SubscriptionManager.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED),
                MODIFY_PHONE_STATE,
                null,
                Context.RECEIVER_EXPORTED,
            )
            subscriptionManager.addOnSubscriptionsChangedListener(context.mainExecutor, listener)
            trySend(Unit)

            awaitClose {
                context.unregisterReceiver(receiver)
                subscriptionManager.removeOnSubscriptionsChangedListener(listener)
            }
        }
            .map { getDefaultSmsSubscriptionId() }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)
    }

    override fun getDefaultSmsSubscriptionId(): Int {
        return PhoneUtils.getDefault().defaultSmsSubscriptionId
    }

    override fun resolveAttachmentLimit(): Int {
        return BugleGservices
            .get()
            .getInt(
                BugleGservicesKeys.MMS_ATTACHMENT_LIMIT,
                BugleGservicesKeys.MMS_ATTACHMENT_LIMIT_DEFAULT,
            )
    }

    override fun resolveMaxMessageSize(selfParticipantId: String): Flow<Int> {
        return typedFlow {
            resolveSubscriptionId(selfParticipantId = selfParticipantId)
        }
            .flowOn(messagingDbDispatcher)
            .map { resolvedSubId ->
                resolveMaxMessageSizeForSubId(resolvedSubId = resolvedSubId)
            }
            .catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }

                LogUtil.w(TAG, "Failed to resolve max message size", throwable)
                emit(MmsConfig.getMaxMaxMessageSize())
            }
    }

    private fun applyDebugEmulation(
        subscriptions: ImmutableList<Subscription>,
        mode: DebugSimEmulationMode,
    ): ImmutableList<Subscription> {
        return when (mode) {
            DebugSimEmulationMode.DEFAULT -> subscriptions
            DebugSimEmulationMode.SINGLE -> applySingleSimEmulation(subscriptions = subscriptions)
            DebugSimEmulationMode.DUAL -> applyDualSimEmulation(subscriptions = subscriptions)
        }
    }

    private fun applySingleSimEmulation(
        subscriptions: ImmutableList<Subscription>,
    ): ImmutableList<Subscription> {
        val hasRealSubscription = subscriptions.isNotEmpty()

        if (hasRealSubscription) {
            return subscriptions
        }

        return persistentListOf(
            fakeSubscription(slotId = 1, colorIndex = 0),
        )
    }

    private fun applyDualSimEmulation(
        subscriptions: ImmutableList<Subscription>,
    ): ImmutableList<Subscription> {
        return when (subscriptions.size) {
            0 -> {
                persistentListOf(
                    fakeSubscription(slotId = 1, colorIndex = 0),
                    fakeSubscription(slotId = 2, colorIndex = 1),
                )
            }

            1 -> pairRealSubscriptionWithFake(realSubscription = subscriptions.first())

            else -> subscriptions
        }
    }

    private fun pairRealSubscriptionWithFake(
        realSubscription: Subscription,
    ): ImmutableList<Subscription> {
        val fakeSlot = when (realSubscription.displaySlotId) {
            1 -> 2
            else -> 1
        }
        return sequenceOf(
            realSubscription,
            fakeSubscription(slotId = fakeSlot, colorIndex = 1),
        )
            .sortedBy { subscription -> subscription.displaySlotId }
            .toImmutableList()
    }

    private fun fakeSubscription(
        slotId: Int,
        colorIndex: Int,
    ): Subscription {
        return Subscription(
            selfParticipantId = "$FAKE_SIM_ID_PREFIX$slotId",
            subId = ParticipantData.DEFAULT_SELF_SUB_ID,
            label = ConversationSubscriptionLabel.DebugFake(slotId = slotId),
            displayDestination = null,
            displaySlotId = slotId,
            color = FAKE_SIM_COLORS[colorIndex % FAKE_SIM_COLORS.size],
        )
    }

    private fun observeUri(uri: Uri): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            contentResolver.registerContentObserver(uri, true, observer)

            trySend(Unit)

            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    private fun queryActiveSubscriptions(): ImmutableList<Subscription> {
        return contentResolver
            .query(
                MessagingContentProvider.PARTICIPANTS_URI,
                ParticipantData.ParticipantsQuery.PROJECTION,
                "${ParticipantColumns.SUB_ID} <> ?",
                arrayOf(ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
                null,
            )
            ?.use { cursor ->
                val subscriptions = persistentListOf<Subscription>().builder()

                while (cursor.moveToNext()) {
                    val participant = ParticipantData.getFromCursor(cursor)

                    val shouldSkip = !participant.isSelf ||
                        participant.isDefaultSelf ||
                        !participant.isActiveSubscription

                    if (shouldSkip) {
                        continue
                    }

                    subscriptions.add(participant.toConversationSubscription())
                }

                subscriptions
                    .build()
                    .sortedBy { subscription -> subscription.displaySlotId }
                    .toImmutableList()
            }
            ?: persistentListOf()
    }

    private fun resolveMaxMessageSizeForSubId(
        resolvedSubId: Int?,
    ): Int {
        return when {
            resolvedSubId == null || resolvedSubId <= ParticipantData.DEFAULT_SELF_SUB_ID -> {
                MmsConfig.getMaxMaxMessageSize()
            }

            else -> {
                MmsConfig.get(resolvedSubId).maxMessageSize
            }
        }
    }

    private fun resolveSubscriptionId(selfParticipantId: String): Int? {
        if (selfParticipantId.isBlank()) {
            return null
        }

        return contentResolver.query(
            MessagingContentProvider.PARTICIPANTS_URI,
            ParticipantData.ParticipantsQuery.PROJECTION,
            "${ParticipantColumns._ID} = ?",
            arrayOf(selfParticipantId),
            null,
        )?.use { cursor ->
            when {
                cursor.moveToFirst() -> {
                    ParticipantData.getFromCursor(cursor).subId
                }

                else -> null
            }
        }
    }

    private companion object {
        private const val TAG = "ConversationSubscriptionsRepo"
        private const val FAKE_SIM_ID_PREFIX = "debug_sim_emulated_"
        private val FAKE_SIM_COLORS = intArrayOf(
            0xFF5E9BE8.toInt(),
            0xFFE97E6A.toInt(),
        )
    }

    private fun ParticipantData.toConversationSubscription(): Subscription {
        val slotId = displaySlotId

        return Subscription(
            selfParticipantId = id,
            subId = subId,
            label = when {
                subscriptionName.isNullOrBlank() -> ConversationSubscriptionLabel.Slot(
                    slotId = slotId,
                )

                else -> ConversationSubscriptionLabel.Named(name = subscriptionName)
            },
            displayDestination = displayDestination?.takeIf { it.isNotBlank() },
            displaySlotId = slotId,
            color = subscriptionColor,
        )
    }
}
