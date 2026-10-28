package com.android.messaging.ui.conversationlist.common.item

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.common.components.LocalIsListDetailPane
import com.android.messaging.ui.conversationlist.common.support.conversationListItemTestTag
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.core.AppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
internal class ConversationListItemRowTest {

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
    fun openedRow_inListDetailPane_standsOutFromPaneAndOtherRows() {
        val colorScheme = setRow(
            item = rowItem().copy(isOpened = true),
            isListDetailPane = true,
        )

        val rowColor = rowContainerColor()
        assertColorEquals(expected = colorScheme.secondaryContainer, actual = rowColor)
        assertNotEquals(colorScheme.surfaceContainerLow.toArgb(), rowColor.toArgb())
        assertNotEquals(colorScheme.background.toArgb(), rowColor.toArgb())
    }

    @Test
    @Config(qualifiers = "night")
    fun openedRow_inListDetailPaneDarkTheme_standsOutFromPaneAndOtherRows() {
        val colorScheme = setRow(
            item = rowItem().copy(isOpened = true),
            isListDetailPane = true,
        )

        val rowColor = rowContainerColor()
        assertColorEquals(expected = colorScheme.secondaryContainer, actual = rowColor)
        assertNotEquals(colorScheme.surfaceContainerLow.toArgb(), rowColor.toArgb())
        assertNotEquals(colorScheme.background.toArgb(), rowColor.toArgb())
    }

    @Test
    fun selectedRow_inListDetailPane_usesHighlightColor() {
        val colorScheme = setRow(
            item = rowItem(isSelected = true),
            isListDetailPane = true,
        )

        assertColorEquals(expected = colorScheme.secondaryContainer, actual = rowContainerColor())
    }

    @Test
    fun plainRow_inListDetailPane_usesBackground() {
        val colorScheme = setRow(
            item = rowItem(),
            isListDetailPane = true,
        )

        assertColorEquals(expected = colorScheme.background, actual = rowContainerColor())
    }

    @Test
    fun selectedRow_inSinglePane_keepsSurfaceContainerLow() {
        val colorScheme = setRow(
            item = rowItem(isSelected = true),
            isListDetailPane = false,
        )

        assertColorEquals(expected = colorScheme.surfaceContainerLow, actual = rowContainerColor())
    }

    private fun setRow(
        item: ConversationListItemUiModel,
        isListDetailPane: Boolean,
    ): ColorScheme {
        lateinit var colorScheme: ColorScheme

        composeTestRule.setContent {
            AppTheme {
                colorScheme = MaterialTheme.colorScheme

                CompositionLocalProvider(LocalIsListDetailPane provides isListDetailPane) {
                    ConversationListItemRow(
                        item = item,
                        onClick = {},
                        onLongClick = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        return colorScheme
    }

    // Samples the row's trailing padding at mid-height, where only the container is drawn.
    private fun rowContainerColor(): Color {
        val image = composeTestRule
            .onNodeWithTag(conversationListItemTestTag(ROW_ID))
            .captureToImage()
        val inset = with(composeTestRule.density) { CONTAINER_SAMPLE_INSET.roundToPx() }

        return image.toPixelMap()[image.width - inset, image.height / 2]
    }

    private fun assertColorEquals(expected: Color, actual: Color) {
        assertEquals(expected.toArgb(), actual.toArgb())
    }

    private fun rowItem(isSelected: Boolean = false): ConversationListItemUiModel {
        return previewConversationListItem(
            conversationId = ROW_ID,
            title = "Ada Lovelace",
            snippetText = "Sounds good, thanks!",
            isSelected = isSelected,
        )
    }

    private companion object {
        private val ROW_ID = ConversationId("row")
        private val CONTAINER_SAMPLE_INSET = 4.dp
    }
}
