package com.android.messaging.datamodel.action

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DataModel
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class BackgroundWorkerServiceResponseSizeTest {

    private val actionService = RecordingActionService()

    @Before
    fun setUp() {
        val dataModel = mockk<DataModel>(relaxed = true)
        every { dataModel.actionService } returns actionService
        installTestFactory(
            context = RuntimeEnvironment.getApplication().applicationContext,
            dataModel = dataModel,
        )
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun oversizedResponseIsReportedAsFailure() {
        val action = FixedResponseAction(
            responseBytes = BackgroundWorkerService.MAX_RESPONSE_SIZE_BYTES,
        )

        runBackgroundWork(action = action)

        assertNull(actionService.response)
        assertSame(action, actionService.failedAction)
        assertTrue(actionService.failure is IllegalStateException)
    }

    @Test
    fun responseWithinLimitIsDelivered() {
        val action = FixedResponseAction(
            responseBytes = BackgroundWorkerService.MAX_RESPONSE_SIZE_BYTES / 2,
        )

        runBackgroundWork(action = action)

        assertSame(action, actionService.respondedAction)
        assertSame(action.response, actionService.response)
        assertNull(actionService.failure)
    }

    @Test
    fun missingResponseIsDelivered() {
        val action = FixedResponseAction(responseBytes = null)

        runBackgroundWork(action = action)

        assertSame(action, actionService.respondedAction)
        assertNull(actionService.response)
        assertNull(actionService.failure)
    }

    @Test
    fun oversizedActionIsReportedAsFailure() {
        val action = FixedResponseAction(responseBytes = null)
        action.actionParameters.putByteArray(
            PAYLOAD_KEY,
            ByteArray(BackgroundWorkerService.MAX_RESPONSE_SIZE_BYTES),
        )

        runBackgroundWork(action = action)

        assertNull(actionService.respondedAction)
        assertSame(action, actionService.failedAction)
    }

    private fun runBackgroundWork(action: Action) {
        val intent = Intent()
            .putExtra(
                BackgroundWorkerService.EXTRA_OP_CODE,
                BackgroundWorkerService.OP_PROCESS_REQUEST,
            )
            .putExtra(BackgroundWorkerService.EXTRA_ACTION, action)
        BackgroundWorkerService().onHandleWork(intent)
    }

    private class FixedResponseAction(
        responseBytes: Int?,
    ) : Action() {
        val response: Bundle? = responseBytes?.let { size ->
            Bundle().apply { putByteArray(PAYLOAD_KEY, ByteArray(size)) }
        }

        override fun doBackgroundWork(): Bundle? {
            return response
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            writeActionToParcel(parcel, flags)
        }
    }

    private class RecordingActionService : ActionService() {
        var respondedAction: Action? = null
        var response: Bundle? = null
        var failedAction: Action? = null
        var failure: Exception? = null

        override fun handleResponseFromBackgroundWorker(action: Action, response: Bundle?) {
            respondedAction = action
            this.response = response
        }

        override fun handleFailureFromBackgroundWorker(action: Action, exception: Exception) {
            failedAction = action
            failure = exception
        }
    }

    private companion object {
        private const val PAYLOAD_KEY = "payload"
    }
}
