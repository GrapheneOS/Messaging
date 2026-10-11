package com.android.messaging.data.search.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class SnippetAroundTest {

    @Test
    fun earlyMatch_keepsTheWholeText() {
        assertEquals("find the needle here", "find the needle here".snippetAround("needle"))
    }

    @Test
    fun lateMatch_startsOnAWordShortlyBeforeIt() {
        val text = "This is a fairly long message that eventually mentions the needle near the end"

        assertEquals(
            "\u2026mentions the needle near the end",
            text.snippetAround("NEEDLE"),
        )
    }

    @Test
    fun noMatch_keepsTheWholeText() {
        assertEquals("nothing to see", "nothing to see".snippetAround("needle"))
    }
}
