package com.android.messaging.ui.common.components.composer

/**
 * Keeps the message field and the owner of its text in step. The owner learns about edits
 * asynchronously, so the text it hands back can lag behind the field, and that text alone can't
 * tell a late echo of an edit from the owner replacing the text with the same value. So the owner's
 * text only replaces the field's under a new text revision, which the owner moves whenever it
 * replaces the text itself, and each edit tells the owner which revision it was typed over.
 */
internal class MessageComposeTextSync {

    private var appliedTextRevision: Int? = null
    private var lastFieldText: String? = null

    var isApplyingOwnerText: Boolean = false
        private set

    /**
     * Returns the revision of the owner's text that [fieldText] was typed over, or null when there
     * is no edit to report: the text didn't change, or it is the owner's own text being applied.
     */
    fun onFieldTextChanged(fieldText: String): Int? {
        val isNewText = fieldText != lastFieldText
        lastFieldText = fieldText

        return when {
            isNewText && !isApplyingOwnerText -> appliedTextRevision
            else -> null
        }
    }

    /** Returns whether the owner's text, at [textRevision], must replace the field's. */
    fun shouldApplyOwnerText(textRevision: Int): Boolean {
        val isNewRevision = textRevision != appliedTextRevision
        appliedTextRevision = textRevision

        return isNewRevision
    }

    fun applyOwnerText(apply: () -> Unit) {
        isApplyingOwnerText = true
        try {
            apply()
        } finally {
            isApplyingOwnerText = false
        }
    }
}
