package com.android.messaging.ui.conversation.v2.audio.delegate

import android.net.Uri
import android.os.SystemClock
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.repository.ConversationSubscriptionsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.repository.ConversationAttachmentRepository
import com.android.messaging.ui.mediapicker.LevelTrackingMediaRecorder
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal interface ConversationAudioRecordingDelegate :
    ConversationScreenDelegate<ConversationAudioRecordingUiState> {

    fun startRecording(selfParticipantId: String)

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

    private var boundScope: CoroutineScope? = null
    private var durationJob: Job? = null
    private var finishRecordingJob: Job? = null
    private var mediaRecorder: LevelTrackingMediaRecorder? = null
    private var recordingStartedAtMillis: Long? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope
        scope.launch(defaultDispatcher) {
            conversationIdFlow.collect {
                cancelRecording()
            }
        }
    }

    override fun startRecording(selfParticipantId: String) {
        if (state.value.isRecording) {
            return
        }

        boundScope?.launch(defaultDispatcher) {
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
                return@launch
            }

            mediaRecorder = resolvedMediaRecorder
            recordingStartedAtMillis = SystemClock.elapsedRealtime()
            _state.value = ConversationAudioRecordingUiState(
                isRecording = true,
            )
            bindDurationTicker(scope = this)
        }
    }

    override fun finishRecording() {
        if (!state.value.isRecording) {
            return
        }

        finishRecordingJob?.cancel()

        finishRecordingJob = boundScope?.launch(defaultDispatcher) {
            val recordedDurationMillis = when (val startedAtMillis = recordingStartedAtMillis) {
                null -> 0L
                else -> SystemClock.elapsedRealtime() - startedAtMillis
            }

            if (recordedDurationMillis.milliseconds < audioRecordMinimumDurationMillis) {
                deleteStoppedRecording(stopRecording())
                resetRecordingState()
                return@launch
            }

            delay(audioRecordEndingBufferMillis)

            val recordedAttachment = stopRecording()?.let { outputUri ->
                ConversationDraftAttachment(
                    contentType = ContentType.AUDIO_3GPP,
                    contentUri = outputUri.toString(),
                )
            }

            recordedAttachment?.let { attachment ->
                conversationDraftDelegate.addAttachments(
                    attachments = listOf(attachment),
                )
            }

            resetRecordingState()
        }
    }

    override fun cancelRecording() {
        if (!state.value.isRecording) {
            return
        }

        boundScope?.launch(defaultDispatcher) {
            finishRecordingJob?.cancel()
            deleteStoppedRecording(stopRecording())
            resetRecordingState()
        }
    }

    override fun onScreenCleared() {
        cancelRecording()
    }

    private fun bindDurationTicker(scope: CoroutineScope) {
        durationJob?.cancel()
        durationJob = scope.launch(defaultDispatcher) {
            while (state.value.isRecording) {
                val resolvedStartMillis = recordingStartedAtMillis ?: break
                _state.value = ConversationAudioRecordingUiState(
                    isRecording = true,
                    durationMillis = SystemClock.elapsedRealtime() - resolvedStartMillis,
                )
                delay(durationTickIntervalMillis)
            }
        }
    }

    private fun stopRecording(): Uri? {
        val resolvedMediaRecorder = mediaRecorder ?: return null

        return try {
            resolvedMediaRecorder.stopRecording()
        } catch (throwable: Throwable) {
            LogUtil.w(TAG, "Failed to stop audio recording", throwable)
            null
        } finally {
            mediaRecorder = null
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

    private fun resetRecordingState() {
        finishRecordingJob = null
        durationJob?.cancel()
        durationJob = null
        mediaRecorder = null
        recordingStartedAtMillis = null
        _state.value = ConversationAudioRecordingUiState()
    }

    private companion object {
        private const val TAG = "ConversationAudioRecording"

        private val audioRecordEndingBufferMillis = 500L.milliseconds
        private val audioRecordMinimumDurationMillis = 300L.milliseconds
        private val durationTickIntervalMillis = 200L.milliseconds
    }
}
