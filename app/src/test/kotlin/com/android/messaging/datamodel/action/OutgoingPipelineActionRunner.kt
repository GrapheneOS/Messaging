package com.android.messaging.datamodel.action

import android.os.Bundle
import android.os.Parcel

internal data class ScheduledAlarm(
    val requestCode: Int,
    val delayMillis: Long,
    val action: Action,
)

/**
 * Stands in for the ActionService and BackgroundWorkerService process boundary: every action
 * crosses it as a parcel, as it does through the real intents, and alarms wait until fired.
 */
internal class RecordingActionService : ActionService() {

    private val startedActions = ArrayDeque<Action>()
    private val pendingAlarms = LinkedHashMap<Int, ScheduledAlarm>()

    @Synchronized
    override fun startAction(action: Action) {
        action.markStart()
        startedActions.addLast(action.parcelRoundTrip())
    }

    @Synchronized
    override fun scheduleAction(action: Action, code: Int, delayMs: Long) {
        when (delayMs) {
            Long.MAX_VALUE -> pendingAlarms.remove(code)
            else -> pendingAlarms[code] = ScheduledAlarm(
                requestCode = code,
                delayMillis = delayMs,
                action = action.parcelRoundTrip(),
            )
        }
    }

    @Synchronized
    fun pollStartedAction(): Action? {
        return startedActions.removeFirstOrNull()
    }

    @Synchronized
    fun pendingAlarm(requestCode: Int): ScheduledAlarm? {
        return pendingAlarms[requestCode]
    }

    @Synchronized
    fun pendingAlarms(): List<ScheduledAlarm> {
        return pendingAlarms.values.toList()
    }

    @Synchronized
    fun fireAlarm(requestCode: Int) {
        val alarm = requireNotNull(pendingAlarms.remove(requestCode)) {
            "No alarm is scheduled for request code $requestCode"
        }
        startAction(alarm.action)
    }
}

internal class CapturingBackgroundWorker : BackgroundWorker() {

    private val queuedActions = ArrayDeque<Action>()

    @Synchronized
    override fun queueBackgroundWork(backgroundActions: List<Action>) {
        backgroundActions.forEach { action ->
            queuedActions.addLast(action.parcelRoundTrip())
        }
    }

    @Synchronized
    fun pollQueuedAction(): Action? {
        return queuedActions.removeFirstOrNull()
    }
}

/**
 * Runs actions the way ActionServiceImpl and BackgroundWorkerService do, one at a time, started
 * actions first. Must be called on a thread that may touch the database.
 */
internal class OutgoingPipelineActionRunner(
    private val actionService: RecordingActionService,
    private val backgroundWorker: CapturingBackgroundWorker,
) {

    fun runUntilIdle() {
        var steps = 0
        while (runNextStep()) {
            steps++
            check(steps < MAX_STEPS) { "Actions did not settle after $MAX_STEPS steps" }
        }
    }

    private fun runNextStep(): Boolean {
        val startedAction = actionService.pollStartedAction()
        val backgroundAction = when (startedAction) {
            null -> backgroundWorker.pollQueuedAction()
            else -> null
        }
        when {
            startedAction != null -> executeStartedAction(action = startedAction)
            backgroundAction != null -> executeBackgroundAction(action = backgroundAction)
        }
        return startedAction != null || backgroundAction != null
    }

    private fun executeStartedAction(action: Action) {
        action.markBeginExecute()
        val result = action.executeAction()
        action.markEndExecute(result)
        action.sendBackgroundActions(backgroundWorker)
    }

    private fun executeBackgroundAction(action: Action) {
        action.markBackgroundWorkStarting()
        val response: Result<Bundle?> = runCatching { action.doBackgroundWork() }
        action.markBackgroundCompletionQueued()
        val deliveredAction = action.parcelRoundTrip()
        response
            .onSuccess { bundle -> deliveredAction.processBackgroundWorkResponse(bundle) }
            .onFailure { throwable ->
                if (throwable !is Exception) {
                    throw throwable
                }
                deliveredAction.processBackgroundWorkFailure()
            }
        deliveredAction.sendBackgroundActions(backgroundWorker)
    }

    private companion object {
        private const val MAX_STEPS = 500
    }
}

internal fun Action.parcelRoundTrip(): Action {
    val parcel = Parcel.obtain()
    try {
        parcel.writeParcelable(this, 0)
        parcel.setDataPosition(0)
        return requireNotNull(
            parcel.readParcelable(Action::class.java.classLoader, Action::class.java),
        )
    } finally {
        parcel.recycle()
    }
}
