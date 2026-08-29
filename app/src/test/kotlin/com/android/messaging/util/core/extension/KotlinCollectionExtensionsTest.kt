package com.android.messaging.util.core.extension

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinCollectionExtensionsTest {

    @Test
    fun allOrNull_emptyCollection_isNullRatherThanVacuouslyTrue() {
        assertNull(emptyList<Int>().allOrNull { it > 0 })
    }

    @Test
    fun allOrNull_everyElementMatches_isTrue() {
        assertTrue(requireNotNull(listOf(1, 2, 3).allOrNull { it > 0 }))
    }

    @Test
    fun allOrNull_oneElementDoesNotMatch_isFalse() {
        assertFalse(requireNotNull(listOf(1, -2, 3).allOrNull { it > 0 }))
    }

    @Test
    fun allOrNull_isIndependentOfElementOrder() {
        val elements = listOf(1, -2, 3)

        assertEquals(
            elements.allOrNull { it > 0 },
            elements.reversed().allOrNull { it > 0 },
        )
    }
}
