package com.android.messaging.ui.common.components.composer

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.ContentInfo
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import com.android.messaging.R
import com.android.messaging.ui.common.components.ComposeBarControlHeight

private val TextFieldPadding = 16.dp
private val TextFieldPaddingBesideIcon = 4.dp

private const val MAX_LINES = 4
private const val SELECTION_HIGHLIGHT_ALPHA = 0.4f

private const val MESSAGE_INPUT_TYPE = InputType.TYPE_CLASS_TEXT or
    InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE or
    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or
    InputType.TYPE_TEXT_FLAG_MULTI_LINE

/**
 * The message field is a platform [EditText] rather than a Compose text field: only platform text
 * views get the system spell checker's underlines, and Compose applies a keyboard's batched
 * edits too late for the keyboard to read them back, which made backspacing into a picked
 * suggestion delete the space before the word.
 */
@Composable
internal fun MessageComposeTextField(
    text: String,
    textRevision: Int,
    onTextChange: (text: String, textRevision: Int) -> Unit,
    isEnabled: Boolean,
    isContentHidden: Boolean,
    stateDescription: String?,
    hasLeadingContent: Boolean,
    hasTrailingContent: Boolean,
    onImeSend: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val colorScheme = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge
    val imeSendLabel = stringResource(id = R.string.sendButtonContentDescription)
    val textSync = remember { MessageComposeTextSync() }
    val currentOnTextChange = rememberUpdatedState(newValue = onTextChange)
    val currentOnImeSend = rememberUpdatedState(newValue = onImeSend)
    val currentIsEnabled = rememberUpdatedState(newValue = isEnabled)
    val currentIsContentHidden = rememberUpdatedState(newValue = isContentHidden)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            createMessageEditText(
                context = context,
                density = density,
                textSync = textSync,
                isEnabled = currentIsEnabled,
                isContentHidden = currentIsContentHidden,
                onTextChange = currentOnTextChange,
                onImeSend = currentOnImeSend,
            )
        },
        update = { editText ->
            editText.applyStyle(
                density = density,
                colorScheme = colorScheme,
                textStyle = textStyle,
                hasLeadingContent = hasLeadingContent,
                hasTrailingContent = hasTrailingContent,
            )
            if (editText.isAttachedToWindow) {
                editText.applyEnabled(isEnabled = isEnabled, textSync = textSync)
            }
            editText.stateDescription = stateDescription
            editText.importantForAccessibility = when {
                isContentHidden -> View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                else -> View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            }
            editText.applyImeSend(
                isImeSendEnabled = onImeSend != null,
                imeSendLabel = imeSendLabel,
            )
            editText.syncText(
                text = text,
                textRevision = textRevision,
                textSync = textSync,
            )
        },
    )
}

private fun createMessageEditText(
    context: Context,
    density: Density,
    textSync: MessageComposeTextSync,
    isEnabled: State<Boolean>,
    isContentHidden: State<Boolean>,
    onTextChange: State<(text: String, textRevision: Int) -> Unit>,
    onImeSend: State<(() -> Unit)?>,
): EditText {
    return MessageEditText(context = context).apply {
        // A real view id makes keyboards treat this as the ordinary text field it is, rather than
        // falling back to workarounds for Compose fields, which have no id.
        id = View.generateViewId()
        isSaveEnabled = false
        background = null
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        minHeight = with(density) { ComposeBarControlHeight.roundToPx() }
        maxLines = MAX_LINES
        includeFontPadding = false
        inputType = MESSAGE_INPUT_TYPE
        hint = context.getString(R.string.compose_message_view_hint_text)

        // A text view only starts its spell checker when it's attached while enabled, and the
        // conversation screen shows the field disabled until its conversation loads. So the field
        // is enabled whenever it's attached and takes its real state right after.
        addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    view.isEnabled = isEnabled.value
                }

                override fun onViewDetachedFromWindow(view: View) {
                    view.isEnabled = true
                }
            },
        )

        // While the field is hidden behind the recording overlay the keyboard stays up, so edits
        // are refused here instead of disabling the field, which would close the keyboard.
        filters = arrayOf(
            InputFilter { _, _, _, destination, destinationStart, destinationEnd ->
                when {
                    isContentHidden.value && !textSync.isApplyingOwnerText -> {
                        destination.subSequence(destinationStart, destinationEnd)
                    }

                    else -> null
                }
            },
        )

        // Owner text the field takes isn't reported back: the owner already has it, or has replaced
        // it since, in which case reporting it would undo the newer text.
        doAfterTextChanged { editable ->
            val fieldText = editable?.toString().orEmpty()
            textSync.onFieldTextChanged(fieldText = fieldText)?.let { textRevision ->
                onTextChange.value(fieldText, textRevision)
            }
        }

        setOnEditorActionListener { _, actionId, _ ->
            val onSend = onImeSend.value
            val isSendAction = actionId == EditorInfo.IME_ACTION_SEND && onSend != null
            if (isSendAction) {
                onSend()
            }
            isSendAction
        }
    }
}

/** The platform text view behind the message field. */
private class MessageEditText(
    context: Context,
) : EditText(context) {

    /**
     * Pasted and dropped content is taken as plain text: a message is sent as plain text, and
     * formatting such as a centered paragraph would otherwise outlive the text it came with.
     */
    override fun onReceiveContent(payload: ContentInfo): ContentInfo? {
        val plainTextPayload = ContentInfo.Builder(payload)
            .setFlags(payload.flags or ContentInfo.FLAG_CONVERT_TO_PLAIN_TEXT)
            .build()

        return super.onReceiveContent(plainTextPayload)
    }

    /**
     * Picking another conversation in two panes removes the field while it's being typed in. The
     * keyboard would stay up with nothing to type into, so it's closed unless another text field
     * has taken it.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (rootView.findFocus()?.onCheckIsTextEditor() != true) {
            windowInsetsController?.hide(WindowInsets.Type.ime())
        }
    }
}

private fun EditText.applyStyle(
    density: Density,
    colorScheme: ColorScheme,
    textStyle: TextStyle,
    hasLeadingContent: Boolean,
    hasTrailingContent: Boolean,
) {
    with(density) {
        val verticalPadding = TextFieldPadding.roundToPx()
        val startPadding = when {
            hasLeadingContent -> TextFieldPaddingBesideIcon
            else -> TextFieldPadding
        }
        val endPadding = when {
            hasTrailingContent -> TextFieldPaddingBesideIcon
            else -> TextFieldPadding
        }

        setPaddingRelative(
            startPadding.roundToPx(),
            verticalPadding,
            endPadding.roundToPx(),
            verticalPadding,
        )
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textStyle.fontSize.toPx())
        lineHeight = textStyle.lineHeight.roundToPx()
        letterSpacing = textStyle.letterSpacing.toPx() / textStyle.fontSize.toPx()
    }

    setTextColor(colorScheme.onSurface.toArgb())
    setHintTextColor(colorScheme.onSurfaceVariant.toArgb())
    tintSelection(color = colorScheme.primary)
}

private fun EditText.tintSelection(color: Color) {
    val selectionHighlightColor = color.copy(alpha = SELECTION_HIGHLIGHT_ALPHA).toArgb()
    if (highlightColor == selectionHighlightColor) {
        return
    }

    val tint = color.toArgb()
    highlightColor = selectionHighlightColor
    textCursorDrawable = textCursorDrawable?.tinted(color = tint)
    textSelectHandle?.tinted(color = tint)?.let(::setTextSelectHandle)
    textSelectHandleLeft?.tinted(color = tint)?.let(::setTextSelectHandleLeft)
    textSelectHandleRight?.tinted(color = tint)?.let(::setTextSelectHandleRight)
}

private fun Drawable.tinted(color: Int): Drawable {
    return mutate().apply {
        setTint(color)
    }
}

private fun EditText.applyImeSend(isImeSendEnabled: Boolean, imeSendLabel: String) {
    val imeAction = when {
        isImeSendEnabled -> EditorInfo.IME_ACTION_SEND
        else -> EditorInfo.IME_ACTION_UNSPECIFIED
    }

    // Enter keeps inserting a new line; voice typing and accessibility services can still send.
    imeOptions = imeAction or
        EditorInfo.IME_FLAG_NO_ENTER_ACTION or
        EditorInfo.IME_FLAG_NO_FULLSCREEN

    when {
        isImeSendEnabled -> {
            setImeActionLabel(imeSendLabel, EditorInfo.IME_ACTION_SEND)
        }
        else -> {
            setImeActionLabel(null, EditorInfo.IME_NULL)
        }
    }
}

/**
 * A text view only spell-checks text it takes while enabled, and the conversation screen can load
 * a draft into the field before enabling it. So the field takes its text again once enabled.
 */
private fun EditText.applyEnabled(isEnabled: Boolean, textSync: MessageComposeTextSync) {
    val isBecomingEnabled = isEnabled && !this.isEnabled
    this.isEnabled = isEnabled

    if (isBecomingEnabled && text.isNotEmpty()) {
        takeText(
            text = text.toString(),
            selectionStart = selectionStart,
            selectionEnd = selectionEnd,
            textSync = textSync,
        )
    }
}

private fun EditText.syncText(
    text: String,
    textRevision: Int,
    textSync: MessageComposeTextSync,
) {
    val isOwnerTextNew = textSync.shouldApplyOwnerText(textRevision = textRevision)
    if (isOwnerTextNew && this.text.toString() != text) {
        takeText(
            text = text,
            selectionStart = text.length,
            selectionEnd = text.length,
            textSync = textSync,
        )
    }
}

/**
 * Puts text from the field's owner in the field. The platform spell checker opens its session on
 * the first check that finds text in the field, and checks nothing that time. So while the field
 * can be spell-checked, the text is put in twice: once to open the session, once to be checked.
 */
private fun EditText.takeText(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
    textSync: MessageComposeTextSync,
) {
    val takeCount = when {
        isAttachedToWindow && isEnabled && text.isNotEmpty() -> 2
        else -> 1
    }

    textSync.applyOwnerText {
        repeat(takeCount) {
            setText(text)
        }
        setSelection(selectionStart, selectionEnd)
    }
}
