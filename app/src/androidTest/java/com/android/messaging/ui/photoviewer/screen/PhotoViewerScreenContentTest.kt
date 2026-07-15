package com.android.messaging.ui.photoviewer.screen

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.android.messaging.R
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_CLOSE_BUTTON_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_DETAILS_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_FORWARD_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_METADATA_RECEIVED_TIMESTAMP_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_METADATA_SENDER_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_METADATA_SHEET_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_PAGER_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_PAGE_INDICATOR_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_SAVE_BUTTON_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_SHARE_BUTTON_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_TIMESTAMP_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_TITLE_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_ZOOMABLE_PHOTO_TEST_TAG
import com.android.messaging.ui.photoviewer.component.PhotoViewerTopBar
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.model.PhotoViewerSourceBounds
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerLoadState
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerUiState
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val TEST_WAIT_TIMEOUT_MILLIS = 5_000L

internal class PhotoViewerScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun initialRender_showsCurrentItemChrome() {
        setScreenContent()

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_TITLE_TEST_TAG)
            .assertTextContains(value = FIRST_SENDER)
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_CLOSE_BUTTON_TEST_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_SAVE_BUTTON_TEST_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_SHARE_BUTTON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun participantTitle_whenCurrentItemIsIncoming_showsSenderInTopBarAndDetails() {
        setScreenContent()

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_TITLE_TEST_TAG)
            .assertTextContains(value = FIRST_SENDER)
        openMetadataSheet()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_METADATA_SENDER_TEST_TAG)
            .assertTextContains(value = FIRST_SENDER)
    }

    @Test
    fun participantTitle_whenCurrentItemIsOutgoing_showsSelfInTopBarAndDetails() {
        setScreenContent(
            uiState = loadedPhotoViewerUiState(firstItemIsIncoming = false),
        )

        val selfTitle = string(resId = R.string.unknown_self_participant)
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_TITLE_TEST_TAG)
            .assertTextContains(value = selfTitle)
        openMetadataSheet()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_METADATA_SENDER_TEST_TAG)
            .assertTextContains(value = selfTitle)
    }

    @Test
    fun pageIndicator_whenMultipleItemsInCarousel_isDisplayed() {
        setScreenContent()

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_PAGE_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun pageIndicator_whenSingleItem_doesNotExist() {
        setScreenContent(
            uiState = loadedPhotoViewerUiState(itemCount = 1),
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_PAGE_INDICATOR_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun pageIndicator_whenImmersive_doesNotExist() {
        setScreenContent(
            uiState = loadedPhotoViewerUiState(displayMode = PhotoViewerDisplayMode.Immersive),
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_PAGE_INDICATOR_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun pageIndicator_whenClosing_doesNotExist() {
        setScreenContent(
            uiState = loadedPhotoViewerUiState(isClosing = true),
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_PAGE_INDICATOR_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun topBar_withHorizontalNavigationInsets_keepsEdgeActionsInsideSafeRegion() {
        val containerWidth = 640.dp
        val navigationInset = 72.dp

        composeRule.setContent {
            AppTheme {
                Box(
                    modifier = Modifier.size(
                        width = containerWidth,
                        height = 360.dp,
                    ),
                ) {
                    PhotoViewerTopBar(
                        isVisible = true,
                        item = photoViewerItem(index = 1, senderName = FIRST_SENDER),
                        actionsEnabled = true,
                        navigationBarInsets = WindowInsets(
                            left = navigationInset,
                            right = navigationInset,
                        ),
                        onMetadataClick = {},
                        onCloseClick = {},
                        onForwardClick = {},
                        onSaveClick = {},
                        onShareClick = {},
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val containerWidthPx = with(composeRule.density) { containerWidth.toPx() }
        val navigationInsetPx = with(composeRule.density) { navigationInset.toPx() }
        val closeButtonBounds = composeRule
            .onNodeWithTag(testTag = PHOTO_VIEWER_CLOSE_BUTTON_TEST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val overflowButtonBounds = composeRule
            .onNodeWithTag(testTag = PHOTO_VIEWER_OVERFLOW_BUTTON_TEST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(closeButtonBounds.left >= navigationInsetPx)
        assertTrue(overflowButtonBounds.right <= containerWidthPx - navigationInsetPx)
    }

    @Test
    fun pager_whenRootWidthShrinks_keepsContentPaddingNonNegative() {
        val containerWidth = mutableStateOf(value = 900.dp)

        composeRule.setContent {
            AppTheme {
                Box(
                    modifier = Modifier.size(
                        width = containerWidth.value,
                        height = 500.dp,
                    ),
                ) {
                    PhotoViewerScreenContent(
                        launchRequest = launchRequest,
                        uiState = loadedPhotoViewerUiState(),
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
        }

        composeRule.waitForIdle()
        composeRule.runOnIdle {
            containerWidth.value = 320.dp
        }

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_PAGER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun swipeLeft_setsNextPage() {
        val currentPage = AtomicInteger()
        setScreenContent(
            onPageSettled = { page ->
                currentPage.set(page)
            },
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_PAGER_TEST_TAG)
            .performTouchInput {
                swipeLeft()
            }

        composeRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            currentPage.get() == 1
        }
        composeRule.onNodeWithText(text = SECOND_SENDER).assertIsDisplayed()
    }

    @Test
    fun overflowDetailsClick_showsMetadataSheet() {
        setScreenContent()

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_OVERFLOW_BUTTON_TEST_TAG)
            .performClick()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_DETAILS_MENU_ITEM_TEST_TAG)
            .performClick()

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_METADATA_SHEET_TEST_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithText(text = string(resId = R.string.message_details_from_label))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text = string(resId = R.string.message_details_type_label))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text = IMAGE_JPEG).assertIsDisplayed()
    }

    @Test
    fun timestamps_whenCurrentItemIsNotDraft_areDisplayed() {
        setScreenContent()

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_TIMESTAMP_TEST_TAG)
            .assertIsDisplayed()
        openMetadataSheet()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_METADATA_RECEIVED_TIMESTAMP_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun timestamps_whenCurrentItemIsDraft_doNotExist() {
        setScreenContent(
            uiState = loadedPhotoViewerUiState(firstItemIsDraft = true),
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_TIMESTAMP_TEST_TAG)
            .assertDoesNotExist()
        openMetadataSheet()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_METADATA_RECEIVED_TIMESTAMP_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun chromeActions_emitCallbacks() {
        val closeClickCount = AtomicInteger()
        val forwardClickCount = AtomicInteger()
        val saveClickCount = AtomicInteger()
        val shareClickCount = AtomicInteger()
        setScreenContent(
            onCloseClick = {
                closeClickCount.incrementAndGet()
            },
            onForwardClick = {
                forwardClickCount.incrementAndGet()
            },
            onSaveClick = {
                saveClickCount.incrementAndGet()
            },
            onShareClick = {
                shareClickCount.incrementAndGet()
            },
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_SAVE_BUTTON_TEST_TAG)
            .performClick()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_SHARE_BUTTON_TEST_TAG)
            .performClick()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_OVERFLOW_BUTTON_TEST_TAG)
            .performClick()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_FORWARD_MENU_ITEM_TEST_TAG)
            .performClick()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_CLOSE_BUTTON_TEST_TAG)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, saveClickCount.get())
            assertEquals(1, shareClickCount.get())
            assertEquals(1, forwardClickCount.get())
            assertEquals(1, closeClickCount.get())
        }
    }

    @Test
    fun chromeActions_whenCurrentItemDisallowsActions_areDisabled() {
        val uiState = loadedPhotoViewerUiState(
            firstItemCanUseActions = false,
        )
        setScreenContent(uiState = uiState)

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_SAVE_BUTTON_TEST_TAG)
            .assertIsNotEnabled()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_SHARE_BUTTON_TEST_TAG)
            .assertIsNotEnabled()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_OVERFLOW_BUTTON_TEST_TAG)
            .performClick()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_FORWARD_MENU_ITEM_TEST_TAG)
            .assertIsNotEnabled()
    }

    @Test
    fun doubleTapPhoto_entersImmersiveMode() {
        val enterImmersiveCount = AtomicInteger()
        val uiState = setScreenContent(
            onEnterImmersiveMode = {
                enterImmersiveCount.incrementAndGet()
            },
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_ZOOMABLE_PHOTO_TEST_TAG)
            .performTouchInput {
                doubleClick()
            }

        composeRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            enterImmersiveCount.get() == 1 &&
                uiState.value.displayMode == PhotoViewerDisplayMode.Immersive
        }
    }

    @Test
    fun simultaneousPinchOutPhoto_entersImmersiveMode() {
        val enterImmersiveCount = AtomicInteger()
        val uiState = setScreenContent(
            onEnterImmersiveMode = {
                enterImmersiveCount.incrementAndGet()
            },
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_ZOOMABLE_PHOTO_TEST_TAG)
            .performTouchInput {
                pinchOutPhoto(moveFirstPointerBeforeSecondDown = false)
            }

        composeRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            enterImmersiveCount.get() >= 1 &&
                uiState.value.displayMode == PhotoViewerDisplayMode.Immersive
        }
    }

    @Test
    fun staggeredPinchOutPhoto_entersImmersiveMode() {
        val enterImmersiveCount = AtomicInteger()
        val uiState = setScreenContent(
            onEnterImmersiveMode = {
                enterImmersiveCount.incrementAndGet()
            },
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_ZOOMABLE_PHOTO_TEST_TAG)
            .performTouchInput {
                pinchOutPhoto(moveFirstPointerBeforeSecondDown = true)
            }

        composeRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            enterImmersiveCount.get() >= 1 &&
                uiState.value.displayMode == PhotoViewerDisplayMode.Immersive
        }
    }

    @Test
    fun panZoomedPhoto_doesNotDismissViewer() {
        val closeClickCount = AtomicInteger()
        val enterImmersiveCount = AtomicInteger()
        val uiState = setScreenContent(
            onEnterImmersiveMode = {
                enterImmersiveCount.incrementAndGet()
            },
            onCloseClick = {
                closeClickCount.incrementAndGet()
            },
        )

        val photoNode = composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_ZOOMABLE_PHOTO_TEST_TAG)
        photoNode.performTouchInput {
            doubleClick()
        }
        composeRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            enterImmersiveCount.get() == 1 &&
                uiState.value.displayMode == PhotoViewerDisplayMode.Immersive
        }

        photoNode.performTouchInput {
            swipe(
                start = center,
                end = Offset(x = center.x + 120f, y = center.y + 80f),
                durationMillis = 200,
            )
        }

        composeRule.runOnIdle {
            assertEquals(0, closeClickCount.get())
        }
    }

    @Test
    fun swipeDownPhotoPastDismissThreshold_emitsCloseCallback() {
        val closeClickCount = AtomicInteger()
        setScreenContent(
            onCloseClick = {
                closeClickCount.incrementAndGet()
            },
        )

        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_ZOOMABLE_PHOTO_TEST_TAG)
            .performTouchInput {
                swipe(
                    start = center,
                    end = Offset(x = center.x, y = bottom - 1f),
                    durationMillis = 100,
                )
            }

        composeRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            closeClickCount.get() == 1
        }
    }

    @Test
    fun closing_whenRootSizeChangesAfterAnimation_callsCloseAnimationFinishedOnce() {
        val closeAnimationFinishedCount = AtomicInteger()
        val containerHeight = mutableStateOf(value = 640.dp)
        val uiState = mutableStateOf(value = loadedPhotoViewerUiState())
        composeRule.mainClock.autoAdvance = false

        try {
            composeRule.setContent {
                AppTheme {
                    Box(
                        modifier = Modifier.size(
                            width = 320.dp,
                            height = containerHeight.value,
                        ),
                    ) {
                        PhotoViewerScreenContent(
                            launchRequest = launchRequest,
                            uiState = uiState.value,
                            onPageSettled = {},
                            onToggleDisplayMode = {},
                            onEnterImmersiveMode = {},
                            onMetadataClick = {},
                            onMetadataDismissed = {},
                            onCloseClick = {},
                            onCloseAnimationFinished = {
                                closeAnimationFinishedCount.incrementAndGet()
                            },
                            onForwardClick = {},
                            onSaveClick = {},
                            onShareClick = {},
                        )
                    }
                }
            }

            composeRule.mainClock.advanceTimeBy(milliseconds = 100)
            composeRule.runOnIdle {
                uiState.value = uiState.value.copy(isClosing = true)
            }
            composeRule.mainClock.advanceTimeBy(milliseconds = 1_000)
            composeRule.runOnIdle {
                assertEquals(1, closeAnimationFinishedCount.get())
                containerHeight.value = 480.dp
            }
            composeRule.mainClock.advanceTimeBy(milliseconds = 1_000)
            composeRule.runOnIdle {
                assertEquals(1, closeAnimationFinishedCount.get())
            }
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun setScreenContent(
        uiState: PhotoViewerUiState = loadedPhotoViewerUiState(),
        onPageSettled: (Int) -> Unit = {},
        onToggleDisplayMode: () -> Unit = {},
        onEnterImmersiveMode: () -> Unit = {},
        onCloseClick: () -> Unit = {},
        onForwardClick: () -> Unit = {},
        onSaveClick: () -> Unit = {},
        onShareClick: () -> Unit = {},
    ): MutableState<PhotoViewerUiState> {
        val uiState = mutableStateOf(value = uiState)

        composeRule.setContent {
            AppTheme {
                PhotoViewerScreenContent(
                    launchRequest = launchRequest,
                    uiState = uiState.value,
                    onPageSettled = { page ->
                        uiState.value = uiState.value.copy(currentPage = page)
                        onPageSettled(page)
                    },
                    onToggleDisplayMode = {
                        uiState.value = uiState.value.copy(
                            displayMode = nextDisplayMode(
                                displayMode = uiState.value.displayMode,
                            ),
                        )
                        onToggleDisplayMode()
                    },
                    onEnterImmersiveMode = {
                        uiState.value = uiState.value.copy(
                            displayMode = PhotoViewerDisplayMode.Immersive,
                        )
                        onEnterImmersiveMode()
                    },
                    onMetadataClick = {
                        uiState.value = uiState.value.copy(isMetadataSheetVisible = true)
                    },
                    onMetadataDismissed = {
                        uiState.value = uiState.value.copy(isMetadataSheetVisible = false)
                    },
                    onCloseClick = onCloseClick,
                    onCloseAnimationFinished = {},
                    onForwardClick = onForwardClick,
                    onSaveClick = onSaveClick,
                    onShareClick = onShareClick,
                )
            }
        }

        return uiState
    }

    private fun openMetadataSheet() {
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_OVERFLOW_BUTTON_TEST_TAG)
            .performClick()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_DETAILS_MENU_ITEM_TEST_TAG)
            .performClick()
        composeRule.onNodeWithTag(testTag = PHOTO_VIEWER_METADATA_SHEET_TEST_TAG)
            .assertIsDisplayed()
    }

    private fun loadedPhotoViewerUiState(
        firstItemCanUseActions: Boolean = true,
        firstItemIsIncoming: Boolean = true,
        firstItemIsDraft: Boolean = false,
        itemCount: Int = 2,
        displayMode: PhotoViewerDisplayMode = PhotoViewerDisplayMode.Carousel,
        isClosing: Boolean = false,
    ): PhotoViewerUiState {
        val firstItem = photoViewerItem(
            index = 1,
            senderName = FIRST_SENDER,
            canUseActions = firstItemCanUseActions,
            isIncoming = firstItemIsIncoming,
            isDraft = firstItemIsDraft,
        )
        val items = when (itemCount) {
            1 -> persistentListOf(firstItem)
            else -> persistentListOf(
                firstItem,
                photoViewerItem(index = 2, senderName = SECOND_SENDER),
            )
        }

        return PhotoViewerUiState(
            items = items,
            loadState = PhotoViewerLoadState.Loaded,
            displayMode = displayMode,
            isClosing = isClosing,
        )
    }

    private fun photoViewerItem(
        index: Int,
        senderName: String,
        canUseActions: Boolean = true,
        isIncoming: Boolean = true,
        isDraft: Boolean = false,
    ): PhotoViewerItem {
        return PhotoViewerItem(
            contentUri = photoViewerImageUri(),
            contentType = IMAGE_JPEG,
            isIncoming = isIncoming,
            senderName = senderName,
            senderDestination = "+1555000$index",
            receivedTimestampMillis = 1_735_689_600_000L + index,
            isDraft = isDraft,
            canUseActions = canUseActions,
        )
    }

    private fun photoViewerImageUri(): Uri {
        return Uri.parse(
            "android.resource://${context.packageName}/${R.drawable.ic_preview_play}",
        )
    }

    private fun string(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    private fun nextDisplayMode(displayMode: PhotoViewerDisplayMode): PhotoViewerDisplayMode {
        return when (displayMode) {
            PhotoViewerDisplayMode.Carousel -> PhotoViewerDisplayMode.Immersive
            PhotoViewerDisplayMode.Immersive -> PhotoViewerDisplayMode.Carousel
        }
    }

    private fun TouchInjectionScope.pinchOutPhoto(moveFirstPointerBeforeSecondDown: Boolean) {
        val firstPointerStart = Offset(
            x = center.x - 40f,
            y = center.y,
        )
        val secondPointerStart = Offset(
            x = center.x + 40f,
            y = center.y,
        )

        down(pointerId = 0, position = firstPointerStart)
        if (moveFirstPointerBeforeSecondDown) {
            moveBy(
                pointerId = 0,
                delta = Offset(x = 0f, y = viewConfiguration.touchSlop + 8f),
                delayMillis = 40,
            )
        }
        down(pointerId = 1, position = secondPointerStart)

        repeat(times = 8) {
            updatePointerBy(pointerId = 0, delta = Offset(x = -32f, y = -8f))
            updatePointerBy(pointerId = 1, delta = Offset(x = 32f, y = 8f))
            move(delayMillis = 16)
        }

        up(pointerId = 0)
        up(pointerId = 1)
    }

    private companion object {
        const val FIRST_SENDER = "Ada Lovelace"
        const val IMAGE_JPEG = "image/jpeg"
        const val SECOND_SENDER = "Grace Hopper"

        val launchRequest = PhotoViewerLaunchRequest(
            initialPhotoUri = "content://example/content/1",
            photosUri = "content://example/photos",
            sourceBounds = PhotoViewerSourceBounds(),
        )
    }
}
