package com.android.messaging.datamodel

import com.android.messaging.FactoryTestAccess
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BugleNotificationsConcurrentPassTest {

    private val phoneUtils = mockk<PhoneUtils>(relaxed = true)

    @Before
    fun setUp() {
        installTestFactory(
            context = RuntimeEnvironment.getApplication().applicationContext,
            phoneUtils = phoneUtils,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun update_whileAnotherPassIsRunning_waitsForIt() {
        val firstIsInside = CountDownLatch(1)
        val secondGotInside = CountDownLatch(1)
        val overlapped = AtomicBoolean(false)
        val passes = AtomicInteger()

        // The first thing a pass does once it is under way. Returning false makes it stop here, so
        // the test observes the pass boundary and nothing else.
        every { phoneUtils.isDefaultSmsApp() } answers {
            when (passes.getAndIncrement()) {
                0 -> {
                    firstIsInside.countDown()
                    overlapped.set(secondGotInside.await(OVERLAP_TIMEOUT_MILLIS, MILLISECONDS))
                }

                else -> secondGotInside.countDown()
            }
            false
        }

        val first = thread { BugleNotifications.update("1", BugleNotifications.UPDATE_MESSAGES) }
        assertTrue("the first pass never started", firstIsInside.await(5, SECONDS))
        val second = thread { BugleNotifications.update("2", BugleNotifications.UPDATE_MESSAGES) }
        first.join()
        second.join()

        assertEquals("both passes should have run", 2, passes.get())
        assertFalse("a second notification pass ran inside the first one", overlapped.get())
    }

    private companion object {
        const val OVERLAP_TIMEOUT_MILLIS = 500L
    }
}
