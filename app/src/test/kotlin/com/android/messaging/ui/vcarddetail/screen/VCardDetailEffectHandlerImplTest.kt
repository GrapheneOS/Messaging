package com.android.messaging.ui.vcarddetail.screen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import com.android.messaging.data.vcarddetail.model.VCardFieldAction
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailScreenEffect as Effect
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VCardDetailEffectHandlerImplTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val uiIntents = mockk<UIIntents>(relaxed = true)
    private val clipboardManager = mockk<ClipboardManager>(relaxed = true)

    private val handler = VCardDetailEffectHandlerImpl(
        activity = activity,
        clipboardManager = clipboardManager,
    )

    @Before
    fun setUp() {
        mockkStatic(UIIntents::class)
        every { UIIntents.get() } returns uiIntents
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun handle_close_finishesActivity() {
        handler.handle(Effect.Close)

        verify { activity.finish() }
    }

    @Test
    fun handle_launchSaveToContacts_launchesSaveActivityWithScratchUri() {
        val uriSlot = slot<Uri>()
        every {
            uiIntents.launchSaveVCardToContactsActivity(activity, capture(uriSlot))
        } just runs

        handler.handle(Effect.LaunchSaveToContacts("content://scratch/vcard"))

        assertEquals("content://scratch/vcard", uriSlot.captured.toString())
    }

    @Test
    fun handle_copyToClipboard_setsPrimaryClipWithText() {
        val clipSlot = slot<ClipData>()
        every { clipboardManager.setPrimaryClip(capture(clipSlot)) } just runs

        handler.handle(Effect.CopyToClipboard("+1 555 0001"))

        assertEquals("+1 555 0001", clipSlot.captured.getItemAt(0).text.toString())
    }

    @Test
    fun handle_openDial_startsDialIntent() {
        val intent = captureStartedIntent(VCardFieldAction.Dial("+15550001"))

        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("tel:+15550001", intent.data.toString())
    }

    @Test
    fun handle_openEmail_startsSendtoIntentWithAddress() {
        val intent = captureStartedIntent(VCardFieldAction.Email("example@example.com"))

        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto:", intent.data.toString())
        assertArrayEquals(
            arrayOf("example@example.com"),
            intent.getStringArrayExtra(Intent.EXTRA_EMAIL),
        )
    }

    @Test
    fun handle_openMap_startsViewIntentWithEncodedGeoQuery() {
        val intent = captureStartedIntent(VCardFieldAction.OpenMap("Main Street 1"))

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("geo:0,0?q=Main%20Street%201", intent.data.toString())
    }

    @Test
    fun handle_openUrl_startsViewIntentWithUrl() {
        val intent = captureStartedIntent(VCardFieldAction.OpenUrl("https://example.com"))

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://example.com", intent.data.toString())
    }

    @Test
    fun handle_openNoneAction_doesNotStartActivity() {
        handler.handle(Effect.OpenFieldAction(VCardFieldAction.None))

        verify(exactly = 0) { activity.startActivity(any()) }
    }

    @Test
    fun handle_openField_whenNoActivityFound_isSwallowed() {
        every { activity.startActivity(any()) } throws ActivityNotFoundException()

        handler.handle(Effect.OpenFieldAction(VCardFieldAction.Dial("+15550001")))
    }

    private fun captureStartedIntent(action: VCardFieldAction): Intent {
        val intentSlot = slot<Intent>()
        every { activity.startActivity(capture(intentSlot)) } just runs

        handler.handle(Effect.OpenFieldAction(action))

        return intentSlot.captured
    }
}
