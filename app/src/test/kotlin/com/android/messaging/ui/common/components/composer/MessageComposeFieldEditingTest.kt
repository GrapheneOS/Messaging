package com.android.messaging.ui.common.components.composer

import android.content.ClipData
import android.content.ClipboardManager
import android.text.style.AlignmentSpan
import android.text.style.URLSpan
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
                    )
                }
            }
        }
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
