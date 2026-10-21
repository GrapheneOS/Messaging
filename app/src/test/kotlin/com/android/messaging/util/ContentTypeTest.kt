package com.android.messaging.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTypeTest {

    @Test
    fun isPdfType_matchesPdfMimeTypeCaseInsensitively() {
        assertTrue(ContentType.isPdfType(ContentType.APPLICATION_PDF))
        assertTrue(ContentType.isPdfType("application/PDF"))
    }

    @Test
    fun isPdfType_rejectsOtherTypes() {
        assertFalse(ContentType.isPdfType(null))
        assertFalse(ContentType.isPdfType(""))
        assertFalse(ContentType.isPdfType(ContentType.IMAGE_JPEG))
        assertFalse(ContentType.isPdfType(ContentType.TEXT_PLAIN))
        assertFalse(ContentType.isPdfType("application/zip"))
    }
}
