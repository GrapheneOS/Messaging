package com.android.messaging.datamodel

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.util.ContentType
import com.android.messaging.util.NotificationChannelUtil
import java.io.File
import java.io.FileNotFoundException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageNotificationAttachmentUriTest {

    private lateinit var context: Context
    private lateinit var attachmentUri: Uri

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        attachmentUri = MediaScratchFileProvider.buildMediaScratchSpaceUri(IMAGE_EXTENSION)
        context.contentResolver.openOutputStream(attachmentUri).use { out ->
            val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.BLUE)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, checkNotNull(out))
        }
        val attachmentFile = checkNotNull(MediaScratchFileProvider.getFileFromUri(attachmentUri))
        ExifInterface(attachmentFile.absolutePath).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "37/1,25/1,19/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "122/1,5/1,4/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
            setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString(),
            )
            saveAttributes()
        }

        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                NotificationChannelUtil.INCOMING_MESSAGES,
                "Conversations",
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    @After
    fun tearDown() {
        context.contentResolver.delete(attachmentUri, null, null)
        NotificationImageProvider.listImageFiles().forEach { it.delete() }
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
    }

    @Test
    fun createStyledMessage_withImageAttachment_attachesAnOpenableUri() {
        val message = messageLineInfo(attachmentUri).createStyledMessage(SENDER)

        val dataUri = message.dataUri
        assertNotNull("no image was attached to the notification", dataUri)

        val bytes = try {
            context.contentResolver.openInputStream(checkNotNull(dataUri)).use {
                checkNotNull(it).readBytes()
            }
        } catch (e: FileNotFoundException) {
            throw AssertionError(
                "BUG-015: the notification carries $dataUri, which cannot be opened, so the " +
                    "image never renders in the shade",
                e,
            )
        }

        assertTrue("the notification image $dataUri opened but is empty", bytes.isNotEmpty())
    }

    @Test
    fun createStyledMessage_withImageAttachment_stripsTheSendersMetadata() {
        assertNotNull(
            "the attachment was seeded without the gps metadata this test looks for",
            exifOf(attachmentUri).getAttribute(ExifInterface.TAG_GPS_LATITUDE),
        )

        val dataUri =
            checkNotNull(messageLineInfo(attachmentUri).createStyledMessage(SENDER).dataUri)

        assertNotEquals("the sender's own file was attached", attachmentUri, dataUri)
        assertTrue(
            "no image was attached, so the metadata check below proves nothing",
            boundsOf(dataUri).outWidth > 0,
        )
        assertNull(
            "the notification image still carries the sender's gps metadata",
            exifOf(dataUri).getAttribute(ExifInterface.TAG_GPS_LATITUDE),
        )
    }

    @Test
    fun createStyledMessage_withRotatedImageAttachment_bakesTheOrientationIn() {
        val dataUri = messageLineInfo(attachmentUri).createStyledMessage(SENDER).dataUri

        val bounds = boundsOf(checkNotNull(dataUri))

        assertEquals("the exif rotation was dropped, not applied", IMAGE_HEIGHT, bounds.outWidth)
        assertEquals("the exif rotation was dropped, not applied", IMAGE_WIDTH, bounds.outHeight)
    }

    /**
     * The platform grant-checks every uri a notification carries and, at targetSdk >= P, rethrows
     * the [SecurityException] instead of dropping the uri. An image we attach has to survive that.
     */
    @Test
    fun postNotification_withImageAttachment_posts() {
        val posted = post(messageLineInfo(attachmentUri).createStyledMessage(SENDER))

        assertTrue("the platform refused to grant access to the attached image", posted)
    }

    /**
     * ...and an image it will not grant has to be dropped rather than thrown out of `notify()`,
     * which would otherwise kill the process. `content://call_log` is read-guarded by a permission
     * we never hold, so it stands in for an mms part we can no longer read after losing the sms
     * role -- the case [BugleNotifications.updateWithInlineReply] hits when it re-posts a
     * notification that has been sitting in the shade since before the role changed hands.
     */
    @Test
    fun postNotification_withUngrantableAttachment_dropsItInsteadOfThrowing() {
        val message = MessagingStyle.Message("Check out this photo!", 1L, SENDER)
            .setData(ContentType.IMAGE_JPEG, UNGRANTABLE_URI)

        val posted = post(message)

        assertFalse("$UNGRANTABLE_URI was grantable after all, so this proves nothing", posted)
    }

    @Test
    fun createStyledMessage_withUndecodableAttachment_leavesNoFileBehind() {
        val brokenUri = MediaScratchFileProvider.buildMediaScratchSpaceUri(IMAGE_EXTENSION)
        context.contentResolver.openOutputStream(brokenUri).use {
            checkNotNull(it).write(NOT_AN_IMAGE.toByteArray())
        }

        val dataUri = messageLineInfo(brokenUri).createStyledMessage(SENDER).dataUri
        context.contentResolver.delete(brokenUri, null, null)

        assertNull("an attachment that cannot be decoded was attached anyway", dataUri)
        assertEquals(
            "the failed encode left its placeholder behind",
            0,
            NotificationImageProvider.listImageFiles().size,
        )
    }

    @Test
    fun createStyledMessage_calledTwice_doesNotReuseTheSameFile() {
        val first = messageLineInfo(attachmentUri).createStyledMessage(SENDER).dataUri
        val second = messageLineInfo(attachmentUri).createStyledMessage(SENDER).dataUri

        assertNotEquals("the same file was handed to two notifications", first, second)
    }

    /**
     * The sweep runs after every notification pass, so it has to tell an image the shade is still
     * showing from one left behind by a notification that has since been replaced or cancelled.
     */
    @Test
    fun sweepNotificationImages_keepsPostedImagesAndDeletesOrphans() {
        val orphan = imageFileOf(NotificationImageProvider.buildNotificationImageUri())
        val message = messageLineInfo(attachmentUri).createStyledMessage(SENDER)
        val posted = imageFileOf(message.dataUri)
        assertTrue("the platform refused to grant access to the attached image", post(message))
        awaitPostedNotification()
        val earlier = System.currentTimeMillis() - PREVIOUS_PASS_AGE_MILLIS
        assertTrue(orphan.setLastModified(earlier))
        assertTrue(posted.setLastModified(earlier))

        BugleNotifications.sweepNotificationImages(System.currentTimeMillis())

        assertTrue("the image the shade is showing was swept", posted.exists())
        assertFalse("the orphaned image survived the sweep", orphan.exists())
    }

    @Test
    fun sweepNotificationImages_keepsImagesFromTheCurrentPass() {
        val passStart = System.currentTimeMillis()
        val styled = messageLineInfo(attachmentUri).createStyledMessage(SENDER)
        val image = imageFileOf(styled.dataUri)

        BugleNotifications.sweepNotificationImages(passStart)

        assertTrue(
            "an image written during the pass was swept before it was posted",
            image.exists(),
        )
    }

    private fun imageFileOf(uri: Uri?): File {
        return checkNotNull(NotificationImageProvider.getFileFromUri(uri))
    }

    private fun awaitPostedNotification() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val deadline = System.currentTimeMillis() + POST_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            if (manager.activeNotifications.any { it.tag == NOTIFICATION_TAG }) {
                return
            }
            Thread.sleep(POST_POLL_MILLIS)
        }
        throw AssertionError("the notification never reached the shade")
    }

    private fun boundsOf(uri: Uri): BitmapFactory.Options {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        return bounds
    }

    private fun exifOf(uri: Uri): ExifInterface {
        return context.contentResolver.openInputStream(uri).use {
            ExifInterface(checkNotNull(it))
        }
    }

    private fun post(message: MessagingStyle.Message): Boolean {
        val style = MessagingStyle(SELF).also { it.addMessage(message) }
        val notification =
            NotificationCompat.Builder(context, NotificationChannelUtil.INCOMING_MESSAGES)
                .setSmallIcon(R.drawable.ic_sms_light)
                .setStyle(style)
                .build()

        return BugleNotifications.postNotification(
            NotificationManagerCompat.from(context),
            NOTIFICATION_TAG,
            NOTIFICATION_ID,
            notification,
        )
    }

    private fun messageLineInfo(uri: Uri): MessageNotificationState.MessageLineInfo {
        return MessageNotificationState.MessageLineInfo(
            // authorId, authorFullName, authorFirstName
            "author",
            "Sender",
            "Sender",
            // text, attachmentUri, attachmentType
            "Check out this photo!",
            uri,
            ContentType.IMAGE_JPEG,
            // isManualDownloadNeeded, avatarUri, messageId, timestamp, contactUriString
            false,
            null,
            "1",
            1L,
            null,
        )
    }

    private companion object {
        const val IMAGE_WIDTH = 8
        const val IMAGE_HEIGHT = 16
        const val IMAGE_EXTENSION = "jpg"
        const val NOT_AN_IMAGE = "this is not an image"
        const val PREVIOUS_PASS_AGE_MILLIS = 60_000L
        const val POST_TIMEOUT_MILLIS = 5_000L
        const val POST_POLL_MILLIS = 50L
        const val NOTIFICATION_ID = 0x7103
        const val NOTIFICATION_TAG = "BUG-015"
        val UNGRANTABLE_URI: Uri = Uri.parse("content://call_log/calls/1")
        val SELF: Person = Person.Builder().setName("Me").build()
        val SENDER: Person = Person.Builder().setName("Sender").setKey("author").build()
    }
}
