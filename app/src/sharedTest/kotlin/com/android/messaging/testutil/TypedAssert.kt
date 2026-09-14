package com.android.messaging.testutil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

@JvmInline
value class TypedAssert<T>(
    private val actual: T,
) {
    fun isEqualTo(expected: T) {
        assertEquals(expected, actual)
    }

    fun isNull() {
        assertNull(actual)
    }
}

fun <T> assertThat(actual: T): TypedAssert<T> {
    return TypedAssert(actual)
}
