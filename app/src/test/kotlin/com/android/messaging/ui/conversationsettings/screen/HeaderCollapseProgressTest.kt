package com.android.messaging.ui.conversationsettings.screen

import org.junit.Assert.assertEquals
import org.junit.Test

internal class HeaderCollapseProgressTest {

    @Test
    fun headerCollapseProgress_listScrollsFurtherThanHeader_followsScrollOffset() {
        val progress = headerCollapseProgress(
            scrollOffset = COLLAPSE_DISTANCE / 4,
            headerSize = HEADER_SIZE,
            maxScrollDistance = HEADER_SIZE * 2,
        )

        assertEquals(0.25f, progress, 0f)
    }

    @Test
    fun headerCollapseProgress_listScrollsExactlyOneHeader_collapsesFully() {
        val progress = headerCollapseProgress(
            scrollOffset = HEADER_SIZE,
            headerSize = HEADER_SIZE,
            maxScrollDistance = HEADER_SIZE,
        )

        assertEquals(1f, progress, 0f)
    }

    @Test
    fun headerCollapseProgress_listScrollsJustShortOfHeader_collapsesFully() {
        val scrollDistance = COLLAPSE_DISTANCE + 1

        val progress = headerCollapseProgress(
            scrollOffset = scrollDistance,
            headerSize = HEADER_SIZE,
            maxScrollDistance = scrollDistance,
        )

        assertEquals(1f, progress, 0f)
    }

    @Test
    fun headerCollapseProgress_listScrollsExactlyTheCollapseDistance_collapsesFully() {
        val progress = headerCollapseProgress(
            scrollOffset = COLLAPSE_DISTANCE,
            headerSize = HEADER_SIZE,
            maxScrollDistance = COLLAPSE_DISTANCE,
        )

        assertEquals(1f, progress, 0f)
    }

    @Test
    fun headerCollapseProgress_listScrollsLessThanHeaderCollapseDistance_staysExpanded() {
        val progress = headerCollapseProgress(
            scrollOffset = 12,
            headerSize = HEADER_SIZE,
            maxScrollDistance = COLLAPSE_DISTANCE - 1,
        )

        assertEquals(0f, progress, 0f)
    }

    @Test
    fun headerCollapseProgress_endOfListNotLaidOut_followsScrollOffset() {
        val progress = headerCollapseProgress(
            scrollOffset = COLLAPSE_DISTANCE / 2,
            headerSize = HEADER_SIZE,
            maxScrollDistance = null,
        )

        assertEquals(0.5f, progress, 0f)
    }

    @Test
    fun headerCollapseProgress_headerNotMeasured_staysExpanded() {
        val progress = headerCollapseProgress(
            scrollOffset = 12,
            headerSize = 0,
            maxScrollDistance = null,
        )

        assertEquals(0f, progress, 0f)
    }

    private companion object {
        const val HEADER_SIZE = 400

        /** Scroll distance over which the cross-fade of a [HEADER_SIZE] header completes. */
        const val COLLAPSE_DISTANCE = 360
    }
}
