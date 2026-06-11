package com.android.messaging.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MmsSubjectSanitizerTest {

    @Test
    fun cleanseMmsSubject_returnsNullForNullSubject() {
        val cleansedSubject = cleanseMmsSubject(
            subject = null,
            emptySubjectStrings = EMPTY_SUBJECT_STRINGS,
        )

        assertNull(cleansedSubject)
    }

    @Test
    fun cleanseMmsSubject_returnsNullForEmptySubject() {
        val cleansedSubject = cleanseMmsSubject(
            subject = "",
            emptySubjectStrings = EMPTY_SUBJECT_STRINGS,
        )

        assertNull(cleansedSubject)
    }

    @Test
    fun cleanseMmsSubject_returnsNullForConfiguredNoSubjectValue_ignoringCase() {
        val cleansedSubject = cleanseMmsSubject(
            subject = "No Subject",
            emptySubjectStrings = EMPTY_SUBJECT_STRINGS,
        )

        assertNull(cleansedSubject)
    }

    @Test
    fun cleanseMmsSubject_returnsOriginalSubjectForRealSubject() {
        val cleansedSubject = cleanseMmsSubject(
            subject = "Trip details",
            emptySubjectStrings = EMPTY_SUBJECT_STRINGS,
        )

        assertEquals("Trip details", cleansedSubject)
    }

    private companion object {
        private val EMPTY_SUBJECT_STRINGS = arrayOf(
            "no subject",
            "nosubject",
        )
    }
}
