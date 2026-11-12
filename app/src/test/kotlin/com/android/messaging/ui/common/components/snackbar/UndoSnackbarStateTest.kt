package com.android.messaging.ui.common.components.snackbar

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UndoSnackbarStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val undone = mutableListOf<TestUndoable>()
    private val dismissed = mutableListOf<TestUndoable>()

    private lateinit var undoSnackbarState: UndoSnackbarState<TestUndoable>

    @Test
    fun undo_afterSameKindUndoables_undoesTheMergedUndoable() {
        setContent()

        show(undoable = TestUndoable(kind = ARCHIVE, ids = listOf("1")))
        show(undoable = TestUndoable(kind = ARCHIVE, ids = listOf("2")))
        composeTestRule.onNodeWithText(text = "$ARCHIVE 1, 2").assertExists()
        composeTestRule.onNodeWithText(text = UNDO).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(TestUndoable(kind = ARCHIVE, ids = listOf("1", "2"))), undone)
        assertEquals(emptyList<TestUndoable>(), dismissed)
    }

    @Test
    fun show_differentKind_dismissesTheShownUndoableAtOnce() {
        setContent()

        show(undoable = TestUndoable(kind = ARCHIVE, ids = listOf("1")))
        show(undoable = TestUndoable(kind = BLOCK, ids = listOf("2")))

        assertEquals(listOf(TestUndoable(kind = ARCHIVE, ids = listOf("1"))), dismissed)
        composeTestRule.onNodeWithText(text = "$ARCHIVE 1").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = UNDO).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(TestUndoable(kind = BLOCK, ids = listOf("2"))), undone)
    }

    @Test
    fun merge_restartsTheTimeout() {
        setContent()
        composeTestRule.mainClock.autoAdvance = false

        show(undoable = TestUndoable(kind = ARCHIVE, ids = listOf("1")))
        composeTestRule.mainClock.advanceTimeBy(milliseconds = MOST_OF_TIMEOUT_MILLIS)
        show(undoable = TestUndoable(kind = ARCHIVE, ids = listOf("2")))
        composeTestRule.mainClock.advanceTimeBy(milliseconds = MOST_OF_TIMEOUT_MILLIS)

        assertEquals(emptyList<TestUndoable>(), dismissed)

        composeTestRule.mainClock.advanceTimeBy(milliseconds = MOST_OF_TIMEOUT_MILLIS)

        assertEquals(listOf(TestUndoable(kind = ARCHIVE, ids = listOf("1", "2"))), dismissed)
        assertEquals(emptyList<TestUndoable>(), undone)
    }

    private fun show(undoable: TestUndoable) {
        composeTestRule.runOnIdle { undoSnackbarState.show(undoable = undoable) }
        composeTestRule.waitForIdle()
    }

    private fun setContent() {
        composeTestRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }

            undoSnackbarState = rememberUndoSnackbarState(
                snackbarHostState = snackbarHostState,
                merge = ::merge,
                message = { undoable -> "${undoable.kind} ${undoable.ids.joinToString()}" },
                onUndo = { undoable -> undone += undoable },
                onDismiss = { undoable -> dismissed += undoable },
            )

            SnackbarHost(hostState = snackbarHostState)
        }
        composeTestRule.waitForIdle()
    }

    private fun merge(shown: TestUndoable, newer: TestUndoable): TestUndoable? {
        return when (shown.kind) {
            newer.kind -> TestUndoable(kind = shown.kind, ids = shown.ids + newer.ids)
            else -> null
        }
    }

    private data class TestUndoable(
        val kind: String,
        val ids: List<String>,
    )

    private companion object {
        const val ARCHIVE = "archive"
        const val BLOCK = "block"
        const val UNDO = "Undo"
        const val MOST_OF_TIMEOUT_MILLIS = 3_000L
    }
}
