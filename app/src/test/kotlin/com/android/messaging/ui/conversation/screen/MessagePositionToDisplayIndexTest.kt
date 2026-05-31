package com.android.messaging.ui.conversation.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class MessagePositionToDisplayIndexTest {

    @Test
    fun position0_inFiveItemList_mapsToLastDisplayIndex() {
        assertEquals(4, messagePositionToDisplayIndex(position = 0, size = 5))
    }

    @Test
    fun lastPosition_mapsToFirstDisplayIndex() {
        assertEquals(0, messagePositionToDisplayIndex(position = 4, size = 5))
    }

    @Test
    fun positionAtOrBeyondSize_clampsToZero() {
        assertEquals(0, messagePositionToDisplayIndex(position = 5, size = 5))
        assertEquals(0, messagePositionToDisplayIndex(position = 100, size = 5))
    }

    @Test
    fun emptyList_clampsToZero() {
        assertEquals(0, messagePositionToDisplayIndex(position = 0, size = 0))
    }

    @Test
    fun singleItemList_alwaysMapsToZero() {
        assertEquals(0, messagePositionToDisplayIndex(position = 0, size = 1))
    }
}
