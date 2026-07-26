package com.android.messaging.ui.conversation.screen.effects

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect as ComposeRect
import com.android.messaging.Factory
import com.android.messaging.R
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.screen.openAttachmentPreviewEffect
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.model.PhotoViewerSourceBounds
import com.android.messaging.util.UiUtils
import com.android.messaging.util.UriUtil
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationAttachmentPreviewEffectTest {

    private lateinit var context: Context
    private lateinit var applicationContext: Context
    private lateinit var resources: Resources
    private lateinit var factory: Factory
    private lateinit var uiIntents: UIIntents

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        applicationContext = mockk(relaxed = true)
        resources = mockk(relaxed = true)
        factory = mockk(relaxed = true)
        uiIntents = mockk(relaxed = true)
        mockkStatic(Factory::class)
        mockkStatic(UIIntents::class)
        mockkStatic(UriUtil::class)
        every { Factory.get() } returns factory
        every { factory.applicationContext } returns applicationContext
        every { applicationContext.resources } returns resources
        every { UIIntents.get() } returns uiIntents
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun openAttachmentPreviewEffect_navigatesToPhotoViewerWhenCollectionExists() {
        runTest {
            val activity = mockk<Activity>(relaxed = true)
            val launchRequests = mutableListOf<PhotoViewerLaunchRequest>()

            openAttachmentPreviewEffect(
                context = activity,
                hostBoundsState = mutableStateOf(
                    ComposeRect(left = 1.2f, top = 2.6f, right = 7.4f, bottom = 8.5f),
                ),
                effect = ConversationScreenEffect.OpenAttachmentPreview(
                    contentType = "image/png",
                    contentUri = "content://media/image/1",
                    imageCollectionUri = "content://media/images",
                ),
                onNavigateToPhotoViewer = launchRequests::add,
            )

            assertEquals(
                listOf(
                    PhotoViewerLaunchRequest(
                        initialPhotoUri = "content://media/image/1",
                        photosUri = "content://media/images",
                        sourceBounds = PhotoViewerSourceBounds(
                            left = 1,
                            top = 3,
                            right = 7,
                            bottom = 9,
                        ),
                        initialPhotoOccurrenceIndex = 0,
                    ),
                ),
                launchRequests,
            )
            verify(exactly = 0) {
                activity.startActivity(any())
            }
        }
    }

    @Test
    fun openAttachmentPreviewEffect_fallsBackToGenericImageIntentWithoutCollection() {
        runTest {
            val context = mockk<Context>(relaxed = true)
            val intentSlot = slot<Intent>()
            val launchRequests = mutableListOf<PhotoViewerLaunchRequest>()
            every { context.startActivity(capture(intentSlot)) } just runs

            openAttachmentPreviewEffect(
                context = context,
                hostBoundsState = mutableStateOf(
                    ComposeRect(left = 0f, top = 0f, right = 10f, bottom = 10f),
                ),
                effect = ConversationScreenEffect.OpenAttachmentPreview(
                    contentType = "image/png",
                    contentUri = "content://media/image/3",
                    imageCollectionUri = null,
                ),
                onNavigateToPhotoViewer = launchRequests::add,
            )

            assertEquals(Intent.ACTION_VIEW, intentSlot.captured.action)
            assertEquals(Uri.parse("content://media/image/3"), intentSlot.captured.data)
            assertEquals("image/png", intentSlot.captured.type)
            assertTrue(
                intentSlot.captured.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
            )
            assertTrue(launchRequests.isEmpty())
        }
    }

    @Test
    fun openAttachmentPreviewEffect_opensVCardViaGenericIntentWithNormalizedUri() {
        runTest {
            val fileUri = Uri.parse("file:///tmp/contact.vcf")
            val scratchUri = Uri.parse("content://scratch/contact.vcf")
            val intentSlot = slot<Intent>()
            every { UriUtil.persistContentToScratchSpace(fileUri) } returns scratchUri
            every { context.startActivity(capture(intentSlot)) } just runs

            openAttachmentPreviewEffect(
                context = context,
                hostBoundsState = mutableStateOf(
                    ComposeRect(left = 0f, top = 0f, right = 10f, bottom = 10f),
                ),
                effect = ConversationScreenEffect.OpenAttachmentPreview(
                    contentType = "text/x-vcard",
                    contentUri = fileUri.toString(),
                    imageCollectionUri = null,
                ),
                onNavigateToPhotoViewer = {},
            )

            assertEquals(Intent.ACTION_VIEW, intentSlot.captured.action)
            assertEquals(scratchUri, intentSlot.captured.data)
            assertEquals("text/x-vcard", intentSlot.captured.type)
            assertTrue(
                intentSlot.captured.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
            )
        }
    }

    @Test
    fun openAttachmentPreviewEffect_opensVideoViewerWithNormalizedUri() {
        runTest {
            val fileUri = Uri.parse("file:///tmp/video.mp4")
            every { UriUtil.persistContentToScratchSpace(fileUri) } returns null

            openAttachmentPreviewEffect(
                context = context,
                hostBoundsState = mutableStateOf(
                    ComposeRect(left = 0f, top = 0f, right = 10f, bottom = 10f),
                ),
                effect = ConversationScreenEffect.OpenAttachmentPreview(
                    contentType = "video/mp4",
                    contentUri = fileUri.toString(),
                    imageCollectionUri = null,
                ),
                onNavigateToPhotoViewer = {},
            )

            verify(exactly = 1) {
                uiIntents.launchFullScreenVideoViewer(context, fileUri)
            }
        }
    }

    @Test
    fun openAttachmentPreviewEffect_showsToastWhenGenericIntentCannotBeHandled() {
        runTest {
            mockkStatic(UiUtils::class)
            every {
                context.startActivity(any())
            } throws ActivityNotFoundException("no handler")
            every {
                UiUtils.showToastAtBottom(R.string.activity_not_found_message)
            } just runs

            openAttachmentPreviewEffect(
                context = context,
                hostBoundsState = mutableStateOf(
                    ComposeRect(left = 0f, top = 0f, right = 10f, bottom = 10f),
                ),
                effect = ConversationScreenEffect.OpenAttachmentPreview(
                    contentType = "application/pdf",
                    contentUri = "content://media/file/1",
                    imageCollectionUri = null,
                ),
                onNavigateToPhotoViewer = {},
            )

            verify(exactly = 1) {
                UiUtils.showToastAtBottom(R.string.activity_not_found_message)
            }
        }
    }
}
