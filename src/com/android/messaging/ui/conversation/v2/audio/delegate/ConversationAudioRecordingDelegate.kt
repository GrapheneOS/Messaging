package com.android.messaging.ui.conversation.v2.audio.delegate

import android.net.Uri
import android.os.SystemClock
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachmentKind
import com.android.messaging.data.conversation.repository.ConversationSubscriptionsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.repository.ConversationAttachmentRepository
import com.android.messaging.ui.mediapicker.LevelTrackingMediaRecorder
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal interface ConversationAudioRecordingDelegate :
    ConversationScreenDelegate<ConversationAudioRecordingUiState> {

    fun startRecording(selfParticipantId: String)

    fun startLockedRecording(selfParticipantId: String)

    fun lockRecording(): Boolean

    fun finishRecording()

    fun cancelRecording()

    fun onScreenCleared()
}

internal class ConversationAudioRecordingDelegateImpl @Inject constructor(
    private val conversationAttachmentRepository: ConversationAttachmentRepository,
    private val conversationSubscriptionsRepository: ConversationSubscriptionsRepository,
    private val conversationDraftDelegate: ConversationDraftDelegate,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationAudioRecordingDelegate {

    private val _state = MutableStateFlow(ConversationAudioRecordingUiState())
    override val state = _state.asStateFlow()

    private val sessionStateLock = Any()

    private var boundScope: CoroutineScope? = null
    private var sessionState: AudioRecordingSessionState = AudioRecordingSessionState.Idle

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope
        scope.launch(defaultDispatcher) {
            conversationIdFlow.drop(count = 1).collect {
                cancelRecording()
            }
        }
    }

    override fun startRecording(selfParticipantId: String) {
        startRecording(
            selfParticipantId = selfParticipantId,
            queuedStartIntent = QueuedStartIntent.None,
        )
    }

    override fun startLockedRecording(selfParticipantId: String) {
        startRecording(
            selfParticipantId = selfParticipantId,
            queuedStartIntent = QueuedStartIntent.Lock,
        )
    }

    private fun startRecording(
        selfParticipantId: String,
        queuedStartIntent: QueuedStartIntent,
    ) {
        val scope = boundScope ?: return
        val startJob = scope.launch(
            context = defaultDispatcher,
            start = CoroutineStart.LAZY,
        ) {
            startRecordingInBackground(selfParticipantId = selfParticipantId)
        }

        val shouldStartJob = withSessionStateLock {
            tryStartRecordingLocked(queuedStartIntent = queuedStartIntent)
        }

        when {
            shouldStartJob -> startJob.start()
            else -> startJob.cancel()
        }
    }

    override fun lockRecording(): Boolean {
        return withSessionStateLock {
            tryLockRecordingLocked()
        }
    }

    override fun finishRecording() {
        val scope = boundScope ?: return
        val pendingAttachmentId = createPendingAudioAttachmentId()

        val finishJob = scope.launch(
            context = defaultDispatcher,
            start = CoroutineStart.LAZY,
        ) {
            finalizeRecording(pendingAttachmentId = pendingAttachmentId)
        }

        val effect = withSessionStateLock {
            finishRecordingLocked(
                pendingAttachmentId = pendingAttachmentId,
                finishJob = finishJob,
            )
        }

        if (effect !is AudioRecordingEffect.StartFinalization) {
            finishJob.cancel()
        }

        runAudioRecordingEffect(
            scope = scope,
            effect = effect,
        )
    }

    override fun cancelRecording() {
        val scope = boundScope ?: return
        val effect = withSessionStateLock {
            cancelRecordingLocked()
        }

        runAudioRecordingEffect(
            scope = scope,
            effect = effect,
        )
    }

    override fun onScreenCleared() {
        cancelRecording()
    }

    private fun <T> withSessionStateLock(block: () -> T): T {
        return synchronized(sessionStateLock) {
            block()
        }
    }

    private fun tryStartRecordingLocked(queuedStartIntent: QueuedStartIntent): Boolean {
        if (sessionState !is AudioRecordingSessionState.Idle) {
            return false
        }

        sessionState = AudioRecordingSessionState.Starting(queuedStartIntent)

        publishUiStateLocked()

        return true
    }

    private fun tryLockRecordingLocked(): Boolean {
        return when (val currentSessionState = sessionState) {
            is AudioRecordingSessionState.Starting -> {
                lockStartingSessionLocked(currentSessionState)
            }

            is AudioRecordingSessionState.Recording -> {
                lockActiveSessionLocked(currentSessionState)
            }

            else -> false
        }
    }

    private fun lockStartingSessionLocked(
        currentSessionState: AudioRecordingSessionState.Starting,
    ): Boolean {
        if (currentSessionState.queuedIntent == QueuedStartIntent.Cancel) {
            return false
        }

        sessionState = currentSessionState.copy(queuedIntent = QueuedStartIntent.Lock)
        publishUiStateLocked()

        return true
    }

    private fun lockActiveSessionLocked(
        currentSessionState: AudioRecordingSessionState.Recording,
    ): Boolean {
        if (currentSessionState.isLocked) {
            return false
        }

        sessionState = currentSessionState.copy(isLocked = true)
        publishUiStateLocked()

        return true
    }

    private fun finishRecordingLocked(
        pendingAttachmentId: String,
        finishJob: Job,
    ): AudioRecordingEffect {
        return when (val currentSessionState = sessionState) {
            AudioRecordingSessionState.Idle,
            is AudioRecordingSessionState.Finalizing,
            -> AudioRecordingEffect.None

            is AudioRecordingSessionState.Starting -> {
                sessionState = currentSessionState.copy(
                    queuedIntent = QueuedStartIntent.Cancel,
                )
                publishUiStateLocked()
                AudioRecordingEffect.None
            }

            is AudioRecordingSessionState.Recording -> {
                finishActiveRecordingLocked(
                    currentSessionState = currentSessionState,
                    pendingAttachmentId = pendingAttachmentId,
                    finishJob = finishJob,
                )
            }
        }
    }

    private fun finishActiveRecordingLocked(
        currentSessionState: AudioRecordingSessionState.Recording,
        pendingAttachmentId: String,
        finishJob: Job,
    ): AudioRecordingEffect {
        val recordedDurationMillis = SystemClock.elapsedRealtime() -
            currentSessionState.startedAtMillis

        if (recordedDurationMillis.milliseconds < audioRecordMinimumDurationMillis) {
            sessionState = AudioRecordingSessionState.Idle
            publishUiStateLocked()

            return AudioRecordingEffect.StopAndDeleteRecording(
                mediaRecorder = currentSessionState.mediaRecorder,
                durationJob = currentSessionState.durationJob,
            )
        }

        sessionState = AudioRecordingSessionState.Finalizing(
            pendingAttachmentId = pendingAttachmentId,
            mediaRecorder = currentSessionState.mediaRecorder,
            stoppedRecordingUri = null,
            durationMillis = currentSessionState.durationMillis,
            finishJob = finishJob,
        )
        publishUiStateLocked()

        return AudioRecordingEffect.StartFinalization(
            finishJob = finishJob,
            durationJob = currentSessionState.durationJob,
        )
    }

    private fun cancelRecordingLocked(): AudioRecordingEffect {
        return when (val currentSessionState = sessionState) {
            AudioRecordingSessionState.Idle -> AudioRecordingEffect.None

            is AudioRecordingSessionState.Starting -> {
                sessionState = currentSessionState.copy(
                    queuedIntent = QueuedStartIntent.Cancel,
                )
                publishUiStateLocked()

                AudioRecordingEffect.None
            }

            is AudioRecordingSessionState.Recording -> {
                sessionState = AudioRecordingSessionState.Idle
                publishUiStateLocked()

                AudioRecordingEffect.StopAndDeleteRecording(
                    mediaRecorder = currentSessionState.mediaRecorder,
                    durationJob = currentSessionState.durationJob,
                )
            }

            is AudioRecordingSessionState.Finalizing -> {
                sessionState = AudioRecordingSessionState.Idle
                publishUiStateLocked()

                AudioRecordingEffect.RemovePendingAndDeleteRecording(
                    pendingAttachmentId = currentSessionState.pendingAttachmentId,
                    mediaRecorder = currentSessionState.mediaRecorder,
                    stoppedRecordingUri = currentSessionState.stoppedRecordingUri,
                    finishJob = currentSessionState.finishJob,
                )
            }
        }
    }

    private fun runAudioRecordingEffect(
        scope: CoroutineScope,
        effect: AudioRecordingEffect,
    ) {
        when (effect) {
            AudioRecordingEffect.None -> Unit

            is AudioRecordingEffect.StartFinalization -> {
                effect.durationJob.cancel()
                effect.finishJob.start()
            }

            is AudioRecordingEffect.StopAndDeleteRecording -> {
                effect.durationJob?.cancel()

                scope.launch(defaultDispatcher) {
                    val outputUri = stopRecording(mediaRecorder = effect.mediaRecorder)
                    deleteStoppedRecording(outputUri = outputUri)
                }
            }

            is AudioRecordingEffect.RemovePendingAndDeleteRecording -> {
                effect.finishJob.cancel()

                scope.launch(defaultDispatcher) {
                    conversationDraftDelegate.removePendingAttachment(
                        pendingAttachmentId = effect.pendingAttachmentId,
                    )
                    val outputUri = effect.stoppedRecordingUri
                        ?: effect.mediaRecorder?.let { mediaRecorder ->
                            stopRecording(mediaRecorder = mediaRecorder)
                        }
                    deleteStoppedRecording(outputUri = outputUri)
                }
            }
        }
    }

    private suspend fun startRecordingInBackground(selfParticipantId: String) {
        val resolvedMediaRecorder = LevelTrackingMediaRecorder()
        val maxMessageSize = conversationSubscriptionsRepository
            .resolveMaxMessageSize(selfParticipantId = selfParticipantId)
            .first()

        val didStartRecording = resolvedMediaRecorder.startRecording(
            null,
            null,
            maxMessageSize,
        )

        if (!didStartRecording) {
            withSessionStateLock {
                clearStartingSessionLocked()
            }

            return
        }

        val startedAtMillis = SystemClock.elapsedRealtime()
        val durationJob = boundScope?.launch(defaultDispatcher) {
            bindDurationTicker(startedAtMillis = startedAtMillis)
        }

        val effect = withSessionStateLock {
            completeRecorderStartLocked(
                mediaRecorder = resolvedMediaRecorder,
                startedAtMillis = startedAtMillis,
                durationJob = durationJob,
            )
        }

        runAudioRecordingEffect(
            scope = requireNotNull(boundScope) {
                "Bound scope must be available while recording starts"
            },
            effect = effect,
        )
    }

    private fun clearStartingSessionLocked() {
        if (sessionState is AudioRecordingSessionState.Starting) {
            sessionState = AudioRecordingSessionState.Idle
            publishUiStateLocked()
        }
    }

    private fun completeRecorderStartLocked(
        mediaRecorder: LevelTrackingMediaRecorder,
        startedAtMillis: Long,
        durationJob: Job?,
    ): AudioRecordingEffect {
        val currentSessionState = sessionState as? AudioRecordingSessionState.Starting
            ?: return AudioRecordingEffect.StopAndDeleteRecording(
                mediaRecorder = mediaRecorder,
                durationJob = durationJob,
            )

        if (currentSessionState.queuedIntent == QueuedStartIntent.Cancel) {
            sessionState = AudioRecordingSessionState.Idle
            publishUiStateLocked()

            return AudioRecordingEffect.StopAndDeleteRecording(
                mediaRecorder = mediaRecorder,
                durationJob = durationJob,
            )
        }

        sessionState = AudioRecordingSessionState.Recording(
            mediaRecorder = mediaRecorder,
            startedAtMillis = startedAtMillis,
            durationMillis = 0L,
            isLocked = currentSessionState.queuedIntent == QueuedStartIntent.Lock,
            durationJob = requireNotNull(durationJob) {
                "Duration job must be available for active recording"
            },
        )

        publishUiStateLocked()

        return AudioRecordingEffect.None
    }

    private suspend fun finalizeRecording(pendingAttachmentId: String) {
        addPendingAudioAttachment(pendingAttachmentId = pendingAttachmentId)
        delay(audioRecordEndingBufferMillis)

        val mediaRecorder = withSessionStateLock {
            claimFinalizingRecorderLocked(pendingAttachmentId = pendingAttachmentId)
        }

        val outputUri = mediaRecorder?.let { finalizingMediaRecorder ->
            stopRecording(mediaRecorder = finalizingMediaRecorder)
        }

        val shouldResolvePendingAttachment = withSessionStateLock {
            storeStoppedRecordingUriLocked(
                pendingAttachmentId = pendingAttachmentId,
                outputUri = outputUri,
            )
        }

        if (!shouldResolvePendingAttachment || !currentCoroutineContext().isActive) {
            deleteStoppedRecording(outputUri = outputUri)
            return
        }

        resolvePendingAudioAttachment(
            pendingAttachmentId = pendingAttachmentId,
            outputUri = outputUri,
        )

        withSessionStateLock {
            clearFinalizingSessionLocked(pendingAttachmentId = pendingAttachmentId)
        }
    }

    private fun claimFinalizingRecorderLocked(
        pendingAttachmentId: String,
    ): LevelTrackingMediaRecorder? {
        val currentSessionState = sessionState as? AudioRecordingSessionState.Finalizing
            ?: return null

        if (currentSessionState.pendingAttachmentId != pendingAttachmentId) {
            return null
        }

        sessionState = currentSessionState.copy(mediaRecorder = null)

        return currentSessionState.mediaRecorder
    }

    private fun storeStoppedRecordingUriLocked(
        pendingAttachmentId: String,
        outputUri: Uri?,
    ): Boolean {
        val currentSessionState = sessionState as? AudioRecordingSessionState.Finalizing
            ?: return false

        if (currentSessionState.pendingAttachmentId != pendingAttachmentId) {
            return false
        }

        sessionState = currentSessionState.copy(stoppedRecordingUri = outputUri)

        return true
    }

    private fun clearFinalizingSessionLocked(pendingAttachmentId: String) {
        val currentSessionState = sessionState as? AudioRecordingSessionState.Finalizing
            ?: return

        if (currentSessionState.pendingAttachmentId != pendingAttachmentId) {
            return
        }

        sessionState = AudioRecordingSessionState.Idle
        publishUiStateLocked()
    }

    private fun addPendingAudioAttachment(pendingAttachmentId: String) {
        conversationDraftDelegate.addPendingAttachment(
            pendingAttachment = ConversationDraftPendingAttachment(
                pendingAttachmentId = pendingAttachmentId,
                contentUri = createPendingAudioAttachmentUri(
                    pendingAttachmentId = pendingAttachmentId,
                ),
                contentType = ContentType.AUDIO_3GPP,
                kind = ConversationDraftPendingAttachmentKind.AudioFinalizing,
            ),
        )
    }

    private fun resolvePendingAudioAttachment(
        pendingAttachmentId: String,
        outputUri: Uri?,
    ) {
        val recordedAttachment = outputUri?.let { resolvedOutputUri ->
            ConversationDraftAttachment(
                contentType = ContentType.AUDIO_3GPP,
                contentUri = resolvedOutputUri.toString(),
            )
        }

        when (recordedAttachment) {
            null -> {
                conversationDraftDelegate.removePendingAttachment(
                    pendingAttachmentId = pendingAttachmentId,
                )
            }

            else -> {
                conversationDraftDelegate.resolvePendingAttachment(
                    pendingAttachmentId = pendingAttachmentId,
                    attachment = recordedAttachment,
                )
            }
        }
    }

    private suspend fun bindDurationTicker(startedAtMillis: Long) {
        while (true) {
            val shouldContinue = withSessionStateLock {
                tickRecordingDurationLocked(startedAtMillis = startedAtMillis)
            }

            if (!shouldContinue) {
                return
            }

            delay(durationTickIntervalMillis)
        }
    }

    private fun tickRecordingDurationLocked(startedAtMillis: Long): Boolean {
        val currentSessionState = sessionState as? AudioRecordingSessionState.Recording
            ?: return false

        if (currentSessionState.startedAtMillis != startedAtMillis) {
            return false
        }

        sessionState = currentSessionState.copy(
            durationMillis = SystemClock.elapsedRealtime() - startedAtMillis,
        )
        publishUiStateLocked()

        return true
    }

    private fun stopRecording(mediaRecorder: LevelTrackingMediaRecorder): Uri? {
        return try {
            mediaRecorder.stopRecording()
        } catch (throwable: Throwable) {
            LogUtil.w(TAG, "Failed to stop audio recording", throwable)
            null
        }
    }

    private suspend fun deleteStoppedRecording(outputUri: Uri?) {
        outputUri ?: return

        conversationAttachmentRepository
            .deleteTemporaryAttachment(
                contentUri = outputUri.toString(),
            )
            .collect()
    }

    private fun publishUiStateLocked() {
        _state.value = createUiState(sessionState = sessionState)
    }

    private fun createUiState(
        sessionState: AudioRecordingSessionState,
    ): ConversationAudioRecordingUiState {
        return when (sessionState) {
            AudioRecordingSessionState.Idle -> ConversationAudioRecordingUiState()

            is AudioRecordingSessionState.Starting -> {
                createStartingUiState(
                    queuedIntent = sessionState.queuedIntent,
                )
            }

            is AudioRecordingSessionState.Recording -> {
                ConversationAudioRecordingUiState(
                    phase = ConversationAudioRecordingPhase.Recording,
                    durationMillis = sessionState.durationMillis,
                    isLocked = sessionState.isLocked,
                )
            }

            is AudioRecordingSessionState.Finalizing -> {
                ConversationAudioRecordingUiState(
                    phase = ConversationAudioRecordingPhase.Finalizing,
                    durationMillis = sessionState.durationMillis,
                )
            }
        }
    }

    private fun createStartingUiState(
        queuedIntent: QueuedStartIntent,
    ): ConversationAudioRecordingUiState {
        return when {
            queuedIntent == QueuedStartIntent.Cancel -> {
                ConversationAudioRecordingUiState()
            }

            else -> {
                ConversationAudioRecordingUiState(
                    phase = ConversationAudioRecordingPhase.Recording,
                    isLocked = queuedIntent == QueuedStartIntent.Lock,
                )
            }
        }
    }

    private fun createPendingAudioAttachmentId(): String {
        return "pending-audio-${UUID.randomUUID()}"
    }

    private fun createPendingAudioAttachmentUri(pendingAttachmentId: String): String {
        return "${PENDING_AUDIO_URI_PREFIX}$pendingAttachmentId"
    }

    private sealed interface AudioRecordingSessionState {
        data object Idle : AudioRecordingSessionState

        data class Starting(
            val queuedIntent: QueuedStartIntent = QueuedStartIntent.None,
        ) : AudioRecordingSessionState

        data class Recording(
            val mediaRecorder: LevelTrackingMediaRecorder,
            val startedAtMillis: Long,
            val durationMillis: Long,
            val isLocked: Boolean,
            val durationJob: Job,
        ) : AudioRecordingSessionState

        data class Finalizing(
            val pendingAttachmentId: String,
            val mediaRecorder: LevelTrackingMediaRecorder?,
            val stoppedRecordingUri: Uri?,
            val durationMillis: Long,
            val finishJob: Job,
        ) : AudioRecordingSessionState
    }

    private enum class QueuedStartIntent {
        None,
        Lock,
        Cancel,
    }

    private sealed interface AudioRecordingEffect {
        data object None : AudioRecordingEffect

        data class StartFinalization(
            val finishJob: Job,
            val durationJob: Job,
        ) : AudioRecordingEffect

        data class StopAndDeleteRecording(
            val mediaRecorder: LevelTrackingMediaRecorder,
            val durationJob: Job?,
        ) : AudioRecordingEffect

        data class RemovePendingAndDeleteRecording(
            val pendingAttachmentId: String,
            val mediaRecorder: LevelTrackingMediaRecorder?,
            val stoppedRecordingUri: Uri?,
            val finishJob: Job,
        ) : AudioRecordingEffect
    }

    private companion object {
        private const val TAG = "ConversationAudioRecording"
        private const val PENDING_AUDIO_URI_PREFIX = "pending://audio/"

        private val audioRecordEndingBufferMillis = 500L.milliseconds
        private val audioRecordMinimumDurationMillis = 300L.milliseconds
        private val durationTickIntervalMillis = 200L.milliseconds
    }
}
