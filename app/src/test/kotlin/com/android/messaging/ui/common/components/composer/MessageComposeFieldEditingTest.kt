package com.android.messaging.ui.common.components.composer

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Looper
import android.os.SystemClock
import android.text.style.AlignmentSpan
import android.text.style.URLSpan
import android.view.InputDevice
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionParams
import android.view.textservice.SpellCheckerSubtype
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.android.messaging.testutil.focusMessageField
import com.android.messaging.testutil.messageFieldView
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
internal class MessageComposeFieldEditingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var messageText by mutableStateOf("")
    private var messageTextRevision by mutableStateOf(0)
    private var isFieldEnabled by mutableStateOf(true)
    private var isContentHidden by mutableStateOf(false)
    private var isBarShown by mutableStateOf(true)
    private var isFieldIconShown by mutableStateOf(false)
    private var onTextChange: (text: String, textRevision: Int) -> Unit = ::takeEditAsOwner

    @Test
    fun messageComposeField_shownDisabledThenEnabled_getsTheSystemSpellChecker() {
        // The conversation screen shows the field disabled until its conversation loads.
        isFieldEnabled = false
        showMessageComposeBar()
        val field = composeTestRule.messageFieldView(testTag = MESSAGE_COMPOSE_FIELD_TEST_TAG)

        isFieldEnabled = true
        composeTestRule.waitForIdle()

        assertNotNull(field.spellChecker())
    }

    @Test
    @Config(shadows = [SpellCheckServiceShadow::class])
    fun messageComposeField_textItsOwnerSetsWhileDisabled_isSpellCheckedOnceEnabled() {
        // A draft can load before the conversation screen enables the field.
        isFieldEnabled = false
        showMessageComposeBar()
        val field = composeTestRule.messageFieldView(testTag = MESSAGE_COMPOSE_FIELD_TEST_TAG)
        awaitSpellCheckerLanguage(field = field)

        replaceTextAsOwner(text = "Helo wrold")
        composeTestRule.waitForIdle()
        isFieldEnabled = true
        composeTestRule.waitForIdle()

        assertEquals(listOf("Helo wrold"), field.spellCheckService().checkedTexts)
    }

    @Test
    @Config(shadows = [SpellCheckServiceShadow::class])
    fun messageComposeField_textItsOwnerSetsWhileEnabled_isSpellChecked() {
        showMessageComposeBar()
        val field = composeTestRule.messageFieldView(testTag = MESSAGE_COMPOSE_FIELD_TEST_TAG)
        awaitSpellCheckerLanguage(field = field)

        replaceTextAsOwner(text = "Helo wrold")
        composeTestRule.waitForIdle()

        assertEquals(listOf("Helo wrold"), field.spellCheckService().checkedTexts)
    }

    @Test
    fun messageComposeField_backspaceIntoAPickedSuggestion_keepsTheSpaceBeforeTheWord() {
        val inputConnection = focusedField().inputConnection()
        inputConnection.commitText("I want to succeeded", 1)
        composeTestRule.waitForIdle()

        // What LatinIME sends for a backspace right after a picked suggestion: delete one
        // character, then reopen the word before the cursor as the composing region, sized from
        // what getTextBeforeCursor reports inside the same batch.
        val cursorAfterDelete = "I want to succeede".length
        inputConnection.beginBatchEdit()
        inputConnection.deleteSurroundingText(1, 0)
        val textBeforeCursor = inputConnection.getTextBeforeCursor(1024, 0).toString()
        val wordBeforeCursor = textBeforeCursor.substringAfterLast(delimiter = ' ')
        inputConnection.setComposingRegion(
            cursorAfterDelete - wordBeforeCursor.length,
            cursorAfterDelete,
        )
        inputConnection.endBatchEdit()
        inputConnection.setComposingText("succeede", 1)
        composeTestRule.waitForIdle()

        assertEquals("I want to succeede", messageText)
    }

    @Test
    fun messageComposeField_pastingFormattedText_insertsItAsPlainText() {
        val field = focusedField()
        // What a browser puts on the clipboard when copying a centered paragraph with a link.
        val clipboard = field.context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(
            ClipData.newHtmlText(
                "Copied text",
                "Hello link",
                "<p style=\"text-align:center\">Hello <a href=\"https://example.com\">link</a></p>",
            ),
        )

        field.onTextContextMenuItem(android.R.id.paste)
        composeTestRule.waitForIdle()

        val fieldText = field.editableText
        assertEquals("Hello link", fieldText.toString().trim())
        assertTrue(fieldText.getSpans(0, fieldText.length, AlignmentSpan::class.java).isEmpty())
        assertTrue(fieldText.getSpans(0, fieldText.length, URLSpan::class.java).isEmpty())
    }

    @Test
    fun messageComposeField_typingPastItsLineLimitFasterThanFramesAreDrawn_keepsTheCursorInView() {
        val field = focusedField()
        val inputConnection = field.inputConnection()
        inputConnection.commitText("1\n2\n3\n4\n5", 1)
        composeTestRule.waitForIdle()

        // Fast typing commits key after key with no frame drawn in between.
        inputConnection.commitText("\n6", 1)
        inputConnection.commitText("\n7", 1)

        assertTrue(field.isCursorInView())
    }

    @Test
    fun messageComposeField_scrollingBackThroughItsText_leavesTheCursorWhereItWas() {
        val field = focusedField()
        field.inputConnection().commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        composeTestRule.waitForIdle()

        field.drag(distance = field.height.toFloat())
        composeTestRule.waitForIdle()

        assertFalse(field.isCursorInView())
        assertEquals(TEXT_PAST_THE_LINE_LIMIT.length, field.selectionStart)
        assertEquals(TEXT_PAST_THE_LINE_LIMIT.length, field.selectionEnd)
    }

    @Test
    fun messageComposeField_typingAfterScrollingBackThroughItsText_typesAtTheCursorAndShowsIt() {
        val field = focusedField()
        val inputConnection = field.inputConnection()
        inputConnection.commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        composeTestRule.waitForIdle()
        field.drag(distance = field.height.toFloat())
        composeTestRule.waitForIdle()

        inputConnection.commitText("9", 1)
        composeTestRule.waitForIdle()

        assertEquals("${TEXT_PAST_THE_LINE_LIMIT}9", field.text.toString())
        assertTrue(field.isCursorInView())
    }

    @Test
    fun messageComposeField_tappingTextAfterScrollingBackThroughIt_movesTheCursorThere() {
        val field = focusedField()
        field.inputConnection().commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        composeTestRule.waitForIdle()
        field.drag(distance = field.height.toFloat())
        composeTestRule.waitForIdle()

        field.tapLine(line = 0)
        composeTestRule.waitForIdle()

        assertEquals(0, field.layout.getLineForOffset(field.selectionEnd))
    }

    @Test
    fun messageComposeField_laidOutAgainAfterScrollingBackThroughItsText_staysScrolledBack() {
        val field = focusedField()
        field.inputConnection().commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        composeTestRule.waitForIdle()
        field.drag(distance = field.height.toFloat())
        composeTestRule.waitForIdle()
        val scrolledBackY = field.scrollY

        // Hiding the keyboard, for one, lays the field out again.
        field.requestLayout()
        composeTestRule.waitForIdle()

        assertEquals(scrolledBackY, field.scrollY)
        assertFalse(field.isCursorInView())
    }

    @Test
    fun messageComposeField_cursorMovedAfterScrollingBackThroughItsText_showsTheCursor() {
        val field = focusedField()
        val inputConnection = field.inputConnection()
        inputConnection.commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        composeTestRule.waitForIdle()
        field.drag(distance = field.height.toFloat())
        composeTestRule.waitForIdle()

        val beforeLastCharacter = TEXT_PAST_THE_LINE_LIMIT.length - 1
        inputConnection.setSelection(beforeLastCharacter, beforeLastCharacter)
        // Robolectric draws no frame when only the cursor moves, so the one a device draws is
        // drawn here.
        field.onPreDraw()

        assertTrue(field.isCursorInView())
    }

    @Test
    fun messageComposeField_deletingForwardAfterScrollingBackThroughItsText_showsTheCursor() {
        val field = focusedField()
        val inputConnection = field.inputConnection()
        inputConnection.commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        val beforeLastCharacter = TEXT_PAST_THE_LINE_LIMIT.length - 1
        inputConnection.setSelection(beforeLastCharacter, beforeLastCharacter)
        composeTestRule.waitForIdle()
        field.drag(distance = field.height.toFloat())
        composeTestRule.waitForIdle()

        inputConnection.deleteSurroundingText(0, 1)
        composeTestRule.waitForIdle()

        assertEquals(TEXT_PAST_THE_LINE_LIMIT.dropLast(1), field.text.toString())
        assertTrue(field.isCursorInView())
    }

    @Test
    fun messageComposeField_narrowedAfterScrollingBackThroughItsText_showsTheCursor() {
        val field = focusedField()
        field.inputConnection().commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        composeTestRule.waitForIdle()
        field.drag(distance = field.height.toFloat())
        composeTestRule.waitForIdle()

        isFieldIconShown = true
        composeTestRule.waitForIdle()

        assertTrue(field.isCursorInView())
    }

    @Test
    fun messageComposeField_scrollingBackOnceAtTheStartOfItsText_leavesTheCursorWhereItWas() {
        val field = focusedField()
        field.inputConnection().commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        composeTestRule.waitForIdle()
        field.drag(distance = field.height.toFloat())
        composeTestRule.waitForIdle()

        field.drag(distance = field.height.toFloat())
        composeTestRule.waitForIdle()

        assertEquals(TEXT_PAST_THE_LINE_LIMIT.length, field.selectionStart)
        assertEquals(TEXT_PAST_THE_LINE_LIMIT.length, field.selectionEnd)
    }

    @Test
    fun messageComposeField_scrollingOnAtTheEndOfItsText_leavesTheCursorWhereItWas() {
        val field = focusedField()
        field.inputConnection().commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        composeTestRule.waitForIdle()

        field.drag(distance = -field.height.toFloat())
        composeTestRule.waitForIdle()

        assertEquals(TEXT_PAST_THE_LINE_LIMIT.length, field.selectionStart)
        assertEquals(TEXT_PAST_THE_LINE_LIMIT.length, field.selectionEnd)
    }

    @Test
    fun messageComposeField_laidOutAgainAfterACancelledScrollBack_staysScrolledBack() {
        val field = focusedField()
        field.inputConnection().commitText(TEXT_PAST_THE_LINE_LIMIT, 1)
        composeTestRule.waitForIdle()
        field.drag(distance = field.height.toFloat(), endAction = MotionEvent.ACTION_CANCEL)
        composeTestRule.waitForIdle()
        val scrolledBackY = field.scrollY

        field.requestLayout()
        composeTestRule.waitForIdle()

        assertEquals(scrolledBackY, field.scrollY)
        assertFalse(field.isCursorInView())
    }

    @Test
    fun messageComposeField_showsTextItsOwnerSets() {
        messageText = "Draft"
        val field = focusedField()

        assertEquals("Draft", field.text.toString())
        assertEquals("Draft".length, field.selectionStart)

        replaceTextAsOwner(text = "")
        composeTestRule.waitForIdle()

        assertEquals("", field.text.toString())
    }

    @Test
    fun messageComposeField_keepsTypingThatItsOwnerHasNotCaughtUpWith() {
        val reportedTexts = mutableListOf<String>()
        onTextChange = { text, _ -> reportedTexts += text }
        val field = focusedField()
        val inputConnection = field.inputConnection()

        inputConnection.commitText("H", 1)
        inputConnection.commitText("i", 1)
        messageText = reportedTexts.first()
        composeTestRule.waitForIdle()

        assertEquals(listOf("H", "Hi"), reportedTexts)
        assertEquals("Hi", field.text.toString())
    }

    @Test
    fun messageComposeField_reportsEachEditWithTheRevisionItWasTypedOver() {
        val reportedEdits = mutableListOf<Pair<String, Int>>()
        onTextChange = { text, textRevision -> reportedEdits += text to textRevision }
        messageText = "Draft"
        val field = focusedField()
        val inputConnection = field.inputConnection()

        inputConnection.commitText("!", 1)
        replaceTextAsOwner(text = "")
        composeTestRule.waitForIdle()
        inputConnection.commitText("A", 1)

        // The owner's own text, which the field takes in between, isn't reported back.
        assertEquals(listOf("Draft!" to 0, "A" to 1), reportedEdits)
    }

    @Test
    fun messageComposeField_ownerCatchingUpWithABatchOneEditAtATime_keepsTheBatchText() {
        val reportedTexts = mutableListOf<String>()
        onTextChange = { text, _ -> reportedTexts += text }
        messageText = "Hello"
        val field = focusedField()
        reportedTexts.clear()

        field.replaceAllInOneBatch(text = "Hello")
        messageText = reportedTexts.first()
        composeTestRule.waitForIdle()

        assertEquals(listOf("", "Hello"), reportedTexts)
        assertEquals("Hello", field.text.toString())
    }

    @Test
    fun messageComposeField_ownerClearsAfterEditsThatReturnedToItsText_showsTheClearedText() {
        messageText = "Hello"
        val field = focusedField()
        field.replaceAllInOneBatch(text = "Hello")
        composeTestRule.waitForIdle()

        // What the conversation screen does once the draft has been sent.
        composeTestRule.runOnIdle { replaceTextAsOwner(text = "") }
        composeTestRule.waitForIdle()

        assertEquals("", field.text.toString())
    }

    @Test
    fun messageComposeField_ownerClearsBeforeCatchingUpWithEdits_showsTheClearedText() {
        messageText = "Hello"
        val field = focusedField()

        composeTestRule.runOnIdle {
            field.replaceAllInOneBatch(text = "Hello")
            replaceTextAsOwner(text = "")
        }
        composeTestRule.waitForIdle()

        assertEquals("", field.text.toString())
    }

    @Test
    fun messageComposeField_typingOverAnOwnerClearItHasNotShownYet_endsWithTheClear() {
        messageText = "Hello"
        val field = focusedField()

        composeTestRule.runOnIdle {
            replaceTextAsOwner(text = "")
            field.inputConnection().commitText("!", 1)
        }
        composeTestRule.waitForIdle()

        assertEquals("", field.text.toString())
        assertEquals("", messageText)
    }

    @Test
    fun messageComposeField_whileContentIsHidden_ignoresTyping() {
        messageText = "Hello"
        val field = focusedField()
        isContentHidden = true
        composeTestRule.waitForIdle()

        field.inputConnection().commitText("!", 1)
        composeTestRule.waitForIdle()

        assertEquals("Hello", field.text.toString())
        assertEquals("Hello", messageText)
    }

    @Test
    @Config(shadows = [ImeHideRecordingShadow::class])
    fun messageComposeField_removedWhileFocused_closesTheKeyboard() {
        // What picking another conversation in two panes does to the field being typed in.
        val field = focusedField()
        val insetsController = Shadow.extract<ImeHideRecordingShadow>(field.windowInsetsController)

        isBarShown = false
        composeTestRule.waitForIdle()

        assertTrue(insetsController.isImeHideRequested)
    }

    /** What an owner does with an edit: takes it, unless it was typed over text since replaced. */
    private fun takeEditAsOwner(text: String, textRevision: Int) {
        if (textRevision == messageTextRevision) {
            messageText = text
        }
    }

    /** What an owner does when it replaces the text itself, rather than taking the field's. */
    private fun replaceTextAsOwner(text: String) {
        messageText = text
        messageTextRevision++
    }

    private fun TextView.inputConnection(): InputConnection {
        return onCreateInputConnection(EditorInfo())!!
    }

    /** Clears the field and types [text] in one keyboard batch, reporting both edits at once. */
    private fun TextView.replaceAllInOneBatch(text: String) {
        val inputConnection = inputConnection()
        inputConnection.beginBatchEdit()
        inputConnection.setSelection(0, length())
        inputConnection.commitText("", 1)
        inputConnection.commitText(text, 1)
        inputConnection.endBatchEdit()
    }

    /** Whether the line the cursor is on shows in the field, rather than being scrolled out of it. */
    private fun TextView.isCursorInView(): Boolean {
        val cursorLine = layout.getLineForOffset(selectionEnd)
        val visibleBottom = scrollY + height - extendedPaddingTop - extendedPaddingBottom

        return layout.getLineTop(cursorLine) >= scrollY &&
            layout.getLineBottom(cursorLine) <= visibleBottom
    }

    /**
     * Drags a finger [distance] pixels down the field, or up for a negative distance, a frame at a
     * time, then ends the touch with [endAction]. The drag starts long enough after the last touch
     * not to be taken for a double tap.
     */
    private fun TextView.drag(distance: Float, endAction: Int = MotionEvent.ACTION_UP) {
        shadowOf(Looper.getMainLooper()).idleFor(TOUCH_GAP_MILLIS, TimeUnit.MILLISECONDS)
        val x = width / 2f
        val startY = height / 2f
        val endY = startY + distance
        val downTime = SystemClock.uptimeMillis()

        dispatchTouch(downTime = downTime, action = MotionEvent.ACTION_DOWN, x = x, y = startY)
        for (frame in 1..DRAG_FRAME_COUNT) {
            shadowOf(Looper.getMainLooper()).idleFor(FRAME_MILLIS, TimeUnit.MILLISECONDS)
            val y = startY + distance * frame / DRAG_FRAME_COUNT
            dispatchTouch(downTime = downTime, action = MotionEvent.ACTION_MOVE, x = x, y = y)
        }
        dispatchTouch(downTime = downTime, action = endAction, x = x, y = endY)
    }

    /**
     * Taps the start of [line] where it shows in the field, long enough after the last touch not
     * to be taken for a double tap.
     */
    private fun TextView.tapLine(line: Int) {
        shadowOf(Looper.getMainLooper()).idleFor(TOUCH_GAP_MILLIS, TimeUnit.MILLISECONDS)
        val x = totalPaddingLeft.toFloat()
        val lineCenter = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f
        val y = totalPaddingTop - scrollY + lineCenter
        val downTime = SystemClock.uptimeMillis()

        dispatchTouch(downTime = downTime, action = MotionEvent.ACTION_DOWN, x = x, y = y)
        dispatchTouch(downTime = downTime, action = MotionEvent.ACTION_UP, x = x, y = y)
    }

    private fun TextView.dispatchTouch(downTime: Long, action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        dispatchTouchEvent(event)
        event.recycle()
    }

    /** The platform spell checker that underlines misspelled words, or null if it never started. */
    private fun TextView.spellChecker(): Any? {
        val editor = ReflectionHelpers.getField<Any>(this, "mEditor")
        return ReflectionHelpers.getField<Any?>(editor, "mSpellChecker")
    }

    /** The system spell checker service, as the field sees it. */
    private fun TextView.spellCheckService(): SpellCheckServiceShadow {
        return Shadow.extract(context.getSystemService(TextServicesManager::class.java))
    }

    /** Waits for the spell checker's language, which a text view looks up in the background. */
    private fun awaitSpellCheckerLanguage(field: TextView) {
        composeTestRule.waitUntil {
            ReflectionHelpers.getField<Locale?>(field, "mCurrentSpellCheckerLocaleCache") != null
        }
    }

    private fun focusedField(): TextView {
        showMessageComposeBar()

        return composeTestRule.focusMessageField(testTag = MESSAGE_COMPOSE_FIELD_TEST_TAG)
    }

    private fun showMessageComposeBar() {
        val fieldIcon: @Composable () -> Unit = {}

        composeTestRule.setContent {
            AppTheme {
                if (isBarShown) {
                    MessageComposeBar(
                        text = messageText,
                        textRevision = messageTextRevision,
                        onTextChange = { text, textRevision -> onTextChange(text, textRevision) },
                        isFieldEnabled = isFieldEnabled,
                        isFieldContentHidden = isContentHidden,
                        fieldFocusRequester = null,
                        fieldStateDescription = null,
                        fieldTestTag = MESSAGE_COMPOSE_FIELD_TEST_TAG,
                        sendAction = {},
                        trailingContent = fieldIcon.takeIf { isFieldIconShown },
                    )
                }
            }
        }
    }

    private companion object {
        /** Twice as many lines as the field shows, with the cursor after the last one. */
        const val TEXT_PAST_THE_LINE_LIMIT = "1\n2\n3\n4\n5\n6\n7\n8"
        const val DRAG_FRAME_COUNT = 10
        const val FRAME_MILLIS = 16L
        const val TOUCH_GAP_MILLIS = 1_000L
    }
}

/**
 * A system spell checker that's on for English and records the text it's asked to check, instead of
 * checking it.
 */
@Implements(TextServicesManager::class)
internal class SpellCheckServiceShadow {

    val checkedTexts = mutableListOf<String>()

    @Implementation
    fun isSpellCheckerEnabled(): Boolean {
        return true
    }

    @Implementation
    @Suppress("UNUSED_PARAMETER")
    fun getCurrentSpellCheckerSubtype(
        allowImplicitlySelectedSubtype: Boolean,
    ): SpellCheckerSubtype {
        @Suppress("DEPRECATION")
        return SpellCheckerSubtype(0, "en_US", "")
    }

    @Implementation
    @Suppress("UNUSED_PARAMETER")
    fun newSpellCheckerSession(
        params: SpellCheckerSessionParams,
        executor: Executor,
        listener: SpellCheckerSessionListener,
    ): SpellCheckerSession {
        return mockk(relaxed = true) {
            every { getSentenceSuggestions(any(), any()) } answers {
                checkedTexts += firstArg<Array<TextInfo>>().map { textInfo -> textInfo.text }
            }
        }
    }
}

/** Records the window's requests to hide the keyboard, instead of hiding it. */
@Implements(className = "android.view.InsetsController", isInAndroidSdk = false)
internal class ImeHideRecordingShadow {

    var isImeHideRequested = false
        private set

    @Implementation
    fun hide(types: Int) {
        if (types and WindowInsets.Type.ime() != 0) {
            isImeHideRequested = true
        }
    }
}
