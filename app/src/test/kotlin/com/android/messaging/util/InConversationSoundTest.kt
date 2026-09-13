package com.android.messaging.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.data.conversationsettings.repository.ConversationSnoozeQuery
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.createIncomingMessagesTestChannel
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InConversationSoundTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val prefs = FakeBuglePrefs()
    private val ringtone = mockk<Ringtone>(relaxed = true)
    private val scheduler = TestCoroutineScheduler()

    // The worker is serialized in production; UnconfinedTestDispatcher runs it eagerly so the
    // assertions below stay synchronous, while the scheduler still drives the five second stop.
    private val sound = InConversationSound(dispatcher = UnconfinedTestDispatcher(scheduler))

    @Before
    fun setUp() {
        installTestFactory(context = context, prefs = prefs)
        createIncomingMessagesTestChannel()
        mockkStatic(RingtoneUtil::class)
        every { RingtoneUtil.getNotificationRingtoneUri(any(), any()) } returns RINGTONE_URI
        mockkStatic(RingtoneManager::class)
        every { RingtoneManager.getRingtone(any(), any()) } returns ringtone
        mockkStatic(ConversationSnoozeQuery::class)
        every { ConversationSnoozeQuery.isConversationSnoozed(any()) } returns false
        mockkStatic(BugleNotifications::class)
        every { BugleNotifications.isConversationBlocked(any()) } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun post_byDefault_staysSilent() {
        // The chime is opt-in: issue #298 is fixed for anyone who never touches the setting.
        sound.post(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_whenEnabled_plays() {
        givenSoundEnabled()

        sound.post(CONVERSATION_ID)

        verify(exactly = 1) { ringtone.play() }
    }

    @Test
    fun post_withoutConversationId_staysSilent() {
        givenSoundEnabled()

        sound.post(null)
        sound.post("")

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_whenDoNotDisturbIsOn_staysSilent() {
        givenSoundEnabled()
        shadowOf(notificationManager()).setNotificationPolicyAccessGranted(true)
        notificationManager()
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)

        sound.post(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_whenRingerIsSilenced_staysSilent() {
        givenSoundEnabled()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

        sound.post(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_whenAppNotificationsAreDisabled_staysSilent() {
        givenSoundEnabled()
        shadowOf(notificationManager()).setNotificationsEnabled(false)

        sound.post(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_whenConversationIsMuted_staysSilent() {
        givenSoundEnabled()
        givenConversationChannel(NotificationManager.IMPORTANCE_LOW)

        sound.post(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_whenTheIncomingMessagesChannelIsSilenced_staysSilent() {
        // A conversation the user has only ever read live has no channel of its own yet, so the
        // one it would inherit from is what decides.
        givenSoundEnabled()
        createIncomingMessagesTestChannel(importance = NotificationManager.IMPORTANCE_LOW)

        sound.post(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_whenTheSenderIsBlocked_staysSilent() {
        givenSoundEnabled()
        every { BugleNotifications.isConversationBlocked(CONVERSATION_ID) } returns true

        sound.post(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_whenConversationIsSnoozed_staysSilent() {
        givenSoundEnabled()
        every { ConversationSnoozeQuery.isConversationSnoozed(CONVERSATION_ID) } returns true

        sound.post(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_whenConversationSoundIsNone_staysSilent() {
        givenSoundEnabled()
        every { RingtoneUtil.getNotificationRingtoneUri(any(), any()) } returns null

        sound.post(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun post_doesNothingOnTheCallerThread() {
        // The caller can be inside a database transaction, and preparing a ringtone blocks on
        // mediaserver, so nothing may touch the media or notification stack before the worker runs.
        givenSoundEnabled()
        val posted = InConversationSound(dispatcher = StandardTestDispatcher(scheduler))

        posted.post(CONVERSATION_ID)

        verify(exactly = 0) { BugleNotifications.isConversationBlocked(any()) }
        verify(exactly = 0) { RingtoneManager.getRingtone(any(), any()) }
        verify(exactly = 0) { ringtone.play() }

        scheduler.runCurrent()

        verify(exactly = 1) { ringtone.play() }
    }

    @Test
    fun post_whileAChimeIsPlaying_dropsTheNewOne() {
        givenSoundEnabled()
        every { ringtone.isPlaying } returns true

        sound.post(CONVERSATION_ID)
        sound.post(CONVERSATION_ID)

        verify(exactly = 1) { ringtone.play() }
        verify(exactly = 0) { ringtone.stop() }
    }

    @Test
    fun post_afterAChimeEnded_releasesItBeforePlayingTheNextOne() {
        // A ringtone that ended on its own still holds its MediaPlayer until stop() releases it.
        givenSoundEnabled()
        val first = mockk<Ringtone>(relaxed = true)
        val second = mockk<Ringtone>(relaxed = true)
        every { RingtoneManager.getRingtone(any(), any()) } returnsMany listOf(first, second)

        sound.post(CONVERSATION_ID)
        sound.post(CONVERSATION_ID)

        verifyOrder {
            first.play()
            first.stop()
            second.play()
        }
    }

    @Test
    fun post_stopsTheChimeAfterFiveSeconds() {
        // Restores the cap BugleNotifications used to post to the main thread, so a ringtone that
        // is a whole song does not play in full.
        givenSoundEnabled()

        sound.post(CONVERSATION_ID)
        scheduler.advanceTimeBy(MAX_DURATION_MS)

        verify(exactly = 0) { ringtone.stop() }

        scheduler.runCurrent()

        verify(exactly = 1) { ringtone.stop() }
    }

    @Test
    fun post_whenAMessageIsDropped_doesNotExtendTheStop() {
        givenSoundEnabled()
        every { ringtone.isPlaying } returns true

        sound.post(CONVERSATION_ID)
        scheduler.advanceTimeBy(MAX_DURATION_MS - 100)
        sound.post(CONVERSATION_ID)
        scheduler.advanceTimeBy(100)
        scheduler.runCurrent()

        verify(exactly = 1) { ringtone.stop() }
    }

    @Test
    fun post_whenAnEndedChimeIsReplaced_doesNotCutTheNewOneShort() {
        // The first chime's timer must not stop the second one three seconds early.
        givenSoundEnabled()
        val first = mockk<Ringtone>(relaxed = true)
        val second = mockk<Ringtone>(relaxed = true)
        every { RingtoneManager.getRingtone(any(), any()) } returnsMany listOf(first, second)

        sound.post(CONVERSATION_ID)
        scheduler.advanceTimeBy(3_000)
        sound.post(CONVERSATION_ID)

        scheduler.advanceTimeBy(2_000)
        scheduler.runCurrent()

        verify(exactly = 0) { second.stop() }

        scheduler.advanceTimeBy(3_000)
        scheduler.runCurrent()

        verify(exactly = 1) { second.stop() }
    }

    @Test
    fun post_whenTheChimeFails_releasesTheRingtoneWithoutAnotherMessage() {
        // A failed chime still holds a prepared MediaPlayer, and nothing guarantees a next message
        // to clean up after it, so the cap has to be armed before play() can throw.
        givenSoundEnabled()
        every { ringtone.play() } throws IllegalStateException("mediaserver died")

        sound.post(CONVERSATION_ID)
        scheduler.advanceTimeBy(MAX_DURATION_MS)
        scheduler.runCurrent()

        verify(exactly = 1) { ringtone.stop() }
    }

    @Test
    fun post_whenTheChimeFails_doesNotCrashTheProcess() {
        // The caller may still be inside a database transaction, and BugleApplication forwards an
        // uncaught background throwable to the system handler. That would kill the process along
        // with the transaction, so a mediaserver or database failure has to stop here.
        givenSoundEnabled()
        every { ringtone.play() } throws IllegalStateException("mediaserver died")
        val escaped = mutableListOf<Throwable>()
        val worker = Thread.currentThread()
        val previous = worker.uncaughtExceptionHandler
        worker.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, failure ->
            escaped.add(failure)
        }

        try {
            sound.post(CONVERSATION_ID)
            // A no-op while the worker runs eagerly, and the whole point under a queued
            // dispatcher: the failure has to land while this thread's handler is still swapped.
            scheduler.runCurrent()
        } finally {
            worker.uncaughtExceptionHandler = previous
        }

        assertTrue("the failure reached the uncaught handler: $escaped", escaped.isEmpty())

        // The next message still chimes: the failure did not poison the worker.
        every { ringtone.play() } just runs
        sound.post(CONVERSATION_ID)

        verify(exactly = 2) { ringtone.play() }
    }

    private fun givenSoundEnabled() {
        prefs.putBoolean(context.getString(R.string.in_conversation_sound_pref_key), true)
    }

    private fun givenConversationChannel(importance: Int) {
        val channel = NotificationChannel(CONVERSATION_ID, "Alice", importance)
        channel.setConversationId(NotificationChannelUtil.INCOMING_MESSAGES, CONVERSATION_ID)
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private companion object {
        private const val CONVERSATION_ID = "194"
        private const val MAX_DURATION_MS = 5_000L
        private val RINGTONE_URI: Uri = Uri.parse("content://settings/system/notification_sound")
    }
}
