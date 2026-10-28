package com.android.messaging.ui.conversationlist.common.pane

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.android.messaging.ui.common.components.LocalIsListDetailPane
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ListPaneColorsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listPaneContentColor_listDetailPane_usesSurfaceContainerLow() {
        val colors = resolveColors(isListDetailPane = true)

        assertEquals(colors.colorScheme.surfaceContainerLow, colors.content)
    }

    @Test
    fun listPaneContentColor_singlePane_usesBackground() {
        val colors = resolveColors(isListDetailPane = false)

        assertEquals(colors.colorScheme.background, colors.content)
    }

    @Test
    fun listPaneHighlightedItemColor_listDetailPane_usesSecondaryContainer() {
        val colors = resolveColors(isListDetailPane = true)

        assertEquals(colors.colorScheme.secondaryContainer, colors.highlightedItem)
    }

    @Test
    fun listPaneHighlightedItemColor_singlePane_usesSurfaceContainerLow() {
        val colors = resolveColors(isListDetailPane = false)

        assertEquals(colors.colorScheme.surfaceContainerLow, colors.highlightedItem)
    }

    private fun resolveColors(isListDetailPane: Boolean): ResolvedColors {
        lateinit var colors: ResolvedColors

        composeTestRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalIsListDetailPane provides isListDetailPane) {
                    colors = ResolvedColors(
                        colorScheme = MaterialTheme.colorScheme,
                        content = listPaneContentColor(),
                        highlightedItem = listPaneHighlightedItemColor(),
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        return colors
    }

    private data class ResolvedColors(
        val colorScheme: ColorScheme,
        val content: Color,
        val highlightedItem: Color,
    )
}
