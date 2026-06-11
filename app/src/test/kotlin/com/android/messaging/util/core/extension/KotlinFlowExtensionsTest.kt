package com.android.messaging.util.core.extension

import app.cash.turbine.test
import com.android.messaging.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KotlinFlowExtensionsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun typedFlow_emitsReturnedValue() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            typedFlow {
                emit("before")
                return@typedFlow "after"
            }.test {
                assertEquals("before", awaitItem())
                assertEquals("after", awaitItem())
                awaitComplete()
            }
        }
    }

    @Test
    fun unitFlow_runsBlockThenEmitsUnit() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val collectedValues = mutableListOf<String>()

            unitFlow {
                collectedValues += "executed"
            }.test {
                assertEquals(listOf("executed"), collectedValues)
                assertEquals(Unit, awaitItem())
                awaitComplete()
            }
        }
    }
}
