package com.android.messaging.ui.photoviewer.screen

import android.net.Uri
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_PAGE_INDICATOR_TEST_TAG
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.model.PhotoViewerSourceBounds
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerLoadState
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h360dp-land")
internal class PhotoViewerPageIndicatorInsetsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun landscapeCutoutOnOneSide_keepsPageIndicatorCentred() {
        val view = setPhotoViewerContent()

        dispatchInsets(
            view = view,
            displayCutout = LEFT_CUTOUT,
            navigationBars = BOTTOM_GESTURE_BAR,
        )

        assertEquals(view.width / 2f, pageIndicatorBounds().center.x, PIXEL_TOLERANCE)
    }

    @Test
    fun landscapeNavigationBarOnOneSide_keepsPageIndicatorCentred() {
        val view = setPhotoViewerContent()

        dispatchInsets(
            view = view,
            displayCutout = LEFT_CUTOUT,
            navigationBars = RIGHT_BUTTON_BAR,
        )

        assertEquals(view.width / 2f, pageIndicatorBounds().center.x, PIXEL_TOLERANCE)
    }

    @Test
    fun bottomNavigationBar_keepsPageIndicatorAboveIt() {
        val view = setPhotoViewerContent()

        dispatchInsets(
            view = view,
            displayCutout = Insets.NONE,
            navigationBars = BOTTOM_GESTURE_BAR,
        )

        val bottomPadding = with(composeTestRule.density) { 24.dp.toPx() }

        assertEquals(
            view.height - BOTTOM_GESTURE_BAR.bottom - bottomPadding,
            pageIndicatorBounds().bottom,
            PIXEL_TOLERANCE,
        )
    }

    private fun setPhotoViewerContent(): View {
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            AppTheme {
                PhotoViewerScreenContent(
                    launchRequest = launchRequest,
                    uiState = PhotoViewerUiState(
                        items = persistentListOf(
                            photoViewerItem(index = 1),
                            photoViewerItem(index = 2),
                            photoViewerItem(index = 3),
                        ),
                        loadState = PhotoViewerLoadState.Loaded,
                    ),
                    onPageSettled = {},
                    onToggleDisplayMode = {},
                    onEnterImmersiveMode = {},
                    onMetadataClick = {},
                    onMetadataDismissed = {},
                    onCloseClick = {},
                    onCloseAnimationFinished = {},
                    onForwardClick = {},
                    onSaveClick = {},
                    onShareClick = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        return view
    }

    private fun dispatchInsets(
        view: View,
        displayCutout: Insets,
        navigationBars: Insets,
    ) {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.displayCutout(), displayCutout)
            .setInsets(WindowInsetsCompat.Type.navigationBars(), navigationBars)
            .build()
        composeTestRule.runOnIdle {
            ViewCompat.dispatchApplyWindowInsets(view, insets)
        }
        composeTestRule.waitForIdle()
    }

    private fun pageIndicatorBounds(): Rect {
        return composeTestRule
            .onNodeWithTag(testTag = PHOTO_VIEWER_PAGE_INDICATOR_TEST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
    }

    private fun photoViewerItem(index: Int): PhotoViewerItem {
        return PhotoViewerItem(
            partId = "part-$index",
            contentUri = Uri.parse(
                "android.resource://${targetContext.packageName}/${R.drawable.ic_preview_play}",
            ),
            contentType = "image/jpeg",
            isIncoming = true,
            senderName = "Sender $index",
            senderDestination = "+1555000$index",
            receivedTimestampMillis = 1_735_689_600_000L + index,
            isDraft = false,
        )
    }

    private companion object {
        private const val PIXEL_TOLERANCE = 0.5f

        private val BOTTOM_GESTURE_BAR = Insets.of(0, 0, 0, 63)
        private val LEFT_CUTOUT = Insets.of(150, 0, 0, 0)
        private val RIGHT_BUTTON_BAR = Insets.of(0, 0, 126, 0)

        private val launchRequest = PhotoViewerLaunchRequest(
            initialPhotoUri = "content://example/content/1",
            photosUri = "content://example/photos",
            sourceBounds = PhotoViewerSourceBounds(),
        )
    }
}
