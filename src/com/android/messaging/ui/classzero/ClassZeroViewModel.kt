package com.android.messaging.ui.classzero

import android.content.ContentValues
import android.os.SystemClock
import android.provider.Telephony.Sms
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.classzero.usecase.SaveClassZeroMessage
import com.android.messaging.ui.classzero.model.ClassZeroScreenEffect
import com.android.messaging.ui.classzero.model.ClassZeroUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val CLASS_ZERO_DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1000L
internal const val CLASS_ZERO_TIMER_FIRE_STATE_KEY = "timer_fire"

internal interface ClassZeroScreenModel {
    val effects: Flow<ClassZeroScreenEffect>
    val uiState: StateFlow<ClassZeroUiState?>

    fun onSaveClicked()
    fun onCancelClicked()

    fun onInitialMessageReceived(messageValues: ContentValues?)
    fun onNewMessageReceived(messageValues: ContentValues?)
}

@HiltViewModel
internal class ClassZeroViewModel @Inject constructor(
    private val saveClassZeroMessage: SaveClassZeroMessage,
    private val savedStateHandle: SavedStateHandle,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) : ViewModel(),
    ClassZeroScreenModel {

    private val messageQueue = ArrayDeque<ContentValues>()
    private var currentMessageValues: ContentValues? = null
    private var restoredTimerFireAtUptimeMillis = savedStateHandle
        .get<Long>(
            CLASS_ZERO_TIMER_FIRE_STATE_KEY,
        )
        ?: 0L

    private var messageSequence = 0
    private var timeoutJob: Job? = null
    private var isCompletingCurrentMessage = false

    private val _effects = MutableSharedFlow<ClassZeroScreenEffect>(
        replay = 1,
        extraBufferCapacity = 1,
    )
    override val effects = _effects.asSharedFlow()

    private val _uiState = MutableStateFlow<ClassZeroUiState?>(value = null)
    override val uiState = _uiState.asStateFlow()

    override fun onInitialMessageReceived(messageValues: ContentValues?) {
        if (!isFinished()) {
            return
        }

        enqueueMessage(messageValues = messageValues)
        finishIfEmpty()
    }

    override fun onNewMessageReceived(messageValues: ContentValues?) {
        enqueueMessage(messageValues = messageValues)
        finishIfEmpty()
    }

    override fun onSaveClicked() {
        completeCurrentMessage(
            saveMessage = true,
            isRead = true,
        )
    }

    override fun onCancelClicked() {
        completeCurrentMessage(
            saveMessage = false,
            isRead = false,
        )
    }

    private fun handleTimeoutExpired(sequence: Int) {
        if (sequence != messageSequence || currentMessageValues == null) {
            return
        }

        timeoutJob = null
        completeCurrentMessage(
            saveMessage = true,
            isRead = false,
        )
    }

    private fun enqueueMessage(messageValues: ContentValues?): Boolean {
        val sourceMessageValues = messageValues ?: return false
        val messageText = sourceMessageValues.classZeroMessageText

        return when {
            messageText == null -> false

            else -> {
                messageQueue.addLast(ContentValues(sourceMessageValues))
                if (currentMessageValues == null) {
                    showNextMessage()
                }

                true
            }
        }
    }

    private fun completeCurrentMessage(saveMessage: Boolean, isRead: Boolean) {
        if (isCompletingCurrentMessage) {
            return
        }

        val messageValues = currentMessageValues ?: return
        cancelTimeout()
        isCompletingCurrentMessage = true

        when {
            saveMessage -> {
                val readValue = when {
                    isRead -> 1
                    else -> 0
                }

                val valuesToSave = ContentValues(messageValues).apply {
                    put(Sms.Inbox.READ, readValue)
                }

                launchSaveMessageAndCompleteCurrentMessage(messageValues = valuesToSave)
            }

            else -> {
                finishCurrentMessage()
            }
        }
    }

    private fun finishCurrentMessage() {
        isCompletingCurrentMessage = false
        currentMessageValues = null
        _uiState.value = null
        savedStateHandle[CLASS_ZERO_TIMER_FIRE_STATE_KEY] = 0L

        showNextMessage()
        finishIfEmpty()
    }

    private fun launchSaveMessageAndCompleteCurrentMessage(messageValues: ContentValues) {
        viewModelScope.launch(defaultDispatcher) {
            saveClassZeroMessage(messageValues = messageValues).collect()
            withContext(mainDispatcher) {
                finishCurrentMessage()
            }
        }
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private fun showNextMessage() {
        var nextMessageValues = messageQueue.removeFirstOrNull()
        while (nextMessageValues != null) {
            nextMessageValues
                .classZeroMessageText
                ?.let { messageText ->
                    displayMessage(
                        messageValues = nextMessageValues,
                        messageText = messageText,
                    )
                    return
                }

            nextMessageValues = messageQueue.removeFirstOrNull()
        }
    }

    private fun displayMessage(messageValues: ContentValues, messageText: String) {
        currentMessageValues = messageValues
        messageSequence += 1

        val timerFireAtUptimeMillis = restoredTimerFireAtUptimeMillis
            .takeIf { it > 0L }
            ?: (SystemClock.uptimeMillis() + CLASS_ZERO_DEFAULT_TIMEOUT_MILLIS)

        restoredTimerFireAtUptimeMillis = 0L
        savedStateHandle[CLASS_ZERO_TIMER_FIRE_STATE_KEY] = timerFireAtUptimeMillis

        _uiState.value = ClassZeroUiState(
            messageText = messageText,
        )

        scheduleTimeout(
            sequence = messageSequence,
            timerFireAtUptimeMillis = timerFireAtUptimeMillis,
        )
    }

    private fun scheduleTimeout(sequence: Int, timerFireAtUptimeMillis: Long) {
        cancelTimeout()

        timeoutJob = viewModelScope.launch(mainDispatcher) {
            val timeoutDelayMillis = timerFireAtUptimeMillis - SystemClock.uptimeMillis()
            if (timeoutDelayMillis > 0L) {
                delay(timeoutDelayMillis.milliseconds)
            }

            handleTimeoutExpired(sequence = sequence)
        }
    }

    private fun finishIfEmpty() {
        if (isFinished()) {
            emitEffect(effect = ClassZeroScreenEffect.Finish)
        }
    }

    private fun isFinished(): Boolean {
        return currentMessageValues == null && messageQueue.isEmpty()
    }

    private fun emitEffect(effect: ClassZeroScreenEffect) {
        _effects.tryEmit(effect)
    }

    private val ContentValues.classZeroMessageText: String?
        get() {
            return getAsString(Sms.BODY)
                ?.takeIf { it.isNotBlank() }
        }
}
