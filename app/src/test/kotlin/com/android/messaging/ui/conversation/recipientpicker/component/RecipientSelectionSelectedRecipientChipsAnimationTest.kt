package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
internal class RecipientSelectionSelectedRecipientChipsAnimationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val hostWidth = mutableStateOf(value = HOST_WIDTH)
    private var chipColor = Color.Unspecified

    @Test
    fun addingRecipient_revealsTheChipAheadOfTheSlidingTrailingContent() {
        assertAddedChipRevealsAheadOfTrailingContent(layoutDirection = LayoutDirection.Ltr)
    }

    @Test
    fun addingRecipientInRtl_revealsTheChipAheadOfTheSlidingTrailingContent() {
        assertAddedChipRevealsAheadOfTrailingContent(layoutDirection = LayoutDirection.Rtl)
    }

    @Test
    fun trailingContentWrappingToTheNextLine_slidesUnderTheChips() {
        val recipients = setChangingRecipientsContent(
            initialRecipients = persistentListOf(ada, bob),
        )
        val widestChipWidth = maxOf(chipWidth(recipient = ada), chipWidth(recipient = bob))
        hostWidth.value = widestChipWidth * 3 + CHIP_SPACING * 2 + WRAP_SLACK
        composeTestRule.waitForIdle()
        var trailingContentCrossedAChip = false

        composeTestRule.mainClock.autoAdvance = false
        recipients.value = persistentListOf(ada, bob, cat)
        composeTestRule.waitForIdle()

        repeat(times = ANIMATION_FRAME_COUNT) { frame ->
            composeTestRule.mainClock.advanceTimeByFrame()
            val pixels = capturePixels()
            val trailingBounds = trailingContentBounds()

            listOf(ada, bob).forEach { recipient ->
                val chipBounds = chipBounds(recipient = recipient)
                val row = chipBounds.center.y.roundToInt()
                val trailingPixelsOverChip = columns(bounds = chipBounds).count { x ->
                    pixels[x, row].isTrailingContent()
                }

                trailingContentCrossedAChip = trailingContentCrossedAChip ||
                    trailingBounds.crossesRow(row = row, bounds = chipBounds)
                assertEquals(
                    "Frame $frame: trailing pixels drawn over ${recipient.label}",
                    0,
                    trailingPixelsOverChip,
                )
            }
        }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertTrue(trailingContentCrossedAChip)
        assertTrue(trailingContentBounds().top >= chipBounds(recipient = cat).bottom)
    }

    @Test
    fun tappingTheTrailingHintDuringAReveal_reachesTheTrailingContentNotTheNewChip() {
        val clickedRecipients = mutableListOf<SelectedRecipient>()
        var trailingContentClicks = 0
        val recipients = setChangingRecipientsContent(
            initialRecipients = persistentListOf(ada),
            onRecipientClick = { recipient -> clickedRecipients += recipient },
            onTrailingContentClick = { trailingContentClicks++ },
        )

        composeTestRule.mainClock.autoAdvance = false
        recipients.value = persistentListOf(ada, bob)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeByFrame()
        val trailingHintCenter = trailingHintCenter()

        assertTrue(chipBounds(recipient = bob).contains(offset = trailingHintCenter))
        composeTestRule.onRoot().performTouchInput { click(position = trailingHintCenter) }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertEquals(emptyList<SelectedRecipient>(), clickedRecipients)
        assertEquals(1, trailingContentClicks)
    }

    @Test
    fun chipsShownWhenTheRowFirstAppears_areDrawnWithoutAnEntrance() {
        composeTestRule.mainClock.autoAdvance = false
        setChangingRecipientsContent(initialRecipients = persistentListOf(ada, bob))
        composeTestRule.waitForIdle()

        assertChipFullyDrawn(recipient = ada)
        assertChipFullyDrawn(recipient = bob)
    }

    private fun assertAddedChipRevealsAheadOfTrailingContent(layoutDirection: LayoutDirection) {
        val recipients = setChangingRecipientsContent(
            initialRecipients = persistentListOf(ada),
            layoutDirection = layoutDirection,
        )

        composeTestRule.mainClock.autoAdvance = false
        recipients.value = persistentListOf(ada, bob)
        composeTestRule.waitForIdle()

        repeat(times = ANIMATION_FRAME_COUNT) { frame ->
            composeTestRule.mainClock.advanceTimeByFrame()
            assertFalse(
                "Frame $frame: Bob overlaps Ada",
                chipBounds(recipient = bob).overlaps(other = chipBounds(recipient = ada)),
            )
            assertChipDrawnBehindTrailingContentEdge(
                recipient = bob,
                layoutDirection = layoutDirection,
                frame = frame,
            )
        }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertChipFullyDrawn(recipient = bob, layoutDirection = layoutDirection)
        assertChipDrawnBehindTrailingContentEdge(
            recipient = bob,
            layoutDirection = layoutDirection,
            frame = ANIMATION_FRAME_COUNT,
        )
    }

    private fun assertChipDrawnBehindTrailingContentEdge(
        recipient: SelectedRecipient,
        layoutDirection: LayoutDirection,
        frame: Int,
    ) {
        val pixels = capturePixels()
        val chipBounds = chipBounds(recipient = recipient)
        val trailingBounds = trailingContentBounds()
        val chipRow = chipBounds.center.y.roundToInt()
        val trailingRow = trailingBounds.center.y.roundToInt()
        val chipPixelsPastTrailingEdge = columns(bounds = chipBounds).count { x ->
            val isPastTrailingEdge = when (layoutDirection) {
                LayoutDirection.Ltr -> x > trailingBounds.left
                LayoutDirection.Rtl -> x < trailingBounds.right - 1
            }

            isPastTrailingEdge && pixels[x, chipRow].isChip()
        }
        val visibleTrailingWidth = columns(bounds = trailingBounds).count { x ->
            pixels[x, trailingRow].isTrailingContent()
        }

        assertEquals(
            "Frame $frame: chip pixels past the trailing edge",
            0,
            chipPixelsPastTrailingEdge,
        )
        assertTrue(
            "Frame $frame: $visibleTrailingWidth trailing hint px visible",
            visibleTrailingWidth >= TRAILING_HINT_WIDTH.toPixels() - 1,
        )
    }

    private fun assertChipFullyDrawn(
        recipient: SelectedRecipient,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        val chipBounds = chipBounds(recipient = recipient)
        val endPaddingInset = CHIP_END_PADDING_PROBE_INSET.toPixels()
        val endPaddingX = when (layoutDirection) {
            LayoutDirection.Ltr -> chipBounds.right - endPaddingInset
            LayoutDirection.Rtl -> chipBounds.left + endPaddingInset
        }
        val pixel = capturePixels()[endPaddingX.roundToInt(), chipBounds.center.y.roundToInt()]

        assertEquals("${recipient.label} end padding", chipColor.toArgb(), pixel.toArgb())
    }

    private fun setChangingRecipientsContent(
        initialRecipients: ImmutableList<SelectedRecipient>,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onRecipientClick: (SelectedRecipient) -> Unit = {},
        onTrailingContentClick: () -> Unit = {},
    ): MutableState<ImmutableList<SelectedRecipient>> {
        val recipients = mutableStateOf(value = initialRecipients)

        composeTestRule.setContent {
            AppTheme {
                chipColor = MaterialTheme.colorScheme.surfaceVariant

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Box(
                        modifier = Modifier
                            .width(width = hostWidth.value)
                            .background(color = BACKGROUND_COLOR),
                    ) {
                        RecipientSelectionSelectedRecipientChips(
                            recipients = recipients.value,
                            armedRecipientDestination = null,
                            enabled = true,
                            onRecipientClick = onRecipientClick,
                            trailingContent = { modifier ->
                                Box(
                                    modifier = modifier
                                        .testTag(tag = TRAILING_CONTENT_TAG)
                                        .size(width = TRAILING_CONTENT_WIDTH, height = 32.dp)
                                        .clickable(onClick = onTrailingContentClick),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(alignment = Alignment.CenterStart)
                                            .size(width = TRAILING_HINT_WIDTH, height = 32.dp)
                                            .background(color = TRAILING_CONTENT_COLOR),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        return recipients
    }

    private fun capturePixels(): PixelMap {
        return composeTestRule.onRoot().captureToImage().toPixelMap()
    }

    private fun chipBounds(recipient: SelectedRecipient): Rect {
        return composeTestRule
            .onNodeWithContentDescription(chipDescription(recipient = recipient))
            .fetchSemanticsNode()
            .boundsInRoot
    }

    private fun chipWidth(recipient: SelectedRecipient): Dp {
        return composeTestRule
            .onNodeWithContentDescription(chipDescription(recipient = recipient))
            .getBoundsInRoot()
            .width
    }

    private fun trailingContentBounds(): Rect {
        return composeTestRule
            .onNodeWithTag(testTag = TRAILING_CONTENT_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
    }

    private fun trailingHintCenter(): Offset {
        val trailingBounds = trailingContentBounds()

        return Offset(
            x = trailingBounds.left + TRAILING_HINT_WIDTH.toPixels() / 2f,
            y = trailingBounds.center.y,
        )
    }

    private fun chipDescription(recipient: SelectedRecipient): String {
        return targetContext.getString(
            R.string.recipient_selection_selected_recipient_content_description,
            recipient.label,
        )
    }

    private fun Dp.toPixels(): Int {
        return with(composeTestRule.density) { roundToPx() }
    }

    private fun columns(bounds: Rect): IntRange {
        return bounds.left.roundToInt() until bounds.right.roundToInt()
    }

    private fun Rect.crossesRow(row: Int, bounds: Rect): Boolean {
        return top <= row && row < bottom && left < bounds.right && right > bounds.left
    }

    private fun Color.isTrailingContent(): Boolean {
        return toArgb() == TRAILING_CONTENT_COLOR.toArgb()
    }

    private fun Color.isChip(): Boolean {
        return toArgb() != BACKGROUND_COLOR.toArgb() && !isTrailingContent()
    }

    private companion object {
        private const val ANIMATION_FRAME_COUNT = 45
        private const val TRAILING_CONTENT_TAG = "selected_recipient_chips_trailing"

        private val BACKGROUND_COLOR = Color.White
        private val CHIP_END_PADDING_PROBE_INSET = 6.dp
        private val CHIP_SPACING = 8.dp
        private val HOST_WIDTH = 320.dp
        private val TRAILING_CONTENT_COLOR = Color.Magenta
        private val TRAILING_CONTENT_WIDTH = 40.dp
        private val TRAILING_HINT_WIDTH = 24.dp
        private val WRAP_SLACK = 24.dp

        private val ada = selectedRecipient(label = "Ada")
        private val bob = selectedRecipient(label = "Bob")
        private val cat = selectedRecipient(label = "Cat")

        private fun selectedRecipient(label: String): SelectedRecipient {
            return SelectedRecipient(
                destination = "+15550100-$label",
                label = label,
                displayDestination = "+1 555 0100",
                photoUri = null,
            )
        }
    }
}
