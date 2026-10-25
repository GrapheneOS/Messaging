package com.android.messaging.ui.common.components.participant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import com.android.messaging.ui.core.appColorScheme
import com.android.messaging.util.AvatarUriUtil
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ParticipantAvatarBitmapTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun createParticipantAvatarBitmap_withoutPhoto_drawsSeededLetterTile() {
        val bitmap = createAvatar(displayName = "Alice")
        val fallbackColors = seededFallbackColors(isDarkTheme = false)

        assertEquals(AndroidColor.TRANSPARENT, bitmap[0, 0])
        assertEquals(fallbackColors.background.toArgb(), bitmap.backgroundPixel())
        assertTrue(bitmap.containsColor(color = fallbackColors.content))
    }

    @Test
    @Config(qualifiers = "night")
    fun createParticipantAvatarBitmap_inDarkTheme_usesDarkSeededColors() {
        val bitmap = createAvatar(displayName = "Alice")

        assertEquals(
            seededFallbackColors(isDarkTheme = true).background.toArgb(),
            bitmap.backgroundPixel(),
        )
    }

    @Test
    fun createParticipantAvatarBitmap_withPhoneNumberName_drawsPersonIcon() {
        val phoneNumberAvatar = createAvatar(displayName = "+31 6 1234 5678")

        assertTrue(phoneNumberAvatar.sameAs(createAvatar(displayName = null)))
        assertFalse(phoneNumberAvatar.sameAs(createAvatar(displayName = "Alice")))
    }

    @Test
    fun createParticipantAvatarBitmap_withPhoto_drawsCircleCroppedPhoto() {
        val photoUri = writePhoto(color = AndroidColor.RED)

        val bitmap = createAvatar(displayName = "Alice", photoUri = photoUri)

        assertEquals(AndroidColor.TRANSPARENT, bitmap[0, 0])
        assertEquals(AndroidColor.RED, bitmap[bitmap.width / 2, bitmap.height / 2])
    }

    @Test
    fun createParticipantAvatarBitmap_withUnreadablePhoto_drawsLetterTile() {
        val missingPhotoUri = Uri.fromFile(File(context.cacheDir, "missing-avatar.png"))

        val bitmap = createAvatar(displayName = "Alice", photoUri = missingPhotoUri)

        assertTrue(bitmap.sameAs(createAvatar(displayName = "Alice")))
    }

    @Test
    fun createGroupAvatarBitmap_usesPrimaryContainer() {
        val bitmap = createGroupAvatarBitmap(context = context)
        val colorScheme = appColorScheme(context = context, isDarkTheme = false)

        assertEquals(AndroidColor.TRANSPARENT, bitmap[0, 0])
        assertEquals(colorScheme.primaryContainer.toArgb(), bitmap.backgroundPixel())
        assertTrue(bitmap.containsColor(color = colorScheme.onPrimaryContainer))
    }

    private fun createAvatar(displayName: String?, photoUri: Uri? = null): Bitmap {
        return createParticipantAvatarBitmap(
            context = context,
            avatarUri = AvatarUriUtil.createAvatarUri(
                photoUri,
                displayName,
                NORMALIZED_DESTINATION,
                null,
            ),
            displayName = displayName,
            normalizedDestination = NORMALIZED_DESTINATION,
        )
    }

    private fun seededFallbackColors(isDarkTheme: Boolean): ParticipantAvatarFallbackColors {
        return participantAvatarFallbackColors(
            colorSeedCode = NORMALIZED_DESTINATION,
            isDarkTheme = isDarkTheme,
        )
    }

    private fun writePhoto(color: Int): Uri {
        val photo = createBitmap(width = PHOTO_SIZE_PX, height = PHOTO_SIZE_PX).apply {
            eraseColor(color)
        }
        val photoFile = File(context.cacheDir, "avatar.png")
        photoFile.outputStream().use { output ->
            photo.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return Uri.fromFile(photoFile)
    }

    private fun Bitmap.backgroundPixel(): Int {
        return this[width / 2, height / 8]
    }

    private fun Bitmap.containsColor(color: Color): Boolean {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return color.toArgb() in pixels
    }

    private companion object {
        private const val NORMALIZED_DESTINATION = "+31612345678"
        private const val PHOTO_SIZE_PX = 64
    }
}
