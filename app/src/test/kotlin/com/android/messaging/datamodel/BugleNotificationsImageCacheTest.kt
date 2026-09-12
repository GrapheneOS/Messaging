package com.android.messaging.datamodel

import android.graphics.Bitmap
import android.net.Uri
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.media.ImageResource
import com.android.messaging.datamodel.media.MediaResourceManager
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.createTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.time.Duration.Companion.minutes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BugleNotificationsImageCacheTest {

    private val context = RuntimeEnvironment.getApplication().applicationContext
    private val mediaResourceManager = mockk<MediaResourceManager>()

    @Before
    fun setUp() {
        val factory = createTestFactory(
            context = context,
            dataModel = mockk(relaxed = true),
            prefs = FakeBuglePrefs(),
        )
        every { factory.mediaResourceManager } returns mediaResourceManager
        FactoryTestAccess.install(factory)

        every { mediaResourceManager.requestMediaResourceSync<ImageResource>(any()) } answers {
            mockk<ImageResource>(relaxed = true) {
                every { bitmap } returns Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun getNotificationImageUri_forSameAttachmentPart_transcodesOnce() {
        val first = BugleNotifications.getNotificationImageUri(context, ATTACHMENT, "7")
        val second = BugleNotifications.getNotificationImageUri(context, ATTACHMENT, "7")

        assertNotNull(first)
        assertEquals(first, second)
        verify(exactly = 1) { mediaResourceManager.requestMediaResourceSync<ImageResource>(any()) }
    }

    @Test
    fun getNotificationImageUri_forDifferentAttachmentParts_transcodesEach() {
        val first = BugleNotifications.getNotificationImageUri(context, ATTACHMENT, "7")
        val second = BugleNotifications.getNotificationImageUri(context, ATTACHMENT, "8")

        assertNotNull(first)
        assertNotNull(second)
        assertNotEquals(first, second)
        verify(exactly = 2) { mediaResourceManager.requestMediaResourceSync<ImageResource>(any()) }
    }

    /** The sweep reclaims images once their notification is gone; the next pass must rebuild them. */
    @Test
    fun getNotificationImageUri_afterCachedImageSwept_transcodesAgain() {
        val first = BugleNotifications.getNotificationImageUri(context, ATTACHMENT, "7")
        NotificationImageProvider.getFileFromUri(first)!!.delete()

        val second = BugleNotifications.getNotificationImageUri(context, ATTACHMENT, "7")

        assertNotNull(second)
        assertEquals(first, second)
        verify(exactly = 2) { mediaResourceManager.requestMediaResourceSync<ImageResource>(any()) }
    }

    /**
     * A cache hit has to touch the file, or [BugleNotifications.sweepNotificationImages] reclaims an
     * image that a notification posted in this pass still points at.
     */
    @Test
    fun getNotificationImageUri_onCacheHit_touchesFileSoTheSweepSpareIt() {
        val uri = BugleNotifications.getNotificationImageUri(context, ATTACHMENT, "7")
        val file = NotificationImageProvider.getFileFromUri(uri)!!
        val stale = System.currentTimeMillis() - 10.minutes.inWholeMilliseconds
        assertTrue(file.setLastModified(stale))

        BugleNotifications.getNotificationImageUri(context, ATTACHMENT, "7")

        assertTrue(file.lastModified() > stale)
    }

    /** Parts that are not persisted yet have no id, and must still get an image. */
    @Test
    fun getNotificationImageUri_withoutPartId_stillTranscodes() {
        val uri = BugleNotifications.getNotificationImageUri(context, ATTACHMENT, null)

        assertNotNull(uri)
        assertTrue(NotificationImageProvider.isNotificationImageUri(uri))
    }

    private companion object {
        val ATTACHMENT: Uri = Uri.parse("content://mms/part/1")
    }
}
