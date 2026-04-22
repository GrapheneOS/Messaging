package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.data.conversation.model.metadata.ConversationSubscription
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.debug.DebugSimEmulationMode
import com.android.messaging.debug.DebugSimEmulationSource
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.sms.MmsConfig
import com.android.messaging.util.LogUtil
import com.android.messaging.util.core.extension.typedFlow
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface ConversationSubscriptionsRepository {
    fun observeActiveSubscriptions(): Flow<ImmutableList<ConversationSubscription>>

    fun resolveMaxMessageSize(selfParticipantId: String): Flow<Int>
}

internal class ConversationSubscriptionsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val debugSimEmulationSource: DebugSimEmulationSource,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationSubscriptionsRepository {

    override fun observeActiveSubscriptions(): Flow<ImmutableList<ConversationSubscription>> {
        val uri = MessagingContentProvider.PARTICIPANTS_URI

        val realSubscriptions = observeUri(uri = uri)
            .conflate()
            .map {
                queryActiveSubscriptions()
            }
            .flowOn(ioDispatcher)

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

    override fun resolveMaxMessageSize(selfParticipantId: String): Flow<Int> {
        return typedFlow {
            queryMaxMessageSize(selfParticipantId = selfParticipantId)
        }.catch { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }

            LogUtil.w(TAG, "Failed to resolve max message size", throwable)
            emit(MmsConfig.getMaxMaxMessageSize())
        }.flowOn(ioDispatcher)
    }

    private fun applyDebugEmulation(
        subscriptions: ImmutableList<ConversationSubscription>,
        mode: DebugSimEmulationMode,
    ): ImmutableList<ConversationSubscription> {
        return when (mode) {
            DebugSimEmulationMode.DEFAULT -> subscriptions
            DebugSimEmulationMode.SINGLE -> applySingleSimEmulation(subscriptions = subscriptions)
            DebugSimEmulationMode.DUAL -> applyDualSimEmulation(subscriptions = subscriptions)
        }
    }

    private fun applySingleSimEmulation(
        subscriptions: ImmutableList<ConversationSubscription>,
    ): ImmutableList<ConversationSubscription> {
        val hasRealSubscription = subscriptions.isNotEmpty()

        if (hasRealSubscription) {
            return subscriptions
        }

        return persistentListOf(
            fakeSubscription(slotId = 1, colorIndex = 0),
        )
    }

    private fun applyDualSimEmulation(
        subscriptions: ImmutableList<ConversationSubscription>,
    ): ImmutableList<ConversationSubscription> {
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
        realSubscription: ConversationSubscription,
    ): ImmutableList<ConversationSubscription> {
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
    ): ConversationSubscription {
        return ConversationSubscription(
            selfParticipantId = "$FAKE_SIM_ID_PREFIX$slotId",
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

    private fun queryActiveSubscriptions(): ImmutableList<ConversationSubscription> {
        return contentResolver
            .query(
                MessagingContentProvider.PARTICIPANTS_URI,
                ParticipantData.ParticipantsQuery.PROJECTION,
                "${ParticipantColumns.SUB_ID} <> ?",
                arrayOf(ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
                null,
            )
            ?.use { cursor ->
                val subscriptions = persistentListOf<ConversationSubscription>().builder()

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

    private fun queryMaxMessageSize(
        selfParticipantId: String,
    ): Int {
        if (selfParticipantId.isBlank()) {
            return MmsConfig.getMaxMaxMessageSize()
        }

        val resolvedSubId = contentResolver.query(
            MessagingContentProvider.PARTICIPANTS_URI,
            ParticipantData.ParticipantsQuery.PROJECTION,
            "${ParticipantColumns._ID} = ?",
            arrayOf(selfParticipantId),
            null,
        )?.use { cursor ->
            when {
                cursor.moveToFirst() -> ParticipantData.getFromCursor(cursor).subId
                else -> null
            }
        } ?: return MmsConfig.getMaxMaxMessageSize()

        if (resolvedSubId <= ParticipantData.DEFAULT_SELF_SUB_ID) {
            return MmsConfig.getMaxMaxMessageSize()
        }

        return MmsConfig.get(resolvedSubId).maxMessageSize
    }

    private companion object {
        private const val TAG = "ConversationSubscriptionsRepo"
        private const val FAKE_SIM_ID_PREFIX = "debug_sim_emulated_"
        private val FAKE_SIM_COLORS = intArrayOf(
            0xFF5E9BE8.toInt(),
            0xFFE97E6A.toInt(),
        )
    }

    private fun ParticipantData.toConversationSubscription(): ConversationSubscription {
        val slotId = displaySlotId

        return ConversationSubscription(
            selfParticipantId = id,
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
